package com.locus.core.data.source.remote.aws

import aws.sdk.kotlin.services.cloudformation.model.Capability
import aws.sdk.kotlin.services.cloudformation.model.CreateStackRequest
import aws.sdk.kotlin.services.cloudformation.model.DescribeStacksRequest
import aws.sdk.kotlin.services.cloudformation.model.Parameter
import aws.sdk.kotlin.services.cloudformation.model.StackStatus
import aws.sdk.kotlin.services.s3.model.ListBucketsRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.StackOutputs
import com.locus.core.domain.result.LocusResult
import kotlinx.coroutines.delay
import javax.inject.Inject
import aws.sdk.kotlin.services.cloudformation.CloudFormationClient as AwsCloudFormationClient

class CloudFormationClient
    @Inject
    constructor(
        private val awsClientFactory: AwsClientFactory,
        private val templateResourceProvider: TemplateResourceProvider,
    ) : InfrastructureProvisioner {
        override suspend fun createStack(
            name: String,
            parameters: Map<String, String>,
            credentials: BootstrapCredentials,
        ): LocusResult<StackOutputs> {
            return try {
                val templateBody = templateResourceProvider.loadTemplate()

                val awsCredentialsProvider =
                    object : CredentialsProvider {
                        override suspend fun resolve(attributes: aws.smithy.kotlin.runtime.collections.Attributes): Credentials {
                            return Credentials(
                                accessKeyId = credentials.accessKeyId,
                                secretAccessKey = credentials.secretAccessKey,
                                sessionToken = credentials.sessionToken,
                            )
                        }
                    }

                val client = awsClientFactory.createCloudFormationClient(awsCredentialsProvider)

                client.use { cfClient ->
                    val cfParams =
                        parameters.map { (key, value) ->
                            Parameter {
                                parameterKey = key
                                parameterValue = value
                            }
                        }

                    cfClient.createStack(
                        CreateStackRequest {
                            stackName = name
                            this.templateBody = templateBody
                            this.parameters = cfParams
                            capabilities = listOf(Capability.CapabilityNamedIam)
                        },
                    )

                    waitForStackCreation(cfClient, name)
                }
            } catch (e: Exception) {
                LocusResult.Failure(e)
            }
        }

        override suspend fun findLocusBucket(credentials: BootstrapCredentials): LocusResult<List<String>> {
            return try {
                val awsCredentialsProvider =
                    object : CredentialsProvider {
                        override suspend fun resolve(attributes: aws.smithy.kotlin.runtime.collections.Attributes): Credentials {
                            return Credentials(
                                accessKeyId = credentials.accessKeyId,
                                secretAccessKey = credentials.secretAccessKey,
                                sessionToken = credentials.sessionToken,
                            )
                        }
                    }

                val client = awsClientFactory.createS3Client(awsCredentialsProvider)

                client.use { s3Client ->
                    val response = s3Client.listBuckets(ListBucketsRequest {})
                    val buckets = response.buckets?.mapNotNull { it.name }?.filter { it.startsWith("locus-") } ?: emptyList()
                    LocusResult.Success(buckets)
                }
            } catch (e: Exception) {
                LocusResult.Failure(e)
            }
        }

        private suspend fun waitForStackCreation(
            cfClient: AwsCloudFormationClient,
            stackName: String,
        ): LocusResult<StackOutputs> {
            val startTime = System.currentTimeMillis()
            val timeout = 10 * 60 * 1000L // 10 minutes

            while (System.currentTimeMillis() - startTime < timeout) {
                try {
                    val describeResponse =
                        cfClient.describeStacks(
                            DescribeStacksRequest {
                                this.stackName = stackName
                            },
                        )

                    val stack = describeResponse.stacks?.firstOrNull() ?: return LocusResult.Failure(Exception("Stack not found"))

                    when (stack.stackStatus) {
                        StackStatus.CreateComplete -> {
                            // Refactored to filter nulls safely as per PR comment
                            val outputs = stack.outputs.orEmpty()
                            val outputsMap =
                                outputs.mapNotNull { output ->
                                    val key = output.outputKey
                                    val value = output.outputValue
                                    if (key != null && value != null) key to value else null
                                }.toMap()

                            // Parse outputs to StackOutputs
                            val bucketName = outputsMap["LocusBucketName"]
                            val accessKeyId = outputsMap["RuntimeAccessKeyId"]
                            val secretAccessKey = outputsMap["RuntimeSecretAccessKey"]

                            if (bucketName != null && accessKeyId != null && secretAccessKey != null) {
                                return LocusResult.Success(
                                    StackOutputs(
                                        bucketName = bucketName,
                                        runtimeAccessKeyId = accessKeyId,
                                        runtimeSecretAccessKey = secretAccessKey,
                                    ),
                                )
                            } else {
                                return LocusResult.Failure(Exception("Missing required outputs from CloudFormation stack"))
                            }
                        }
                        StackStatus.CreateFailed, StackStatus.RollbackInProgress, StackStatus.RollbackComplete -> {
                            return LocusResult.Failure(Exception("Stack creation failed with status: ${stack.stackStatus}"))
                        }
                        else -> {
                            delay(5000) // Wait 5 seconds
                        }
                    }
                } catch (e: Exception) {
                    return LocusResult.Failure(e)
                }
            }
            return LocusResult.Failure(Exception("Stack creation timed out"))
        }
    }
