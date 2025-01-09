package com.reishandy.guestbook.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitFactory {

    private var retrofit: Retrofit? = null

    fun getInstance(baseUrl: String): Retrofit {
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit!!
    }
}