package com.locus.core.data.source.remote.aws

import aws.sdk.kotlin.services.cloudformation.CloudFormationClient
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.locus.core.domain.model.auth.BootstrapCredentials
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating ephemeral AWS clients (S3, CloudFormation) on-demand using
 * temporary Bootstrap Credentials. This avoids injecting scoped clients into the
 * singleton Repository.
 */
@Singleton
class AwsClientFactory
    @Inject
    constructor() {
        fun createBootstrapS3Client(creds: BootstrapCredentials): S3Client {
            val staticProvider = StaticCredentialsProvider(creds)
            return S3Client {
                region = AWS_REGION
                credentialsProvider = staticProvider
            }
        }

        fun createBootstrapCloudFormationClient(creds: BootstrapCredentials): CloudFormationClient {
            val staticProvider = StaticCredentialsProvider(creds)
            return CloudFormationClient {
                region = AWS_REGION
                credentialsProvider = staticProvider
            }
        }

        companion object {
            const val AWS_REGION = "us-east-1"
        }
    }

private class StaticCredentialsProvider(private val creds: BootstrapCredentials) : CredentialsProvider {
    override suspend fun resolve(attributes: aws.smithy.kotlin.runtime.collections.Attributes): Credentials {
        return Credentials(
            accessKeyId = creds.accessKeyId,
            secretAccessKey = creds.secretAccessKey,
            sessionToken = creds.sessionToken,
        )
    }
}
