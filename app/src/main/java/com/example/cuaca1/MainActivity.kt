package com.example.cuaca1

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cuaca1.api.RetrofitClient
import com.example.cuaca1.model.WeatherResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val API_KEY = "277e22a7fbc5477aedc466d91c974316"

    // FusedLocationProviderClient untuk mengambil lokasi perangkat
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Deklarasi Komponen UI Utama
    private lateinit var ivBackground: ImageView
    private lateinit var tvLocation: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvFeelsLike: TextView

    // Deklarasi Komponen UI 5 Fitur Baru
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvUV: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvVisibility: TextView

    // Register callback untuk meminta permission lokasi di runtime
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Izin disetujui, ambil lokasi GPS
                dapatkanLokasiPerangkat()
            }
            else -> {
                Toast.makeText(this, "Izin lokasi ditolak! Menggunakan lokasi default.", Toast.LENGTH_SHORT).show()
                // Jika izin ditolak, jalankan dengan koordinat default (Banjarmasin)
                ambilDataCuaca(-3.3162, 114.5938)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inisialisasi Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Hubungkan ID XML Utama
        ivBackground = findViewById(R.id.ivBackground)
        tvLocation = findViewById(R.id.tvLocation)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvCondition = findViewById(R.id.tvCondition)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)

        // Hubungkan ID XML 5 Fitur Baru
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWind = findViewById(R.id.tvWind)
        tvUV = findViewById(R.id.tvUV)
        tvPressure = findViewById(R.id.tvPressure)
        tvVisibility = findViewById(R.id.tvVisibility)

        // Minta Izin Lokasi dan Ambil Data
        cekAtauMintaIzinLokasi()
    }

    /**
     * Mengecek apakah izin lokasi sudah diberikan, jika belum akan meminta izin
     */
    private fun cekAtauMintaIzinLokasi() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            // Sudah dapat izin
            dapatkanLokasiPerangkat()
        } else {
            // Belum dapat izin, munculkan dialog pop-up izin
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * Membaca titik koordinat GPS HP pengguna
     */
    @SuppressLint("MissingPermission")
    private fun dapatkanLokasiPerangkat() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Berhasil dapatkan lokasi nyata pengguna
                    Log.d("GPS", "Lat: ${location.latitude}, Lon: ${location.longitude}")
                    ambilDataCuaca(location.latitude, location.longitude)
                } else {
                    // Jika lokasi lastLocation null (misal GPS baru dinyalakan di emulator)
                    Toast.makeText(this, "Tidak dapat mendeteksi GPS, memuat lokasi default", Toast.LENGTH_SHORT).show()
                    ambilDataCuaca(-3.3162, 114.5938)
                }
            }
            .addOnFailureListener { e ->
                Log.e("GPS", "Gagal mendapatkan lokasi: ${e.message}")
                ambilDataCuaca(-3.3162, 114.5938)
            }
    }

    /**
     * Memanggil API Cuaca berdasarkan koordinat lat dan lon yang dinamis
     */
    private fun ambilDataCuaca(latitude: Double, longitude: Double) {

        RetrofitClient.api.getWeather(
            lat = latitude,
            lon = longitude,
            apiKey = API_KEY
        ).enqueue(object : Callback<WeatherResponse> {

            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {

                if (response.isSuccessful && response.body() != null) {

                    val data = response.body()!!

                    // Ambil deskripsi cuaca secara aman
                    val weatherCondition = data.weather.firstOrNull()?.description ?: "-"

                    // Tampilkan ke Logcat
                    Log.d("CUACA", "Kota: ${data.name}")
                    Log.d("CUACA", "Suhu: ${data.main.temp}")
                    Log.d("CUACA", "Deskripsi: $weatherCondition")

                    // Update UI Utama
                    tvLocation.text = data.name
                    tvTemperature.text = "${data.main.temp.toInt()}°C"
                    tvCondition.text = weatherCondition
                    // Menggunakan feelsLike (bukan feels_like)
                    tvFeelsLike.text = "Feels like ${data.main.feelsLike.toInt()}°C"

                    // Update UI 5 Fitur Baru
                    tvHumidity.text = "${data.main.humidity}%"
                    tvWind.text = "${data.wind.speed} m/s"
                    tvPressure.text = "${data.main.pressure} hPa"
                    tvVisibility.text = "${data.visibility / 1000} km"

                } else {
                    Log.e("CUACA", "Error Response: ${response.code()}")
                }

            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("CUACA", "Failure: ${t.message}")
            }

        })

    }

}