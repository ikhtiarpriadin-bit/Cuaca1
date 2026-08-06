package com.example.cuaca1.api

import com.example.cuaca1.model.ForecastResponse
import com.example.cuaca1.model.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CuacaApi {

    // Cuaca berdasarkan koordinat GPS
    @GET("data/2.5/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "id"
    ): Call<WeatherResponse>

    // Cuaca berdasarkan nama kota
    @GET("data/2.5/weather")
    fun getWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "id"
    ): Call<WeatherResponse>

    // Forecast 5 hari
    @GET("data/2.5/forecast")
    fun getForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "id"
    ): Call<ForecastResponse>
}