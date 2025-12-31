package com.locus.core.domain.usecase

import com.locus.core.domain.infrastructure.S3Client
import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.model.auth.BucketValidationError
import com.locus.core.domain.model.auth.BucketValidationStatus
import com.locus.core.domain.result.LocusResult
import javax.inject.Inject

class ScanBucketsUseCase
    @Inject
    constructor(
        private val s3Client: S3Client,
    ) {
        suspend operator fun invoke(creds: BootstrapCredentials): LocusResult<List<Pair<String, BucketValidationStatus>>> {
            val listResult = s3Client.listBuckets(creds)
            val buckets =
                when (listResult) {
                    is LocusResult.Success -> listResult.data
                    is LocusResult.Failure -> return LocusResult.Failure(listResult.error)
                }

            val locusBuckets = buckets.filter { it.startsWith(BUCKET_PREFIX) }
            val results = mutableListOf<Pair<String, BucketValidationStatus>>()

            for (bucket in locusBuckets) {
                val tagsResult = s3Client.getBucketTags(creds, bucket)
                val status =
                    when (tagsResult) {
                        is LocusResult.Success -> {
                            val tags = tagsResult.data
                            val hasLocusTag = tags[TAG_LOCUS_ROLE] == TAG_DEVICE_BUCKET
                            if (hasLocusTag) {
                                BucketValidationStatus.Available
                            } else {
                                BucketValidationStatus.Invalid(BucketValidationError.MissingLocusTag)
                            }
                        }
                        is LocusResult.Failure -> {
                            BucketValidationStatus.Invalid(BucketValidationError.AccessDenied)
                        }
                    }
                results.add(bucket to status)
            }

            return LocusResult.Success(results)
        }

        companion object {
            private const val BUCKET_PREFIX = "locus-"
            private const val TAG_LOCUS_ROLE = "LocusRole"
            private const val TAG_DEVICE_BUCKET = "DeviceBucket"
        }
    }
