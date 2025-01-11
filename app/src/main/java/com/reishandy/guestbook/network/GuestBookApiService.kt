package com.reishandy.guestbook.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface GuestBookApiService {
    @GET("/")
    suspend fun checkConnection(): ConnectionResponse

    @POST("/check-in/{id}")
    suspend fun checkIn(@Path("id") entryId: String): CheckInResponse

    @POST("/reset/{id}")
    suspend fun reset(@Path("id") entryId: String = "all"): ResetResponse

    @Multipart
    @POST("/data")
    suspend fun importCSV(@Part file: MultipartBody.Part): ImportResponse

    @GET("/data")
    suspend fun getCsvData(): ResponseBody
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

data class ImportResponse(
    val message: String,
    val rows: String
)