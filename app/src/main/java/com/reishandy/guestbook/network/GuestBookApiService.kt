package com.reishandy.guestbook.network

import retrofit2.http.GET

interface GuestBookApiService {
    @GET("/")
    suspend fun checkConnection(): ConnectionResponse
}

data class ConnectionResponse(
    val message: String
)