package com.example.cuaca1.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val coord: Coord,
    val name: String,
    val weather: List<Weather>,
    val main: Main,
    val wind: Wind,
    val visibility: Int,
    val sys: Sys
)

data class Coord(
    val lat: Double,
    val lon: Double
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)

data class Main(
    val temp: Double,

    @SerializedName("feels_like")
    val feelsLike: Double,

    @SerializedName("temp_min")
    val tempMin: Double,

    @SerializedName("temp_max")
    val tempMax: Double,

    val pressure: Int,
    val humidity: Int
)

data class Wind(
    val speed: Double
)

data class Sys(
    val sunrise: Long,
    val sunset: Long
)