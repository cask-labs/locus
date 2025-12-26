package com.locus.core.domain.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocusResultTest {
    @Test
    fun `success creates Success instance with correct data`() {
        val result = LocusResult.Success("test data")
        assertThat(result.isSuccess).isTrue()
        assertThat(result.isFailure).isFalse()
        assertThat(result.getOrNull()).isEqualTo("test data")
        assertThat(result.exceptionOrNull()).isNull()
    }

    @Test
    fun `failure creates Failure instance with correct error`() {
        val exception = RuntimeException("oops")
        val result = LocusResult.Failure(exception)
        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
        assertThat(result.getOrNull()).isNull()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }

    @Test
    fun `domain exception can be used in failure`() {
        val exception = DomainException.NetworkError.Offline
        val result = LocusResult.Failure(exception)
        assertThat(result.exceptionOrNull()).isInstanceOf(DomainException.NetworkError::class.java)
        assertThat((result.exceptionOrNull() as DomainException.NetworkError)).isEqualTo(DomainException.NetworkError.Offline)
    }
}
