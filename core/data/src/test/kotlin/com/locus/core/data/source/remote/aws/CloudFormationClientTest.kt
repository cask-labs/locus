package com.locus.core.data.source.remote.aws

import aws.sdk.kotlin.services.cloudformation.model.CreateStackResponse
import aws.sdk.kotlin.services.cloudformation.model.DescribeStacksResponse
import aws.sdk.kotlin.services.cloudformation.model.Output
import aws.sdk.kotlin.services.cloudformation.model.Stack
import aws.sdk.kotlin.services.cloudformation.model.StackStatus
import com.google.common.truth.Truth.assertThat
import com.locus.core.domain.result.LocusResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import aws.sdk.kotlin.services.cloudformation.CloudFormationClient as AwsCloudFormationClient

@RunWith(RobolectricTestRunner::class)
class CloudFormationClientTest {
    private val awsClientFactory = mockk<AwsClientFactory>()
    private val awsCloudFormationClient = mockk<AwsCloudFormationClient>(relaxed = true)
    private val templateResourceProvider = mockk<TemplateResourceProvider>()
    private lateinit var client: CloudFormationClient

    @Before
    fun setup() {
        client = CloudFormationClient(awsClientFactory, templateResourceProvider)
        every { templateResourceProvider.loadTemplate() } returns "template-body"
    }

    @Test
    fun `createStack returns success when stack creation completes`() =
        runTest {
            // Mock Factory
            every { awsClientFactory.createCloudFormationClient(any()) } returns awsCloudFormationClient

            // Mock CreateStack
            coEvery { awsCloudFormationClient.createStack(any()) } returns CreateStackResponse { stackId = "id" }

            // Mock DescribeStacks polling
            // 1. First call: In Progress
            // 2. Second call: Complete
            val inProgressStack =
                Stack {
                    stackName = "test-stack"
                    stackStatus = StackStatus.CreateInProgress
                }
            val completeStack =
                Stack {
                    stackName = "test-stack"
                    stackStatus = StackStatus.CreateComplete
                    outputs =
                        listOf(
                            Output {
                                outputKey = "LocusBucketName"
                                outputValue = "my-bucket"
                            },
                            Output {
                                outputKey = "RuntimeAccessKeyId"
                                outputValue = "key-id"
                            },
                            Output {
                                outputKey = "RuntimeSecretAccessKey"
                                outputValue = "secret"
                            },
                        )
                }

            coEvery { awsCloudFormationClient.describeStacks(any()) } returnsMany
                listOf(
                    DescribeStacksResponse { stacks = listOf(inProgressStack) },
                    DescribeStacksResponse { stacks = listOf(completeStack) },
                )

            val dummyCreds = com.locus.core.domain.model.auth.BootstrapCredentials("id", "secret", "token", "us-east-1")

            val result = client.createStack("test-stack", emptyMap(), dummyCreds)

            assertThat(result).isInstanceOf(LocusResult.Success::class.java)
            val data = (result as LocusResult.Success).data
            assertThat(data.bucketName).isEqualTo("my-bucket")
        }

    @Test
    fun `createStack handles null outputs safely`() =
        runTest {
            every { awsClientFactory.createCloudFormationClient(any()) } returns awsCloudFormationClient
            coEvery { awsCloudFormationClient.createStack(any()) } returns CreateStackResponse {}

            // Stack complete but with weird nulls
            val weirdStack =
                Stack {
                    stackName = "test-stack"
                    stackStatus = StackStatus.CreateComplete
                    outputs =
                        listOf(
                            Output {
                                outputKey = null
                                outputValue = "val"
                            },
                            Output {
                                outputKey = "key"
                                outputValue = null
                            },
                        )
                }

            coEvery { awsCloudFormationClient.describeStacks(any()) } returns DescribeStacksResponse { stacks = listOf(weirdStack) }

            val dummyCreds = com.locus.core.domain.model.auth.BootstrapCredentials("id", "secret", "token", "us-east-1")
            val result = client.createStack("test-stack", emptyMap(), dummyCreds)

            // Should fail because required outputs are missing
            assertThat(result).isInstanceOf(LocusResult.Failure::class.java)
            assertThat((result as LocusResult.Failure).error.message).contains("Missing required outputs")
        }
}
