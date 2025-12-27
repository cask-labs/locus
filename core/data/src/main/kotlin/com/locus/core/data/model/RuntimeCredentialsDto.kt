package com.locus.core.data.model

import com.locus.core.domain.model.auth.RuntimeCredentials
import kotlinx.serialization.Serializable

@Serializable
data class RuntimeCredentialsDto(
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucketName: String,
    val region: String,
    val accountId: String,
    val telemetrySalt: String? = null,
) {
    companion object
}

fun RuntimeCredentialsDto.toDomain() =
    RuntimeCredentials(
        accessKeyId = accessKeyId,
        secretAccessKey = secretAccessKey,
        bucketName = bucketName,
        region = region,
        accountId = accountId,
        telemetrySalt = telemetrySalt,
    )

fun RuntimeCredentials.toDto() =
    RuntimeCredentialsDto(
        accessKeyId = accessKeyId,
        secretAccessKey = secretAccessKey,
        bucketName = bucketName,
        region = region,
        accountId = accountId,
        telemetrySalt = telemetrySalt,
    )
