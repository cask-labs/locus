package com.locus.core.domain.model.auth

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BucketValidationStatusTest {
    @Test
    fun `Validating state exists`() {
        assertThat(BucketValidationStatus.Validating).isInstanceOf(BucketValidationStatus::class.java)
    }

    @Test
    fun `Available state exists`() {
        assertThat(BucketValidationStatus.Available).isInstanceOf(BucketValidationStatus::class.java)
    }

    @Test
    fun `Invalid state exists`() {
        val status = BucketValidationStatus.Invalid(BucketValidationError.MissingLocusTag)
        assertThat(status).isInstanceOf(BucketValidationStatus::class.java)
        assertThat(status.reason).isEqualTo(BucketValidationError.MissingLocusTag)
    }
}
