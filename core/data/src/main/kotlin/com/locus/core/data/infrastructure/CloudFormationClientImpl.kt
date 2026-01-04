package com.locus.core.data.infrastructure

import aws.sdk.kotlin.services.cloudformation.describeStackEvents
import aws.sdk.kotlin.services.cloudformation.describeStacks
import aws.smithy.kotlin.runtime.time.toJvmInstant
import com.locus.core.data.source.remote.aws.AwsClientFactory
import com.locus.core.domain.infrastructure.CloudFormationClient
import com.locus.core.domain.infrastructure.StackDetails
import com.locus.core.domain.infrastructure.StackEvent
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import javax.inject.Inject

class CloudFormationClientImpl
    @Inject
    constructor(
        private val awsClientFactory: AwsClientFactory,
    ) : CloudFormationClient {
        override suspend fun createStack(
            creds: BootstrapCredentials,
            stackName: String,
            template: String,
            parameters: Map<String, String>,
        ): LocusResult<String> {
            return try {
                val client = awsClientFactory.createBootstrapCloudFormationClient(creds)
                client.use { cf ->
                    val request =
                        aws.sdk.kotlin.services.cloudformation.model.CreateStackRequest {
                            this.stackName = stackName
                            this.templateBody = template
                            this.parameters =
                                parameters.map { (k, v) ->
                                    aws.sdk.kotlin.services.cloudformation.model.Parameter {
                                        parameterKey = k
                                        parameterValue = v
                                    }
                                }
                            this.capabilities = listOf(aws.sdk.kotlin.services.cloudformation.model.Capability.CapabilityNamedIam)
                        }
                    val response = cf.createStack(request)
                    val id =
                        response.stackId
                            ?: return LocusResult.Failure(
                                DomainException.NetworkError.Generic(Exception("No Stack ID returned")),
                            )
                    LocusResult.Success(id)
                }
            } catch (e: Exception) {
                LocusResult.Failure(DomainException.NetworkError.Generic(e))
            }
        }

        override suspend fun describeStack(
            creds: BootstrapCredentials,
            stackName: String,
        ): LocusResult<StackDetails> {
            return try {
                val client = awsClientFactory.createBootstrapCloudFormationClient(creds)
                client.use { cf ->
                    val response = cf.describeStacks { this.stackName = stackName }
                    val stack =
                        response.stacks?.firstOrNull()
                            ?: return LocusResult.Failure(DomainException.NetworkError.Generic(Exception("Stack not found")))

                    val details =
                        StackDetails(
                            stackId = stack.stackId,
                            status = stack.stackStatus.toString(),
                            outputs =
                                stack.outputs
                                    ?.filter { it.outputKey != null && it.outputValue != null }
                                    ?.associate { it.outputKey!! to it.outputValue!! },
                        )
                    LocusResult.Success(details)
                }
            } catch (e: Exception) {
                LocusResult.Failure(DomainException.NetworkError.Generic(e))
            }
        }

        override suspend fun describeStackEvents(
            creds: BootstrapCredentials,
            stackName: String,
        ): LocusResult<List<StackEvent>> {
            return try {
                val client = awsClientFactory.createBootstrapCloudFormationClient(creds)
                client.use { cf ->
                    val response = cf.describeStackEvents { this.stackName = stackName }
                    val events =
                        response.stackEvents?.map {
                            StackEvent(
                                eventId = it.eventId ?: "",
                                timestamp = it.timestamp?.toJvmInstant() ?: java.time.Instant.now(),
                                logicalResourceId = it.logicalResourceId ?: "",
                                resourceStatus = it.resourceStatus.toString(),
                                resourceStatusReason = it.resourceStatusReason,
                            )
                        } ?: emptyList()
                    // Return sorted by timestamp ascending
                    LocusResult.Success(events.sortedBy { it.timestamp })
                }
            } catch (e: Exception) {
                LocusResult.Failure(DomainException.NetworkError.Generic(e))
            }
        }
    }
