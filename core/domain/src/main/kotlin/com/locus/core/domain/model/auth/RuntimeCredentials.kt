package com.locus.core.domain.model.auth

/**
 * Long-lived credentials used for day-to-day operations (logging, syncing).
 * These typically belong to a restricted IAM User with only S3 permissions.
 *
 * @property accessKeyId The AWS Access Key ID.
 * @property secretAccessKey The AWS Secret Access Key.
 * @property bucketName The S3 bucket name.
 * @property region The AWS region.
 * @property accountId The AWS account ID.
 * @property telemetrySalt A unique salt for anonymizing telemetry data.
 */
data class RuntimeCredentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucketName: String,
    val region: String,
    val accountId: String,
    val telemetrySalt: String? = null,
)
