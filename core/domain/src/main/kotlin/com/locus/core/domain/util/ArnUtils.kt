package com.locus.core.domain.util

object ArnUtils {
    /**
     * Extracts the Account ID from an ARN string.
     * ARN format: arn:partition:service:region:account-id:resource-id
     *
     * @param arn The ARN string to parse
     * @return The Account ID if found, null otherwise
     */
    fun extractAccountId(arn: String): String? {
        // ARN format is colon-separated
        // Index 4 corresponds to account-id
        return arn.split(":").getOrNull(4)
    }
}
