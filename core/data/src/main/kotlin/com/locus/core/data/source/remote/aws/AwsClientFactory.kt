package com.locus.core.data.source.remote.aws

import aws.sdk.kotlin.services.cloudformation.CloudFormationClient
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import javax.inject.Inject

interface AwsClientFactory {
    fun createCloudFormationClient(credentialsProvider: CredentialsProvider): CloudFormationClient

    fun createS3Client(credentialsProvider: CredentialsProvider): S3Client

    companion object {
        const val AWS_REGION = "us-east-1"
    }
}

class DefaultAwsClientFactory
    @Inject
    constructor() : AwsClientFactory {
        override fun createCloudFormationClient(credentialsProvider: CredentialsProvider): CloudFormationClient {
            return CloudFormationClient {
                region = AwsClientFactory.AWS_REGION
                this.credentialsProvider = credentialsProvider
            }
        }

        override fun createS3Client(credentialsProvider: CredentialsProvider): S3Client {
            return S3Client {
                region = AwsClientFactory.AWS_REGION
                this.credentialsProvider = credentialsProvider
            }
        }
    }
