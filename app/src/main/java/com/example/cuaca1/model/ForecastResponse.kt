package com.example.cuaca1.model

import com.google.gson.annotations.SerializedName

data class ForecastResponse(
    @SerializedName("list") val list: List<ForecastItem>,
    @SerializedName("city") val city: CityInfo
)

data class ForecastItem(
    @SerializedName("dt") val dt: Long,
    @SerializedName("main") val main: MainForecast,
    @SerializedName("weather") val weather: List<WeatherDescription>,
    @SerializedName("dt_txt") val dtTxt: String
)

data class MainForecast(
    @SerializedName("temp") val temp: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double
)

data class WeatherDescription(
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)

data class CityInfo(
    @SerializedName("name") val name: String
)