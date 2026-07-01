package com.streamflex.core.network

/**
 * Represents the result of a network operation.
 * All network requests should return NetworkResult instead of throwing exceptions.
 */
sealed class NetworkResult<out T> {

    /**
     * Request completed successfully.
     */
    data class Success<T>(
        val data: T
    ) : NetworkResult<T>()

    /**
     * Server returned an HTTP error (404, 500, etc.).
     */
    data class Error(
        val code: Int,
        val message: String,
        val response: NetworkResponse? = null
    ) : NetworkResult<Nothing>()

    /**
     * Connection timed out.
     */
    data class Timeout(
        val message: String = "Request timed out"
    ) : NetworkResult<Nothing>()

    /**
     * No internet connection or DNS failure.
     */
    data class NetworkError(
        val message: String
    ) : NetworkResult<Nothing>()

    /**
     * Redirect limit reached or redirect blocked.
     */
    data class Redirect(
        val location: String?
    ) : NetworkResult<Nothing>()

    /**
     * Any unexpected exception.
     */
    data class Exception(
        val throwable: Throwable
    ) : NetworkResult<Nothing>()
}