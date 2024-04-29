package live.talkshop.sdk.utils.helpers

sealed class Either<out E, out V> {
    data class Error<out E>(val error: E) : Either<E, Nothing>()
    data class Result<out V>(val value: V) : Either<Nothing, V>()

    /**
     * Returns true if this is an Error, false otherwise.
     */
    val onError get() = this is Error<E>

    /**
     * Returns true if this is a Result, false otherwise.
     */
    val onResult get() = this is Result<V>

    /**
     * Executes the given code block if this is an Error.
     * @param fn Lambda function to execute if this is an Error.
     */
    inline fun onError(fn: (E) -> Unit): Either<E, V> {
        if (this is Error) fn(error)
        return this
    }

    /**
     * Executes the given code block if this is a Result.
     * @param fn Lambda function to execute if this is a Result.
     */
    inline fun onResult(fn: (V) -> Unit): Either<E, V> {
        if (this is Result) fn(value)
        return this
    }
}