package com.example.cuaca1.model

data class CityItem(
    var name: String,
    var temp: String = "--°",
    var condition: String = "Memuat...",
    var tempRange: String = "--° / --°",
    var iconCode: String = "01d"
)