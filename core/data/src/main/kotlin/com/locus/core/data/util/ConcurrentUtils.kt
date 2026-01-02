package com.locus.core.data.util

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> ListenableFuture<T>.await(): T {
    if (isDone) {
        try {
            return get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    return suspendCancellableCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (e: ExecutionException) {
                    continuation.resumeWithException(e.cause ?: e)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            DirectExecutor,
        )

        continuation.invokeOnCancellation {
            cancel(false)
        }
    }
}

private object DirectExecutor : Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}
