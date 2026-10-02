package com.aniflow.core.common.result

/**
 * Universal Result type for AniFlow operations across all layers.
 * Follows Monadic functional result pattern without throwing uncaught exceptions.
 */
sealed interface AniFlowResult<out T> {

    data class Success<T>(val data: T) : AniFlowResult<T>

    data class Error(
        val error: ErrorType,
        val message: String,
        val cause: Throwable? = null
    ) : AniFlowResult<Nothing>

    data class Loading(val progress: Float? = null) : AniFlowResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> defaultValue
    }

    inline fun <R> map(transform: (T) -> R): AniFlowResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    inline fun onSuccess(action: (T) -> Unit): AniFlowResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (ErrorType, String, Throwable?) -> Unit): AniFlowResult<T> {
        if (this is Error) action(error, message, cause)
        return this
    }
}
