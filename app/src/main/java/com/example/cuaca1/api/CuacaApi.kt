package com.example.cuaca1.api

import com.example.cuaca1.model.ForecastResponse
import com.example.cuaca1.model.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CuacaApi {

    // Fetch cuaca saat ini
    @GET("data/2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "id" // Tambahkan param lang agar deskripsi cuaca Bahasa Indonesia
    ): Call<WeatherResponse>

    // Fetch cuaca prakiraan 5 hari / per 3 jam
    @GET("data/2.5/forecast")
    fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "id"
    ): Call<ForecastResponse>

}