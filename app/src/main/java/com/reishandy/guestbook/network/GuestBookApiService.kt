package com.reishandy.guestbook.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface GuestBookApiService {
    @GET("/")
    suspend fun checkConnection(): ConnectionResponse

    @POST("/check-in/{id}")
    suspend fun checkIn(@Path("id") entryId: String): CheckInResponse

    @POST("/reset/{id}")
    suspend fun reset(@Path("id") entryId: String = "all"): ResetResponse
}

data class ConnectionResponse(
    val message: String
)

data class CheckInResponse(
    val message: String,
    val time: String
)

data class ResetResponse(
    val message: String,
    val rows: String
)

data class ErrorResponse(
    val message: String
)