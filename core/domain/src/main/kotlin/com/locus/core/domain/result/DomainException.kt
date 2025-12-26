package com.locus.core.domain.result

/**
 * Base exception class for all Domain Layer exceptions.
 */
sealed class DomainException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    // Network Errors
    sealed class NetworkError(message: String? = null, cause: Throwable? = null) :
        DomainException(message, cause) {
        data object Offline : NetworkError("Network is offline")

        data class Timeout(override val message: String = "Request timed out") :
            NetworkError(message)

        data class ServerError(val code: Int, override val message: String) :
            NetworkError("Server error $code: $message")

        data class Generic(override val cause: Throwable) :
            NetworkError(cause.message, cause)
    }

    // Authentication Errors
    sealed class AuthError(message: String? = null, cause: Throwable? = null) :
        DomainException(message, cause) {
        data object InvalidCredentials : AuthError("Invalid credentials provided")

        data object Expired : AuthError("Credentials have expired")

        data object AccessDenied : AuthError("Access denied")

        data class Generic(override val cause: Throwable) : AuthError(cause.message, cause)
    }

    // S3/Storage Errors
    sealed class S3Error(message: String? = null, cause: Throwable? = null) :
        DomainException(message, cause) {
        data class BucketNotFound(val bucketName: String) :
            S3Error("Bucket '$bucketName' not found")

        data class Generic(override val cause: Throwable) : S3Error(cause.message, cause)
    }

    // Battery Errors
    data object BatteryCriticalException : DomainException("Battery level is critical")

    // Provisioning Errors
    sealed class ProvisioningError(message: String? = null, cause: Throwable? = null) :
        DomainException(message, cause) {
        data class StackExists(val stackName: String) :
            ProvisioningError("Stack '$stackName' already exists")

        data class Permissions(override val message: String) : ProvisioningError(message)

        data class Quota(override val message: String) : ProvisioningError(message)

        data class DeploymentFailed(override val message: String) : ProvisioningError(message)

        data class Wait(override val message: String) : ProvisioningError(message)
    }
}
