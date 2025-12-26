package com.locus.core.domain.result

sealed class LocusResult<out T> {
    data class Success<out T>(val data: T) : LocusResult<T>()

    data class Failure(val error: Throwable) : LocusResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? =
        when (this) {
            is Success -> data
            is Failure -> null
        }

    fun exceptionOrNull(): Throwable? =
        when (this) {
            is Success -> null
            is Failure -> error
        }
}
