package com.locus.core.domain.model.auth

/**
 * Detailed reasons for bucket validation failure.
 */
sealed interface BucketValidationError {
    data object MissingLocusTag : BucketValidationError

    data object AccessDenied : BucketValidationError

    data class Unknown(val message: String) : BucketValidationError
}
