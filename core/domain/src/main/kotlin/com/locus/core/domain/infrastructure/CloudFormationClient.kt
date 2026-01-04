package com.locus.core.domain.infrastructure

import com.locus.core.domain.model.auth.BootstrapCredentials
import com.locus.core.domain.result.LocusResult
import java.time.Instant

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
     */
    suspend fun describeStack(
        creds: BootstrapCredentials,
        stackName: String,
    ): LocusResult<StackDetails>

    /**
     * Retrieves the stack events for logging.
     */
    suspend fun describeStackEvents(
        creds: BootstrapCredentials,
        stackName: String,
    ): LocusResult<List<StackEvent>>
}

data class StackDetails(
    val stackId: String?,
    val status: String,
    val outputs: Map<String, String>?,
)

data class StackEvent(
    val eventId: String,
    val timestamp: Instant,
    val logicalResourceId: String,
    val resourceStatus: String,
    val resourceStatusReason: String?,
)
