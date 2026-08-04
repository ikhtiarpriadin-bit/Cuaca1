package com.example.cuaca1.api

import CuacaApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.openweathermap.org/"

    val api: CuacaApi by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CuacaApi::class.java)

    }

}