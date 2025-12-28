package com.locus.core.data.repository

import aws.sdk.kotlin.services.cloudformation.describeStacks
import aws.sdk.kotlin.services.cloudformation.model.DescribeStacksRequest
import aws.sdk.kotlin.services.s3.getBucketTagging
import aws.sdk.kotlin.services.s3.headBucket
import aws.sdk.kotlin.services.s3.listBuckets
import aws.sdk.kotlin.services.s3.model.GetBucketTaggingRequest
import aws.sdk.kotlin.services.s3.model.HeadBucketRequest
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
                    try {
                        val headRequest = HeadBucketRequest { this.bucket = bucketName }
                        s3.headBucket(headRequest)
                    } catch (e: Exception) {
                        return LocusResult.Success(BucketValidationStatus.Invalid("Bucket not found or access denied"))
                    }

                    // Check Tags
                    val taggingRequest = GetBucketTaggingRequest { this.bucket = bucketName }
                    val tagging =
                        try {
                            s3.getBucketTagging(taggingRequest)
                        } catch (e: Exception) {
                            return LocusResult.Success(BucketValidationStatus.Invalid("Failed to retrieve bucket tags"))
                        }

                    val hasLocusTag = tagging.tagSet?.any { it.key == "LocusRole" && it.value == "DeviceBucket" } == true
                    if (!hasLocusTag) {
                        return LocusResult.Success(BucketValidationStatus.Invalid("Bucket missing required LocusRole tag"))
                    }

                    LocusResult.Success(BucketValidationStatus.Available)
                }
            } catch (e: Exception) {
                LocusResult.Success(BucketValidationStatus.Invalid(e.message ?: "Unknown validation error"))
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
                if (result.data == null) {
                    return LocusResult.Failure(DomainException.AuthError.InvalidCredentials)
                }
                return LocusResult.Success(result.data!!)
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
                    val buckets = response.buckets?.mapNotNull { it.name }?.filter { name -> name.startsWith("locus-") } ?: emptyList()
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
                        val taggingRequest = GetBucketTaggingRequest { this.bucket = bucketName }
                        val tagging =
                            try {
                                s3.getBucketTagging(taggingRequest)
                            } catch (e: Exception) {
                                return LocusResult.Failure(DomainException.RecoveryError.MissingStackTag)
                            }

                        tagging.tagSet?.find { it.key == "aws:cloudformation:stack-name" }?.value
                    }

                if (stackName.isNullOrEmpty()) {
                    return LocusResult.Failure(DomainException.RecoveryError.MissingStackTag)
                }

                val cfClient = awsClientFactory.createBootstrapCloudFormationClient(creds)
                return cfClient.use { cf ->
                    val describeRequest = DescribeStacksRequest { this.stackName = stackName }
                    val response = cf.describeStacks(describeRequest)
                    val outputs = response.stacks?.firstOrNull()?.outputs

                    if (outputs == null) {
                        return LocusResult.Failure(DomainException.RecoveryError.InvalidStackOutputs)
                    }

                    val accessKeyId = outputs.find { it.outputKey == "RuntimeAccessKeyId" }?.outputValue
                    val secretAccessKey = outputs.find { it.outputKey == "RuntimeSecretAccessKey" }?.outputValue
                    val bucket = outputs.find { it.outputKey == "BucketName" }?.outputValue ?: bucketName
                    val region = "us-east-1"

                    if (accessKeyId == null || secretAccessKey == null) {
                        return LocusResult.Failure(DomainException.RecoveryError.InvalidStackOutputs)
                    }

                    LocusResult.Success(
                        RuntimeCredentials(
                            accessKeyId = accessKeyId,
                            secretAccessKey = secretAccessKey,
                            bucketName = bucket,
                            accountId = "",
                            region = region,
                            telemetrySalt = null,
                        ),
                    )
                }
            } catch (e: Exception) {
                return LocusResult.Failure(DomainException.NetworkError.Generic(e))
            }
        }
    }
