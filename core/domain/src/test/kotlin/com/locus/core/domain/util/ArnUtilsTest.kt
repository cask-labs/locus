package com.locus.core.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArnUtilsTest {
    @Test
    fun `extractAccountId returns account id when arn is valid`() {
        val arn = "arn:aws:s3:us-east-1:123456789012:resource"
        val accountId = ArnUtils.extractAccountId(arn)
        assertThat(accountId).isEqualTo("123456789012")
    }

    @Test
    fun `extractAccountId returns null when arn is invalid`() {
        val arn = "invalid-arn"
        val accountId = ArnUtils.extractAccountId(arn)
        assertThat(accountId).isNull()
    }

    @Test
    fun `extractAccountId returns null when arn is missing account id`() {
        val arn = "arn:aws:s3:us-east-1"
        val accountId = ArnUtils.extractAccountId(arn)
        assertThat(accountId).isNull()
    }
}
