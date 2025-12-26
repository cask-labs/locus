package com.locus.core.domain.model.auth

/**
 * Long-lived credentials used for day-to-day operations (logging, syncing).
 * These typically belong to a restricted IAM User with only S3 permissions.
 *
 * @property accessKeyId The AWS Access Key ID.
 * @property secretAccessKey The AWS Secret Access Key.
 * @property sessionToken Optional Session Token (Null for standard IAM Users, present for temporary assume role).
 */
data class RuntimeCredentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String? = null,
)
