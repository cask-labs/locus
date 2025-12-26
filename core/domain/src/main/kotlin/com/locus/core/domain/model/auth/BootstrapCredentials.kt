package com.locus.core.domain.model.auth

/**
 * Temporary credentials used only for the initial setup/provisioning phase.
 * These allow the app to deploy CloudFormation stacks and verify buckets.
 *
 * @property accessKeyId The AWS Access Key ID.
 * @property secretAccessKey The AWS Secret Access Key.
 * @property sessionToken The AWS Session Token (Strictly Required for Bootstrap to ensure temporary nature).
 */
data class BootstrapCredentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String,
)
