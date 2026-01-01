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
        val result: LocusResult<String> = LocusResult.Failure(exception)
        assertThat(result.isSuccess).isFalse()
        assertThat(result.isFailure).isTrue()
        assertThat(result.getOrNull()).isNull()
        assertThat(result.exceptionOrNull()).isEqualTo(exception)
    }

    @Test
    fun `domain exception can be used in failure`() {
        val exception = DomainException.NetworkError.Offline
        val result = LocusResult.Failure(exception)
        val error: Throwable? = result.exceptionOrNull()
        assertThat(error).isInstanceOf(DomainException.NetworkError::class.java)
        assertThat(error as? DomainException.NetworkError).isEqualTo(DomainException.NetworkError.Offline)
    }

    @Test
    fun `verify Success equality and hashcode`() {
        val result1 = LocusResult.Success("data")
        val result2 = LocusResult.Success("data")
        val result3 = LocusResult.Success("other")

        assertThat(result1).isEqualTo(result2)
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode())
        assertThat(result1).isNotEqualTo(result3)
    }

    @Test
    fun `verify Success toString`() {
        val result = LocusResult.Success("data")
        assertThat(result.toString()).contains("data")
    }

    @Test
    fun `verify Failure equality and hashcode`() {
        val ex = RuntimeException("error")
        val result1 = LocusResult.Failure(ex)
        val result2 = LocusResult.Failure(ex)
        val result3 = LocusResult.Failure(RuntimeException("other"))

        assertThat(result1).isEqualTo(result2)
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode())
        assertThat(result1).isNotEqualTo(result3)
    }

    @Test
    fun `verify Failure toString`() {
        val ex = RuntimeException("error")
        val result = LocusResult.Failure(ex)
        assertThat(result.toString()).contains("error")
    }
}
