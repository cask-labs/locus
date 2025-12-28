package com.locus.core.data.source.remote.aws

import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import com.locus.core.domain.result.LocusResult
import javax.inject.Inject

/**
 * Client for interacting with S3 using runtime credentials.
 */
class S3Client
    @Inject
    constructor(
        private val credentialsProvider: LocusCredentialsProvider,
        private val awsClientFactory: AwsClientFactory,
    ) : RemoteStorageInterface {
        override suspend fun uploadTrack(
            bucketName: String,
            key: String,
            fileContent: ByteArray,
        ): LocusResult<Unit> {
            return try {
                val client = awsClientFactory.createS3Client(credentialsProvider)

                client.use { s3Client ->
                    val request =
                        PutObjectRequest {
                            this.bucket = bucketName
                            this.key = key
                            this.body = ByteStream.fromBytes(fileContent)
                        }

                    s3Client.putObject(request)
                    LocusResult.Success(Unit)
                }
            } catch (e: Exception) {
                LocusResult.Failure(e)
            }
        }
    }
