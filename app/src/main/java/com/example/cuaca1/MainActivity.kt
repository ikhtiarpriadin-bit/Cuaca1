package com.example.cuaca1

import android.Manifest
import androidx.appcompat.widget.SearchView
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

        // Adjust padding agar tidak tertutup gesture bar bawah
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Hanya beri padding bottom agar status bar atas menyatu penuh dengan background
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        searchView = findViewById(R.id.searchView)

        searchView.setOnCloseListener {

            searchView.visibility = View.GONE
            true

        }

        ivSearch = findViewById(R.id.btnSearch)
        ivSearch.setOnClickListener {
            ivSearch.visibility = View.GONE

            searchView.visibility = View.VISIBLE
            searchView.isIconified = false
            searchView.requestFocus()
        }
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {

                if (!query.isNullOrEmpty()) {
                    cariKota(query)
                }

                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // Bind SwipeRefreshLayout
        swipeRefresh = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            Toast.makeText(this, "Memperbarui data cuaca...", Toast.LENGTH_SHORT).show()
            dapatkanLokasiPerangkat()
        }

        // Bind View ID Utama
        ivBackground = findViewById(R.id.ivBackground)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)
        tvLocation = findViewById(R.id.tvLocation)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvCondition = findViewById(R.id.tvCondition)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)

        // Bind View ID Detail
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWind = findViewById(R.id.tvWind)
        tvPressure = findViewById(R.id.tvPressure)
        tvVisibility = findViewById(R.id.tvVisibility)

        // Bind & Set Layout Manager untuk RecyclerView Forecast
        rvHourlyForecast = findViewById(R.id.rvHourlyForecast)
        rvDailyForecast = findViewById(R.id.rvDailyForecast)

        rvHourlyForecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvDailyForecast.layoutManager = LinearLayoutManager(this)

        cekAtauMintaIzinLokasi()
    }
    private fun cariKota(city: String) {

        swipeRefresh.isRefreshing = true

        RetrofitClient.api.getWeatherByCity(
            city = city,
            apiKey = API_KEY
        ).enqueue(object : Callback<WeatherResponse> {

            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {

                swipeRefresh.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {

                    val data = response.body()!!

                    updateUI(data)

                    val weather = data.weather.firstOrNull()

                    updateBackground(weather?.icon ?: "01d")

                    ambilForecast(data.coord.lat, data.coord.lon)

                } else {

                    Toast.makeText(
                        this@MainActivity,
                        "Kota tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()

                }

            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {

                swipeRefresh.isRefreshing = false

                Toast.makeText(
                    this@MainActivity,
                    "Gagal mencari kota",
                    Toast.LENGTH_SHORT
                ).show()

            }

        })

    }
    private fun ambilForecast(
        latitude: Double,
        longitude: Double
    ) {

        RetrofitClient.api.getForecast(
            lat = latitude,
            lon = longitude,
            apiKey = API_KEY
        ).enqueue(object : Callback<ForecastResponse> {

            override fun onResponse(
                call: Call<ForecastResponse>,
                response: Response<ForecastResponse>
            ) {

                if (response.isSuccessful && response.body() != null) {

                    val fullList = response.body()!!.list

                    rvHourlyForecast.adapter =
                        HourlyAdapter(fullList.take(8))

                    val grouped = fullList.groupBy {
                        it.dtTxt.substring(0, 10)
                    }

                    val dailyList = mutableListOf<ForecastItem>()

                    grouped.forEach { (_, items) ->

                        val min = items.minOf { it.main.tempMin }
                        val max = items.maxOf { it.main.tempMax }

                        val sample =
                            items.find { it.dtTxt.contains("12:00") }
                                ?: items.first()

                        dailyList.add(
                            sample.copy(
                                main = sample.main.copy(
                                    tempMin = min,
                                    tempMax = max
                                )
                            )
                        )
                    }

                    rvDailyForecast.adapter =
                        DailyAdapter(dailyList)

                }

            }

            override fun onFailure(
                call: Call<ForecastResponse>,
                t: Throwable
            ) {
            }

        })

    }

    private fun setStatusBarTransparent() {
        window.apply {
            statusBarColor = Color.TRANSPARENT
            WindowCompat.getInsetsController(this, decorView).apply {
                isAppearanceLightStatusBars = false // False = Ikon (Jam, Baterai, Wi-Fi) Putih
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
                    Log.d("GPS", "Lat: ${location.latitude}, Lon: ${location.longitude}")
                    ambilDataCuaca(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Tidak dapat mendeteksi GPS, memuat lokasi default", Toast.LENGTH_SHORT).show()
                    ambilDataCuaca(-3.3162, 114.5938)
                }
            }
            .addOnFailureListener { e ->
                Log.e("GPS", "Gagal mendapatkan lokasi: ${e.message}")
                ambilDataCuaca(-3.3162, 114.5938)
            }
    }

    private fun ambilDataCuaca(latitude: Double, longitude: Double) {
        // 1. Panggil Current Weather API
        RetrofitClient.api.getWeather(
            lat = latitude,
            lon = longitude,
            apiKey = API_KEY
        ).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val weather = data.weather.firstOrNull()
                    updateUI(data)

                    val iconCode = weather?.icon ?: "01d"
                    updateBackground(iconCode)

                    Toast.makeText(this@MainActivity, "Data cuaca berhasil diperbarui", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("CUACA", "Error Response Current: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Log.e("CUACA", "Failure Current: ${t.message}")
                Toast.makeText(this@MainActivity, "Gagal memperbarui data cuaca", Toast.LENGTH_SHORT).show()
            }
        })

        // 2. Panggil Forecast API (Per Jam & 5 Hari)
        RetrofitClient.api.getForecast(
            lat = latitude,
            lon = longitude,
            apiKey = API_KEY
        ).enqueue(object : Callback<ForecastResponse> {
            override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val fullList = response.body()!!.list

                    val hourlyList = fullList.take(8)
                    rvHourlyForecast.adapter = HourlyAdapter(hourlyList)

                    val groupedByDate = fullList.groupBy { it.dtTxt.substring(0, 10) }
                    val dailyList = mutableListOf<ForecastItem>()

                    for ((_, itemsInDay) in groupedByDate) {
                        val realMinTemp = itemsInDay.minOfOrNull { it.main.tempMin } ?: 0.0
                        val realMaxTemp = itemsInDay.maxOfOrNull { it.main.tempMax } ?: 0.0

                        val sampleItem =
                            itemsInDay.find { it.dtTxt.contains("09:00:00") }
                                ?: itemsInDay.find { it.dtTxt.contains("12:00:00") }
                                ?: itemsInDay.find { it.dtTxt.contains("15:00:00") }
                                ?: itemsInDay.first()

                        val updatedMain = sampleItem.main.copy(
                            tempMin = realMinTemp,
                            tempMax = realMaxTemp
                        )

                        dailyList.add(sampleItem.copy(main = updatedMain))
                    }

                    rvDailyForecast.adapter = DailyAdapter(dailyList)

                } else {
                    Log.e("CUACA", "Error Response Forecast: ${response.code()}")
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
            "Mist" -> "Berkabut"
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
    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (searchView.visibility == View.VISIBLE) {

            searchView.setQuery("", false)
            searchView.clearFocus()
            searchView.visibility = View.GONE

            ivSearch.visibility = View.VISIBLE
            return
        }

        super.onBackPressed()
    }
}