package com.example.cuaca1

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import com.example.cuaca1.api.RetrofitClient
import com.example.cuaca1.model.WeatherResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : androidx.activity.ComponentActivity() {

    private val API_KEY = "277e22a7fbc5477aedc466d91c974316"

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
        ambilDataCuaca()
    }

    /**
     * Fungsi untuk mengisi semua teks cuaca secara dinamis
     */
    private fun ambilDataCuaca() {

        RetrofitClient.api.getWeather(
            lat = -3.3162,
            lon = 114.5938,
            apiKey = API_KEY
        ).enqueue(object : Callback<WeatherResponse> {

            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {

                if (response.isSuccessful && response.body() != null) {

                    val data = response.body()!!

                    Log.d("CUACA", data.name)
                    Log.d("CUACA", data.main.temp.toString())
                    Log.d("CUACA", data.weather[0].description)

                }

            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {

                Log.e("CUACA", t.message.toString())

            }

        })

    }

}