package com.locus.core.domain.model.auth

data class StackProvisioningResult(
    val stackId: String,
    val outputs: Map<String, String>,
)
