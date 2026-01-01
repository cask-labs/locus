package com.locus.core.domain.infrastructure

interface ResourceProvider {
    /**
     * Loads the CloudFormation template (locus-stack.yaml).
     */
    fun getStackTemplate(): String
}
