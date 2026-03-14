package com.mnikita.knowyourrunway.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// Matches common PHP JSON error shapes like: {"error":"..."} or {"message":"..."}
private data class ApiError(
    val error: String? = null,
    val message: String? = null
)

private val moshi by lazy {
    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
}
private val apiErrorAdapter by lazy {
    moshi.adapter(ApiError::class.java)
}

fun Throwable.toUserMessage(): String {
    return when (this) {
        is UnknownHostException -> "No internet connection. Please check your network."
        is SocketTimeoutException -> "The server is taking too long to respond. Please try again."
        is IOException -> "Network error. Please try again."

        is HttpException -> {
            val code = this.code()
            val body = try {
                this.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }

            val parsedMsg = try {
                body?.let { apiErrorAdapter.fromJson(it) }?.let { it.error ?: it.message }
            } catch (_: Exception) {
                null
            }

            if (!parsedMsg.isNullOrBlank()) return parsedMsg

            // fallback by status code
            when (code) {
                400 -> "Please check your input and try again."
                401 -> "Incorrect email or password."
                403 -> "Access denied. Please verify your account or try again."
                404 -> "Service not found. Please update the app or try again."
                429 -> "Too many attempts. Please wait and try again."
                in 500..599 -> "Server error. Please try again in a moment."
                else -> "Request failed (HTTP $code). Please try again."
            }
        }

        else -> this.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
    }
}