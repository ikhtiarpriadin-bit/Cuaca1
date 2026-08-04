package com.example.cuaca1.api

import CuacaApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather?lat=-3.3186&lon=114.5908&appid=277e22a7fbc5477aedc466d91c974316&units=metric"

    val api: CuacaApi by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CuacaApi::class.java)

    }

}