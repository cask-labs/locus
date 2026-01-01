package com.locus.core.domain.infrastructure

import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.result.LocusResult

interface S3Client {
    suspend fun listBuckets(creds: BootstrapCredentials): LocusResult<List<String>>

    suspend fun getBucketTags(
        creds: BootstrapCredentials,
        bucketName: String,
    ): LocusResult<Map<String, String>>
}
