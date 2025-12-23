package com.locus.core.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocusResultTest {

    @Test
    fun `success returns data and null exception`() {
        val data = "test"
        val result: LocusResult<String> = LocusResult.Success(data)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.isFailure).isFalse()
        assertThat(result.getOrNull()).isEqualTo(data)
        assertThat(result.exceptionOrNull()).isNull()
    }

    @Test
    fun `failure returns error and null data`() {
        val error = RuntimeException("boom")
        val result: LocusResult<String> = LocusResult.Failure(error)

        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
        assertThat(result.getOrNull()).isNull()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
    }
}
