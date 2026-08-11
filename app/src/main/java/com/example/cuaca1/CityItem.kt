package com.example.cuaca1.model

data class CityItem(
    val name: String,
    var temp: String = "--°",
    var condition: String = "Memuat...",
    var tempRange: String = "--° / --°"
)