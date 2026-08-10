package com.example.cuaca1

import android.content.Context
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val rgUnit = findViewById<RadioGroup>(R.id.rgTemperatureUnit)
        val rbCelsius = findViewById<RadioButton>(R.id.rbCelsius)
        val rbFahrenheit = findViewById<RadioButton>(R.id.rbFahrenheit)

        val sharedPref = getSharedPreferences("WeatherPref", Context.MODE_PRIVATE)
        val currentUnit = sharedPref.getString("UNIT", "metric") ?: "metric"

        // Set RadioButton sesuai pilihan tersimpan
        if (currentUnit == "imperial") {
            rbFahrenheit.isChecked = true
        } else {
            rbCelsius.isChecked = true
        }

        // Listener saat pengguna mengganti pilihan
        rgUnit.setOnCheckedChangeListener { _, checkedId ->
            val selectedUnit = if (checkedId == R.id.rbFahrenheit) "imperial" else "metric"
            sharedPref.edit().putString("UNIT", selectedUnit).apply()
        }
    }
}