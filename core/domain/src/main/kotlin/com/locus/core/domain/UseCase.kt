package com.locus.core.domain

/**
 * Marker interface for all Use Cases in the Domain Layer.
 *
 * Use Cases should implement the `invoke` operator function.
 * Example:
 * ```
 * class MyUseCase : UseCase {
 *     suspend operator fun invoke(params: Params): LocusResult<Type>
 * }
 * ```
 */
interface UseCase
