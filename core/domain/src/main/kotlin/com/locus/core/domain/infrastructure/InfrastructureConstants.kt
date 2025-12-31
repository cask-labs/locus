package com.locus.core.domain.infrastructure

/**
 * Shared constants used for CloudFormation infrastructure provisioning and recovery.
 */
object InfrastructureConstants {
    const val POLL_INTERVAL = 5_000L
    const val POLL_TIMEOUT = 600_000L // 10 minutes

    const val TAG_STACK_NAME = "aws:cloudformation:stack-name"
    const val TAG_LOCUS_ROLE = "LocusRole"
    const val TAG_DEVICE_BUCKET = "DeviceBucket"

    const val OUT_RUNTIME_ACCESS_KEY = "RuntimeAccessKeyId"
    const val OUT_RUNTIME_SECRET_KEY = "RuntimeSecretAccessKey"
    const val OUT_BUCKET_NAME = "BucketName"

    const val STATUS_CREATE_COMPLETE = "CREATE_COMPLETE"
    const val STATUS_CREATE_FAILED = "CREATE_FAILED"
    const val STATUS_ROLLBACK_IN_PROGRESS = "ROLLBACK_IN_PROGRESS"
    const val STATUS_ROLLBACK_COMPLETE = "ROLLBACK_COMPLETE"

    // Add other permanent error statuses if needed
    val PERMANENT_ERROR_STATUSES =
        setOf(
            STATUS_CREATE_FAILED,
            STATUS_ROLLBACK_IN_PROGRESS,
            STATUS_ROLLBACK_COMPLETE,
        )
}
