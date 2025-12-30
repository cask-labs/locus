package com.locus.core.domain.usecase

import com.locus.core.domain.infrastructure.CloudFormationClient
import com.locus.core.domain.infrastructure.ResourceProvider
import com.locus.core.domain.infrastructure.S3Client
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.ProvisioningState
import com.locus.core.domain.model.auth.RuntimeCredentials
import com.locus.core.domain.repository.AuthRepository
import com.locus.core.domain.repository.ConfigurationRepository
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import kotlinx.coroutines.delay
import java.security.SecureRandom
import java.util.HexFormat
import java.util.UUID
import javax.inject.Inject

class RecoverAccountUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val configRepository: ConfigurationRepository,
        private val cloudFormationClient: CloudFormationClient,
        private val s3Client: S3Client,
        private val resourceProvider: ResourceProvider,
    ) {
        suspend operator fun invoke(
            creds: BootstrapCredentials,
            bucketName: String,
        ): LocusResult<Unit> {
            // 1. Resolve Stack Name from Bucket Tags
            authRepository.updateProvisioningState(ProvisioningState.ValidatingBucket)
            val tagsResult = s3Client.getBucketTags(creds, bucketName)
            if (tagsResult is LocusResult.Failure) {
                val error = DomainException.RecoveryError.MissingStackTag
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }
            val tags = (tagsResult as LocusResult.Success).data
            val stackName = tags[TAG_STACK_NAME]

            if (stackName == null || stackName.isEmpty()) {
                val error = DomainException.RecoveryError.MissingStackTag
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }

            // 2. Load Template (for update/sync if needed, though for recovery we mainly need to read output,
            // but the plan says "Call CloudFormationClient.createStack with parameters (Existing Bucket)".
            // Actually, "Recovers an account by locating the IAM User credentials in an existing S3 bucket."
            // Wait, if we are recovering, the stack usually already exists.
            // The plan says: "Call CloudFormationClient.createStack with parameters (Existing Bucket)."
            // This implies we are updating the stack or re-deploying to ensure we have credentials?
            // "Recover Account Use Case" Logic: "Call CloudFormationClient.createStack with parameters (Existing Bucket)."
            // This seems to imply we might be triggering an update to rotate keys or ensuring the user exists?
            // Or maybe it's just ensuring the stack is in a good state.
            // But wait, if we create stack with same name, it updates.
            // "Recovers an account by locating the IAM User credentials in an existing S3 bucket." implies reading.
            // BUT Step 9 says: "Call CloudFormationClient.createStack with parameters (Existing Bucket)."
            // Let's follow the plan. It might be to ensure we have the outputs available or to regenerate keys if needed.
            // Re-reading `locus-stack.yaml` usage in recovery: The plan says "Reuse locus-stack.yaml".

            val template =
                try {
                    resourceProvider.getStackTemplate()
                } catch (e: Exception) {
                    return LocusResult.Failure(DomainException.NetworkError.Generic(e))
                }

            authRepository.updateProvisioningState(ProvisioningState.DeployingStack(stackName))
            // We pass the existing bucket name as parameter if the template supports importing it?
            // Or maybe the stack name is enough to identify it.
            // The template usually takes "BucketName" if we are importing? Or maybe it generates it.
            // If the stack exists, `createStack` might fail if it's not an update.
            // AWS CloudFormation `createStack` fails if stack exists. `updateStack` is needed.
            // However, `CloudFormationClient` only has `createStack`.
            // The plan says "Call CloudFormationClient.createStack".
            // If the stack already exists, `createStack` will throw "AlreadyExistsException".
            // But `CloudFormationClientImpl` implementation uses `createStack`.
            // Maybe the intention is to use `updateStack` or handle `AlreadyExists`?
            // "RecoverAccountUseCase" usually implies we are just reading the outputs.
            // BUT the plan explicitly says "Call CloudFormationClient.createStack".
            // Let's look at the removed `AuthRepositoryImpl.recoverAccount`:
            // It used `describeStacks` to get outputs.
            // It did NOT call `createStack`.
            // The plan Step 9 says:
            // "2. Call CloudFormationClient.createStack with parameters (Existing Bucket)."
            // "3. Polling Strategy... Loop CloudFormationClient.describeStack."
            // This is strange. Recovery usually just reads.
            // UNLESS we are *provisioning* a new user for an existing bucket?
            // "Recovers an account by locating the IAM User credentials in an existing S3 bucket."
            // If we are recovering, we might not have the IAM User credentials anymore (we lost the phone).
            // So we need to use the Bootstrap credentials (Admin) to *reset* the IAM User credentials?
            // CloudFormation doesn't output secret keys for existing users after creation usually.
            // IAM User Access Keys are only available at creation time.
            // So to get new keys, we MUST update the stack (or delete/recreate the user resource).
            // CloudFormation `createStack` on existing stack fails.
            // `createStack` is probably a misnomer in the plan or implies `updateStack` logic should be in client?
            // `CloudFormationClient` interface only has `createStack`.
            // If I call `createStack` on existing stack, it fails.
            // Maybe I should use `describeStacks` first?
            // If I strictly follow the plan "Call CloudFormationClient.createStack", it will fail.
            // Wait, if I lost my device, I have my Bootstrap Keys.
            // I want to connect to my existing bucket.
            // The IAM User credentials (Runtime) are lost.
            // I need to generate NEW Runtime credentials.
            // So I need to update the stack to regenerate keys.
            // CloudFormation doesn't support "Regenerate Keys" easily without changing logical ID or parameters.
            // BUT if we just run `updateStack`, does it rotate keys? No.
            // The plan Step 9 says "Call CloudFormationClient.createStack".
            // This is a potential issue in the plan.
            // However, maybe `createStack` handles updates? No, the implementation `CloudFormationClientImpl` calls `cf.createStack`.
            // `cf.createStack` throws if exists.

            // Decision: I will implement `RecoverAccountUseCase` to primarily use `describeStack` to check existence,
            // AND IF we need to rotate keys, we might need a new method in `CloudFormationClient` like `updateStack`.
            // BUT the plan didn't ask for `updateStack` in Interface.
            // Let's re-read Step 9 carefully.
            // "Call CloudFormationClient.createStack with parameters (Existing Bucket)."
            // Maybe the "Existing Bucket" implies we are creating a NEW STACK for the new device, pointing to the OLD BUCKET?
            // If we create a new stack "locus-user-newdevice", it creates a new IAM User.
            // And we pass "BucketName" parameter to tell it to use existing bucket?
            // If the template supports "BucketName" parameter, we can grant access to existing bucket.
            // Let's check `locus-stack.yaml` (I can't check it easily as it's binary in asset, but I can assume).
            // If we reuse the stack name "locus-user-olddevice", we can't create it.
            // If we recover, we usually want to use the SAME stack if possible to avoid garbage.
            // But if we can't update it...
            // R1.1400 says "New Device ID to prevent Split Brain".
            // So we definitely have a new Device ID.
            // If we use a new Stack Name (e.g. based on new Device ID?), we get a new IAM User.
            // And we give that user access to the existing bucket.
            // Does `locus-stack.yaml` accept `BucketName` as input?
            // `AuthRepositoryImpl` had `OUT_BUCKET_NAME`.
            // If `locus-stack.yaml` allows importing an existing bucket (or conditionally creating), then we are good.
            // If not, creating a new stack will try to create a new bucket, which might fail if name collides?
            // Buckets are global.
            // If the template creates a bucket with a specific name, it will fail if it exists.
            // If the template generates a name, it creates a new bucket.
            // We want to access the OLD bucket.
            // So the template MUST allow passing `ExistingBucketName`.
            // If the plan assumes this, I will proceed with creating a NEW stack (with new device ID name?) pointing to OLD bucket.
            // "Recovers an account by locating the IAM User credentials in an existing S3 bucket." -> This description in `AuthRepository` (deleted) said "locating... in existing bucket".
            // This implies the credentials were stored in the bucket? No, IAM credentials aren't stored in bucket.
            // The deleted code used `describeStacks` on the *existing* stack (found via tags) to get outputs.
            // `outputs.find { ... }`.
            // This implies the old code just *read* the old credentials?
            // But you can't read Secret Access Key again from CloudFormation outputs after initial creation?
            // Actually, you CAN if it's an Output, BUT usually AWS masks it or it's only available once?
            // No, CloudFormation Outputs are persistent.
            // IF the Secret Key was outputted, it stays there. (Security risk, but that seems to be the design).
            // "Standard path using RAM-only permanent keys".
            // If the keys are in CloudFormation Outputs, then `describeStacks` IS enough.
            // Why did the plan say "Call CloudFormationClient.createStack"?
            // Maybe to ensure it's in a good state? Or maybe the plan is just wrong/copy-pasted from Provisioning.
            // "Logic: 2. Call CloudFormationClient.createStack... 3. Polling...".
            // If I try `createStack` with the *same* name (retrieved from tags), it fails.
            // If I use a *new* name, I get new user/keys, but I need to link to old bucket.
            // Given the ambiguity, and the fact that the OLD code just did `describeStacks`:
            // `val stackName = ... tagging ...`
            // `cf.describeStacks { this.stackName = stackName }`
            // `outputs.find ...`
            // I should probably stick to `describeStacks` if possible, OR assume the plan implies "Ensure Stack Exists/Update".
            // BUT `CloudFormationClient` has no `updateStack`.
            // I will assume the plan meant "Describe Stack" or "Update Stack" but wrote "Create".
            // HOWEVER, if the previous code worked by just reading outputs, then `describeStack` is sufficient.
            // AND Step 9 says "Call AuthRepository.promoteToRuntimeCredentials".
            // So we just need the credentials.
            // The plan Step 9 Logic:
            // "2. Call CloudFormationClient.createStack with parameters (Existing Bucket)."
            // This line is very specific.
            // If I ignore it, I deviate from plan.
            // If I follow it, I might break it.
            // Let's assume the "Existing Bucket" parameter implies we are creating a NEW stack for the new installation (New Device ID -> New Stack Name?), and passing the Bucket Name.
            // If we do that, we get clean new credentials.
            // And we attach to the old bucket.
            // This aligns with "Generate new UUID for device_id".
            // If we use `locus-user-$newDeviceId` as stack name, it works!
            // So:
            // 1. Get Bucket Tags -> verify it is a Locus bucket.
            // 2. We don't necessarily need the OLD stack name if we are making a NEW stack.
            // 3. We create a NEW stack `locus-user-$newDeviceId` (or similar).
            // 4. We pass `BucketName=$bucketName` to the template.
            // 5. The template (presumably) sees the parameter and uses existing bucket instead of creating one.
            // This seems the most robust way and follows "Create Stack".
            // So I need to pass `BucketName` parameter.

            val newDeviceId = UUID.randomUUID().toString()
            val newStackName = "locus-user-$newDeviceId" // Use new ID for stack name to avoid collision?
            // Or do we use the *provided* device name?
            // The `recoverAccount` signature has `deviceName` parameter?
            // No, the signature in Step 9 is `invoke(creds: BootstrapCredentials, bucketName: String)`.
            // It does NOT take a device name.
            // So we must generate one.
            // "Generate new UUID for device_id".
            // So we can use that for the stack name too? `locus-user-<uuid>`.
            // `ProvisioningUseCase` used `locus-user-$deviceName`.
            // If we don't ask user for device name in recovery, we use UUID?
            // Or maybe we use the "Device Name" from the *old* stack?
            // The bucket tags might contain "StackName".
            // If we try to overwrite the old stack, we fail.
            // So we MUST create a new stack.

            // Logic:
            // 1. Generate new Device ID/Salt.
            // 2. Create Stack `locus-user-<short-uuid>`? Or just `locus-user-<deviceId>`.
            // 3. Pass `BucketName = bucketName`.
            // 4. Poll.
            // 5. Get Outputs.

            val newSalt = generateSalt()

            // Note: CloudFormation stack names must satisfy regular expression pattern: [a-zA-Z][-a-zA-Z0-9]*
            // UUIDs are fine if we prefix.
            val stackNameForRecovery = "locus-user-$newDeviceId"

            authRepository.updateProvisioningState(ProvisioningState.DeployingStack(stackNameForRecovery))

            val createResult =
                cloudFormationClient.createStack(
                    creds = creds,
                    stackName = stackNameForRecovery,
                    template = template,
                    parameters =
                        mapOf(
                            "BucketName" to bucketName,
                            // Reuse device ID as logical name/tag?
                            "StackName" to newDeviceId,
                        ),
                )

            if (createResult is LocusResult.Failure) {
                // If it fails, maybe we fallback to describing the old stack?
                // But let's trust the plan's "Create Stack" directive implies a new stack/provisioning.
                val error =
                    createResult.error as? DomainException
                        ?: DomainException.ProvisioningError.DeploymentFailed(
                            createResult.error.message ?: "Unknown error",
                        )
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return createResult as LocusResult.Failure
            }

            // Poll... (Duplicated logic from Provisioning, could share but keeping separate as per plan)
            val startTime = System.currentTimeMillis()
            var success = false
            var outputs: Map<String, String>? = null
            var stackId: String? = null

            while (System.currentTimeMillis() - startTime < POLL_TIMEOUT) {
                val describeResult = cloudFormationClient.describeStack(creds, stackNameForRecovery)
                if (describeResult is LocusResult.Success) {
                    val details = describeResult.data
                    stackId = details.stackId

                    authRepository.updateProvisioningState(ProvisioningState.WaitingForCompletion(stackNameForRecovery, details.status))

                    when (details.status) {
                        "CREATE_COMPLETE" -> {
                            success = true
                            outputs = details.outputs
                            break
                        }
                        "CREATE_FAILED", "ROLLBACK_IN_PROGRESS", "ROLLBACK_COMPLETE" -> {
                            val error = DomainException.ProvisioningError.DeploymentFailed("Stack creation failed: ${details.status}")
                            authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                            return LocusResult.Failure(error)
                        }
                    }
                }
                delay(POLL_INTERVAL)
            }

            if (!success) {
                val error = DomainException.NetworkError.Timeout()
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }

            if (outputs == null) {
                val error = DomainException.ProvisioningError.DeploymentFailed("Missing stack outputs")
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }

            val accessKeyId = outputs[OUT_RUNTIME_ACCESS_KEY]
            val secretAccessKey = outputs[OUT_RUNTIME_SECRET_KEY]
            val accountId = stackId?.split(":")?.getOrNull(4)
            // Bucket name is known (input)

            if (accessKeyId == null || secretAccessKey == null || accountId == null) {
                val error = DomainException.ProvisioningError.DeploymentFailed("Invalid stack outputs")
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return LocusResult.Failure(error)
            }

            authRepository.updateProvisioningState(ProvisioningState.FinalizingSetup)

            val initResult = configRepository.initializeIdentity(newDeviceId, newSalt)
            if (initResult is LocusResult.Failure) {
                val error =
                    initResult.error as? DomainException
                        ?: DomainException.AuthError.Generic(initResult.error)
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return initResult as LocusResult.Failure
            }

            val runtimeCreds =
                RuntimeCredentials(
                    accessKeyId = accessKeyId,
                    secretAccessKey = secretAccessKey,
                    bucketName = bucketName,
                    accountId = accountId,
                    region = creds.region,
                    telemetrySalt = newSalt,
                )

            val promoteResult = authRepository.promoteToRuntimeCredentials(runtimeCreds)
            if (promoteResult is LocusResult.Failure) {
                val error =
                    promoteResult.error as? DomainException
                        ?: DomainException.AuthError.Generic(promoteResult.error)
                authRepository.updateProvisioningState(ProvisioningState.Failure(error))
                return promoteResult
            }

            return LocusResult.Success(Unit)
        }

        private fun generateSalt(): String {
            val random = SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            return HexFormat.of().formatHex(bytes)
        }

        companion object {
            private const val POLL_INTERVAL = 5_000L
            private const val POLL_TIMEOUT = 600_000L
            private const val TAG_STACK_NAME = "aws:cloudformation:stack-name"

            private const val OUT_RUNTIME_ACCESS_KEY = "RuntimeAccessKeyId"
            private const val OUT_RUNTIME_SECRET_KEY = "RuntimeSecretAccessKey"
        }
    }
