package com.locus.core.domain.infrastructure

import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.result.LocusResult

interface CloudFormationClient {
    /**
     * Creates a CloudFormation stack using the provided template and parameters.
     * Returns the Stack ID on success.
     */
    suspend fun createStack(
        creds: BootstrapCredentials,
        stackName: String,
        template: String,
        parameters: Map<String, String>,
    ): LocusResult<String>

    /**
     * Describes the stack to retrieve its status and outputs.
     * Returns the raw Stack object wrapper or specific status.
     * For domain purity, we'll return a simple data class or LocusResult.
     * Let's return a simple StackStatus model.
     */
    suspend fun describeStack(
        creds: BootstrapCredentials,
        stackName: String,
    ): LocusResult<StackDetails>
}

data class StackDetails(
    val stackId: String?,
    val status: String,
    val outputs: Map<String, String>?,
)
