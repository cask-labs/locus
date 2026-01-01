package com.locus.core.data.infrastructure

import aws.sdk.kotlin.services.s3.getBucketTagging
import aws.sdk.kotlin.services.s3.listBuckets
import com.locus.core.data.source.remote.aws.AwsClientFactory
import com.locus.core.domain.infrastructure.S3Client
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.result.DomainException
import com.locus.core.domain.result.LocusResult
import javax.inject.Inject

class S3ClientImpl
    @Inject
    constructor(
        private val awsClientFactory: AwsClientFactory,
    ) : S3Client {
        override suspend fun listBuckets(creds: BootstrapCredentials): LocusResult<List<String>> {
            return try {
                val client = awsClientFactory.createBootstrapS3Client(creds)
                client.use { s3 ->
                    val response = s3.listBuckets()
                    val buckets = response.buckets?.mapNotNull { it.name } ?: emptyList()
                    LocusResult.Success(buckets)
                }
            } catch (e: Exception) {
                LocusResult.Failure(DomainException.NetworkError.Generic(e))
            }
        }

        override suspend fun getBucketTags(
            creds: BootstrapCredentials,
            bucketName: String,
        ): LocusResult<Map<String, String>> {
            return try {
                val client = awsClientFactory.createBootstrapS3Client(creds)
                client.use { s3 ->
                    val tagging = s3.getBucketTagging { bucket = bucketName }
                    val tags = tagging.tagSet?.associate { it.key to it.value } ?: emptyMap()
                    LocusResult.Success(tags)
                }
            } catch (e: Exception) {
                // If bucket doesn't exist or no access or no tags, just return failure or empty.
                // Here we return failure as it might be important.
                LocusResult.Failure(DomainException.NetworkError.Generic(e))
            }
        }
    }
