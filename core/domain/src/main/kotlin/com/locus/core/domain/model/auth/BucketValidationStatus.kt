package com.locus.core.domain.model.auth

/**
 * Result of a bucket validation check.
 */
sealed class BucketValidationStatus {
    data object Validating : BucketValidationStatus()

    data object Available : BucketValidationStatus()

    data object Invalid : BucketValidationStatus()
}
