package com.example.cuaca1

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : androidx.activity.ComponentActivity() {


    // 1. Deklarasi Komponen UI Utama (Atas)
    private lateinit var ivBackground: ImageView
    private lateinit var tvLocation: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvFeelsLike: TextView

    // 2. Deklarasi Komponen UI 5 Fitur Baru (Di dalam CardView Bawah)
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvUV: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvVisibility: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Logika bawaan EdgeToEdge dengan lambda murni AndroidX tanpa anotasi JetBrains
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 3. Hubungkan ID XML Utama ke Kotlin
        ivBackground = findViewById(R.id.ivBackground)
        tvLocation = findViewById(R.id.tvLocation)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvCondition = findViewById(R.id.tvCondition)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)

        // 4. Hubungkan ID XML 5 Fitur Baru ke Kotlin
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWind = findViewById(R.id.tvWind)
        tvUV = findViewById(R.id.tvUV)
        tvPressure = findViewById(R.id.tvPressure)
        tvVisibility = findViewById(R.id.tvVisibility)

        // 5. Jalankan Pengisian Data Cuaca
        tampilkanDataCuaca()
    }

    /**
     * Fungsi untuk mengisi semua teks cuaca secara dinamis
     */
    private fun tampilkanDataCuaca() {
        // Mengisi Informasi Atas
        tvLocation.text = "Banjarmasin"
        tvTemperature.text = "30°"
        tvCondition.text = "Cerah Berawan"
        tvFeelsLike.text = "Terasa seperti 33°C | Kelembaban Tinggi"

        // Mengisi 5 Fitur Baru di Dalam CardView
        tvHumidity.text = "72%"
        tvWind.text = "10 km/h"
        tvUV.text = "4 (Sedang)"
        tvPressure.text = "1011 hPa"
        tvVisibility.text = "9 km"

        // Mengunci background agar menggunakan gambar sun.png Anda
        ivBackground.setImageResource(R.drawable.sun)
    }
}
