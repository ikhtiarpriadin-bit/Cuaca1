package com.example.cuaca1

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cuaca1.adapter.DailyAdapter
import com.example.cuaca1.adapter.HourlyAdapter
import com.example.cuaca1.api.RetrofitClient
import com.example.cuaca1.model.ForecastItem
import com.example.cuaca1.model.ForecastResponse
import com.example.cuaca1.model.WeatherResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val API_KEY = "277e22a7fbc5477aedc466d91c974316"

    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var ivSearch: ImageView
    private lateinit var searchView: SearchView

    // UI Utama
    private lateinit var ivBackground: ImageView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var tvLocation: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvFeelsLike: TextView

    // Detail Cuaca
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvVisibility: TextView

    // RecyclerView Forecast
    private lateinit var rvHourlyForecast: RecyclerView
    private lateinit var rvDailyForecast: RecyclerView

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                dapatkanLokasiPerangkat()
            }
            else -> {
                Toast.makeText(this, "Izin lokasi ditolak! Menggunakan lokasi default.", Toast.LENGTH_SHORT).show()
                ambilDataCuaca(-3.3162, 114.5938)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1. Set Status Bar Transparan & Ikon Putih
        setStatusBarTransparent()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Bind View ID Utama & Detail
        ivBackground = findViewById(R.id.ivBackground)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)
        tvLocation = findViewById(R.id.tvLocation)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvCondition = findViewById(R.id.tvCondition)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)

        tvHumidity = findViewById(R.id.tvHumidity)
        tvWind = findViewById(R.id.tvWind)
        tvPressure = findViewById(R.id.tvPressure)
        tvVisibility = findViewById(R.id.tvVisibility)

        // Bind RecyclerView
        rvHourlyForecast = findViewById(R.id.rvHourlyForecast)
        rvDailyForecast = findViewById(R.id.rvDailyForecast)
        rvHourlyForecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvDailyForecast.layoutManager = LinearLayoutManager(this)

        // Setup SearchView & Search Button
        ivSearch = findViewById(R.id.btnSearch)
        searchView = findViewById(R.id.searchView)
        searchView.visibility = View.GONE

        ivSearch.setOnClickListener { openSearch() }

        searchView.setOnCloseListener {
            closeSearch()
            true
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    cariKota(query.trim())
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })

        // Bind SwipeRefreshLayout
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            dapatkanLokasiPerangkat()
        }

        cekAtauMintaIzinLokasi()
    }

    private fun setStatusBarTransparent() {
        window.apply {
            statusBarColor = Color.TRANSPARENT
            WindowCompat.getInsetsController(this, decorView).apply {
                isAppearanceLightStatusBars = false
            }
        }
    }

    private fun cekAtauMintaIzinLokasi() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            dapatkanLokasiPerangkat()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun dapatkanLokasiPerangkat() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    ambilDataCuaca(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Tidak dapat mendeteksi GPS, memuat lokasi default", Toast.LENGTH_SHORT).show()
                    ambilDataCuaca(-3.3162, 114.5938)
                }
            }
            .addOnFailureListener {
                ambilDataCuaca(-3.3162, 114.5938)
            }
    }

    // Panggil API Cuaca berdasarkan Koordinat (GPS)
    private fun ambilDataCuaca(latitude: Double, longitude: Double) {
        swipeRefresh.isRefreshing = true

        RetrofitClient.api.getWeather(
            lat = latitude,
            lon = longitude,
            apiKey = API_KEY
        ).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    updateUI(data)
                    updateBackground(data.weather.firstOrNull()?.icon ?: "01d")
                    ambilForecast(data.coord.lat, data.coord.lon)
                } else {
                    Toast.makeText(this@MainActivity, "Gagal memuat data cuaca", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@MainActivity, "Gagal terhubung ke jaringan", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Panggil API Cuaca berdasarkan Nama Kota
    private fun cariKota(city: String) {
        swipeRefresh.isRefreshing = true

        RetrofitClient.api.getWeatherByCity(
            city = city,
            apiKey = API_KEY
        ).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    updateUI(data)
                    updateBackground(data.weather.firstOrNull()?.icon ?: "01d")
                    ambilForecast(data.coord.lat, data.coord.lon)
                    closeSearch()
                } else {
                    Toast.makeText(this@MainActivity, "Kota \"$city\" tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@MainActivity, "Gagal mencari kota", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Fungsi Terpusat Mengambil & Memproses Forecast (Per Jam & 5 Hari)
    private fun ambilForecast(latitude: Double, longitude: Double) {
        RetrofitClient.api.getForecast(
            lat = latitude,
            lon = longitude,
            apiKey = API_KEY
        ).enqueue(object : Callback<ForecastResponse> {
            override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val fullList = response.body()!!.list

                    // Hourly: 8 interval 3-jam pertama
                    rvHourlyForecast.adapter = HourlyAdapter(fullList.take(8))

                    // Daily: Grouping berdasarkan tanggal
                    val groupedByDate = fullList.groupBy { it.dtTxt.substring(0, 10) }
                    val dailyList = mutableListOf<ForecastItem>()

                    for ((_, itemsInDay) in groupedByDate) {
                        val realMinTemp = itemsInDay.minOfOrNull { it.main.tempMin } ?: 0.0
                        val realMaxTemp = itemsInDay.maxOfOrNull { it.main.tempMax } ?: 0.0

                        val sampleItem = itemsInDay.find { it.dtTxt.contains("12:00:00") }
                            ?: itemsInDay.find { it.dtTxt.contains("09:00:00") }
                            ?: itemsInDay.first()

                        val updatedMain = sampleItem.main.copy(
                            tempMin = realMinTemp,
                            tempMax = realMaxTemp
                        )

                        dailyList.add(sampleItem.copy(main = updatedMain))
                    }

                    rvDailyForecast.adapter = DailyAdapter(dailyList)
                }
            }

            override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                Log.e("CUACA", "Failure Forecast: ${t.message}")
            }
        })
    }

    private fun translateWeather(weather: String): String {
        return when (weather) {
            "Clear" -> "Cerah"
            "Clouds" -> "Berawan"
            "Rain" -> "Hujan"
            "Thunderstorm" -> "Badai Petir"
            "Drizzle" -> "Gerimis"
            "Mist", "Fog" -> "Berkabut"
            else -> weather
        }
    }

    private fun updateUI(data: WeatherResponse) {
        val weather = data.weather.firstOrNull()
        tvLocation.text = data.name
        tvTemperature.text = "${data.main.temp.toInt()}°C"
        tvCondition.text = translateWeather(weather?.main ?: "")
        tvFeelsLike.text = "Terasa seperti ${data.main.feelsLike.toInt()}°C"
        tvHumidity.text = "${data.main.humidity}%"
        tvWind.text = "${data.wind.speed} m/s"
        tvPressure.text = "${data.main.pressure} hPa"
        tvVisibility.text = "${data.visibility / 1000} km"
    }

    private fun updateBackground(iconCode: String) {
        when (iconCode) {
            "01d" -> {
                ivBackground.setImageResource(R.drawable.sun)
                ivWeatherIcon.setImageResource(R.drawable.ic_sun)
            }
            "01n" -> {
                ivBackground.setImageResource(R.drawable.night_clear)
                ivWeatherIcon.setImageResource(R.drawable.ic_moon)
            }
            "02d", "03d", "04d" -> {
                ivBackground.setImageResource(R.drawable.cloud)
                ivWeatherIcon.setImageResource(R.drawable.ic_cloud_sun)
            }
            "02n", "03n", "04n" -> {
                ivBackground.setImageResource(R.drawable.night_cloud)
                ivWeatherIcon.setImageResource(R.drawable.ic_cloud_moon)
            }
            "09d", "09n", "10d", "10n" -> {
                ivBackground.setImageResource(R.drawable.rain)
                ivWeatherIcon.setImageResource(R.drawable.ic_rain)
            }
            else -> {
                ivBackground.setImageResource(R.drawable.sun)
                ivWeatherIcon.setImageResource(R.drawable.ic_sun)
            }
        }
    }

    private fun openSearch() {
        if (searchView.visibility == View.VISIBLE) return

        ivSearch.visibility = View.GONE
        searchView.visibility = View.VISIBLE
        searchView.alpha = 0f
        searchView.translationY = -20f
        searchView.setQuery("", false)
        searchView.isIconified = false
        searchView.requestFocus()

        searchView.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
        }

        searchView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .start()
    }

    private fun closeSearch() {
        if (searchView.visibility == View.GONE) return

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken ?: searchView.windowToken, 0)

        searchView.clearFocus()
        searchView.isIconified = true

        searchView.animate()
            .alpha(0f)
            .translationY(-10f)
            .setDuration(220)
            .withEndAction {
                searchView.setQuery("", false)
                searchView.visibility = View.GONE
                searchView.alpha = 1f
                searchView.translationY = 0f
                ivSearch.visibility = View.VISIBLE
            }
            .start()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (searchView.visibility == View.VISIBLE) {
            closeSearch()
            return
        }
        super.onBackPressed()
    }
}