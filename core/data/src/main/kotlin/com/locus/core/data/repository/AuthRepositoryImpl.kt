package com.locus.core.data.repository

import aws.sdk.kotlin.services.cloudformation.describeStacks
import aws.sdk.kotlin.services.s3.getBucketTagging
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.listBuckets
import aws.sdk.kotlin.services.s3.model.NotFound
import com.locus.core.data.source.local.SecureStorageDataSource
import com.locus.core.data.source.remote.aws.AwsClientFactory
import com.locus.core.domain.model.auth.AuthState
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.BucketValidationStatus
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val awsClientFactory: AwsClientFactory,
        private val secureStorage: SecureStorageDataSource,
        private val applicationScope: CoroutineScope,
    ) : AuthRepository {
        private val mutableAuthState = MutableStateFlow<AuthState>(AuthState.Uninitialized)
        private val mutableProvisioningState = MutableStateFlow<ProvisioningState>(ProvisioningState.Idle)

        init {
            applicationScope.launch {
                loadInitialState()
            }
        }

        private suspend fun loadInitialState() {
            val runtimeResult = secureStorage.getRuntimeCredentials()
            if (runtimeResult is LocusResult.Success && runtimeResult.data != null) {
                mutableAuthState.value = AuthState.Authenticated
                return
            }

            val bootstrapResult = secureStorage.getBootstrapCredentials()
            if (bootstrapResult is LocusResult.Success && bootstrapResult.data != null) {
                mutableAuthState.value = AuthState.SetupPending
                return
            }

            mutableAuthState.value = AuthState.Uninitialized
        }

        override fun getAuthState(): Flow<AuthState> = mutableAuthState.asStateFlow()

        override fun getProvisioningState(): Flow<ProvisioningState> = mutableProvisioningState.asStateFlow()

        override suspend fun updateProvisioningState(state: ProvisioningState) {
            mutableProvisioningState.value = state
        }

        override suspend fun validateCredentials(creds: BootstrapCredentials): LocusResult<Unit> {
            return try {
                val client = awsClientFactory.createBootstrapS3Client(creds)
                // Use 'use' to auto-close, but we perform operation inside explicitly
                client.use { s3 ->
                    s3.listBuckets()
                }
                LocusResult.Success(Unit)
            } catch (e: Exception) {
                LocusResult.Failure(DomainException.AuthError.InvalidCredentials)
            }
        }

        override suspend fun validateBucket(bucketName: String): LocusResult<BucketValidationStatus> {
            val credsResult = secureStorage.getBootstrapCredentials()
            val creds =
                (credsResult as? LocusResult.Success)?.data
                    ?: return LocusResult.Success(BucketValidationStatus.Invalid("Missing bootstrap credentials"))

            mutableProvisioningState.value = ProvisioningState.ValidatingBucket

            return try {
                val client = awsClientFactory.createBootstrapS3Client(creds)
                client.use { s3 ->
                    // Check Existence
                    s3.headBucket { bucket = bucketName }

                    // Check Tags
                    val tagging = s3.getBucketTagging { bucket = bucketName }

                    val hasLocusTag = tagging.tagSet?.any { it.key == TAG_LOCUS_ROLE && it.value == TAG_DEVICE_BUCKET } == true
                    if (!hasLocusTag) {
                        return LocusResult.Success(BucketValidationStatus.Invalid("Bucket missing required LocusRole tag"))
                    }

                    LocusResult.Success(BucketValidationStatus.Available)
                }
            } catch (e: Exception) {
                if (e is NotFound) {
                    return LocusResult.Success(BucketValidationStatus.Invalid("Bucket not found or access denied"))
                }
                LocusResult.Failure(DomainException.NetworkError.Generic(e))
            }
        }

        override suspend fun saveBootstrapCredentials(creds: BootstrapCredentials): LocusResult<Unit> {
            val result = secureStorage.saveBootstrapCredentials(creds)
            if (result is LocusResult.Success) {
                mutableAuthState.value = AuthState.SetupPending
            }
            return result
        }

        override suspend fun promoteToRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit> {
            val saveResult = secureStorage.saveRuntimeCredentials(creds)
            if (saveResult is LocusResult.Failure) return saveResult

            val clearResult = secureStorage.clearBootstrapCredentials()
            if (clearResult is LocusResult.Failure) {
                return clearResult
            }

            mutableAuthState.value = AuthState.Authenticated
            mutableProvisioningState.value = ProvisioningState.Success
            return LocusResult.Success(Unit)
        }

        override suspend fun replaceRuntimeCredentials(creds: RuntimeCredentials): LocusResult<Unit> {
            val result = secureStorage.saveRuntimeCredentials(creds)
            if (result is LocusResult.Success) {
                mutableAuthState.value = AuthState.Authenticated
            }
            return result
        }

        override suspend fun clearBootstrapCredentials(): LocusResult<Unit> {
            return secureStorage.clearBootstrapCredentials()
        }

        override suspend fun getRuntimeCredentials(): LocusResult<RuntimeCredentials> {
            val result = secureStorage.getRuntimeCredentials()
            if (result is LocusResult.Success) {
                val data = result.data ?: return LocusResult.Failure(DomainException.AuthError.InvalidCredentials)
                return LocusResult.Success(data)
            }
            return result as LocusResult.Failure
        }

        override suspend fun scanForRecoveryBuckets(): LocusResult<List<String>> {
            val credsResult = secureStorage.getBootstrapCredentials()
            val creds =
                (credsResult as? LocusResult.Success)?.data
                    ?: return LocusResult.Failure(DomainException.AuthError.InvalidCredentials)

            return try {
                val client = awsClientFactory.createBootstrapS3Client(creds)
                client.use { s3 ->
                    val response = s3.listBuckets()
                    val buckets = response.buckets?.mapNotNull { it.name }?.filter { name -> name.startsWith(BUCKET_PREFIX) } ?: emptyList()
                    LocusResult.Success(buckets)
                }
            } catch (e: Exception) {
                LocusResult.Failure(DomainException.NetworkError.Generic(e))
            }
        }

        override suspend fun recoverAccount(
            bucketName: String,
            deviceName: String,
        ): LocusResult<RuntimeCredentials> {
            val credsResult = secureStorage.getBootstrapCredentials()
            val creds =
                (credsResult as? LocusResult.Success)?.data
                    ?: return LocusResult.Failure(DomainException.AuthError.InvalidCredentials)

            try {
                val s3Client = awsClientFactory.createBootstrapS3Client(creds)
                val stackName =
                    s3Client.use { s3 ->
                        val tagging =
                            try {
                                s3.getBucketTagging { bucket = bucketName }
                            } catch (e: Exception) {
                                return LocusResult.Failure(DomainException.RecoveryError.MissingStackTag)
                            }

                        tagging.tagSet?.find { it.key == TAG_STACK_NAME }?.value
                    }

                if (stackName.isNullOrEmpty()) {
                    return LocusResult.Failure(DomainException.RecoveryError.MissingStackTag)
                }

                val cfClient = awsClientFactory.createBootstrapCloudFormationClient(creds)
                return cfClient.use { cf ->
                    val response = cf.describeStacks { this.stackName = stackName }
                    val stack = response.stacks?.firstOrNull()
                    val outputs = stack?.outputs

                    if (outputs == null) {
                        return LocusResult.Failure(DomainException.RecoveryError.InvalidStackOutputs)
                    }

                    // Extract Account ID from ARN: arn:aws:cloudformation:region:account-id:stack/stack-name/stack-id
                    // Index 4 is the account ID
                    val stackId = stack.stackId
                    val accountId = stackId?.split(":")?.getOrNull(4)

                    // WARNING: This depends on specific Output keys from the CloudFormation stack.
                    // Verify these match the actual stack template outputs (e.g. locus-stack.yaml or similar).
                    // Current expectation: RuntimeAccessKeyId, RuntimeSecretAccessKey, BucketName.
                    val accessKeyId = outputs.find { it.outputKey == OUT_RUNTIME_ACCESS_KEY }?.outputValue
                    val secretAccessKey = outputs.find { it.outputKey == OUT_RUNTIME_SECRET_KEY }?.outputValue
                    val bucket = outputs.find { it.outputKey == OUT_BUCKET_NAME }?.outputValue ?: bucketName
                    val region = AwsClientFactory.AWS_REGION

                    if (accessKeyId == null || secretAccessKey == null || accountId == null) {
                        return LocusResult.Failure(DomainException.RecoveryError.InvalidStackOutputs)
                    }

                    LocusResult.Success(
                        RuntimeCredentials(
                            accessKeyId = accessKeyId,
                            secretAccessKey = secretAccessKey,
                            bucketName = bucket,
                            accountId = accountId,
                            region = region,
                            telemetrySalt = null,
                        ),
                    )
                }
            } catch (e: Exception) {
                return LocusResult.Failure(DomainException.NetworkError.Generic(e))
            }
        }

        companion object {
            private const val TAG_LOCUS_ROLE = "LocusRole"
            private const val TAG_DEVICE_BUCKET = "DeviceBucket"
            private const val TAG_STACK_NAME = "aws:cloudformation:stack-name"
            private const val BUCKET_PREFIX = "locus-"

            private const val OUT_RUNTIME_ACCESS_KEY = "RuntimeAccessKeyId"
            private const val OUT_RUNTIME_SECRET_KEY = "RuntimeSecretAccessKey"
            private const val OUT_BUCKET_NAME = "BucketName"
        }
    }
