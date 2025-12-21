# Deep Dive: Domain Primitives

## Context

The "Pure Domain" layer is the core of the application. It must remain independent of the Android framework. To achieve this, we need a standard way to propagate success and failure (including Technical errors) without relying on Android exceptions or libraries. This document defines the `LocusResult` primitive and the standard `UseCase` pattern.

## 1. `LocusResult<T>`

**Purpose:** A sealed class hierarchy to represent the outcome of a domain operation. It explicitly models failure, forcing the consumer to handle it. It is preferred over `kotlin.Result` to avoid potential future collisions with the standard library inline class and to allow richer error hierarchies if needed.

**Definition:**

```kotlin
package com.locus.core.domain

sealed class LocusResult<out T> {
    data class Success<out T>(val data: T) : LocusResult<T>()
    data class Failure(val error: LocusError) : LocusResult<Nothing>()

    // Helper to fold
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (LocusError) -> R
    ): R {
        return when (this) {
            is Success -> onSuccess(data)
            is Failure -> onFailure(error)
        }
    }
}

// Sealed hierarchy for Errors
sealed class LocusError {
    // Technical Errors (Infra/IO)
    data class NetworkError(val cause: Throwable? = null) : LocusError()
    data class StorageError(val cause: Throwable? = null) : LocusError()
    data class UnknownError(val cause: Throwable? = null) : LocusError()

    // Domain Errors (Business Rules)
    data object InvalidState : LocusError()
    data object NotFound : LocusError()
}
```

## 2. Use Case Pattern

**Purpose:** Encapsulate a single unit of business logic.

**Contract:**
- Must be an abstract class or interface (usually a class with a single public function).
- Must utilize the `operator fun invoke` for call-site ergonomics.
- Must be `suspend` by default for I/O operations.
- Must return `LocusResult<T>` for operations that can fail.

**Template:**

```kotlin
package com.locus.core.domain.usecase

import com.locus.core.domain.LocusResult
import javax.inject.Inject

class GetAppVersionUseCase @Inject constructor(
    private val appVersionRepository: AppVersionRepository
) {
    suspend operator fun invoke(): LocusResult<String> {
        return appVersionRepository.getAppVersion()
    }
}
```

## 3. Repository Interface

**Purpose:** Define the contract for data access. Implemented in `:core:data`.

**Contract:**
- Pure Kotlin interface.
- Returns domain models (no Room Entities, no Retrofit DTOs).
- Returns `LocusResult` for fallible operations.
- Uses `Flow` for reactive streams.

**Template:**

```kotlin
package com.locus.core.domain.repository

import com.locus.core.domain.LocusResult
import kotlinx.coroutines.flow.Flow

interface AppVersionRepository {
    // One-shot
    suspend fun getAppVersion(): LocusResult<String>

    // Stream
    fun getAppVersionStream(): Flow<LocusResult<String>>
}
```

## Decisions

1.  **No Exceptions:** We strictly use `LocusResult` for control flow. Exceptions are for crashes (programmer error).
2.  **Sealed Errors:** `LocusError` allows the UI to react specifically to different error types (e.g., Show Toast for Network, Show Dialog for Fatal).
3.  **Operator Invoke:** Makes use cases look like functions: `val version = getAppVersion()`.
