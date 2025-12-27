package com.locus.core.data.model

import com.locus.core.domain.model.auth.BootstrapCredentials
import kotlinx.serialization.Serializable

@Serializable
data class BootstrapCredentialsDto(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String,
    val region: String
) {
    companion object
}

fun BootstrapCredentialsDto.toDomain() = BootstrapCredentials(
    accessKeyId = accessKeyId,
    secretAccessKey = secretAccessKey,
    sessionToken = sessionToken,
    region = region
)

fun BootstrapCredentials.toDto() = BootstrapCredentialsDto(
    accessKeyId = accessKeyId,
    secretAccessKey = secretAccessKey,
    sessionToken = sessionToken,
    region = region
)
