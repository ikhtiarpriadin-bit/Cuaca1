package com.example.cuaca1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // --- CONSTANTS & SERVICES ---
    private val apiKey = "277e22a7fbc5477aedc466d91c974316"
    private val defaultLat = -3.3162
    private val defaultLon = 114.5938

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // --- UI COMPONENTS ---
    private lateinit var ivBackground: ImageView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var tvLocation: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvCondition: TextView
    private lateinit var tvFeelsLike: TextView

    // Detail Views
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvVisibility: TextView
    private lateinit var tvSunrise: TextView
    private lateinit var tvSunset: TextView

    // Top Navigation & Search Views
    private lateinit var ivSearch: ImageView
    private lateinit var ivSettings: ImageView
    private lateinit var searchView: SearchView
    private lateinit var nestedScrollView: NestedScrollView

    // Custom Pull-to-Refresh Views
    private lateinit var pullContainer: LinearLayout
    private lateinit var tvPullToRefresh: TextView
    private var isRefreshing = false

    // RecyclerViews
    private lateinit var rvHourlyForecast: RecyclerView
    private lateinit var rvDailyForecast: RecyclerView

    // Variable untuk menyimpan kota/koordinat aktif saat ini
    private var currentCityName: String? = null
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    // Permission Handler
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

        if (isGranted) {
            dapatkanLokasiPerangkat()
        } else {
            Toast.makeText(this, "Izin lokasi ditolak. Menampilkan lokasi default.", Toast.LENGTH_SHORT).show()
            ambilDataCuaca(defaultLat, defaultLon)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setStatusBarTransparent()
        initViews()
        setupRecyclerViews()
        setupScrollFadeAnimation()
        setupCustomPullToRefresh()
        setupSearchView()
        setupSettingsButton()
        setupBackButtonHandler()

        cekAtauMintaIzinLokasi()
    }

    override fun onResume() {
        super.onResume()
        // Saat kembali dari SettingsActivity, refresh data cuaca dengan unit (metric/imperial) yang baru dipilih
        refreshCurrentWeatherData()
    }

    // =========================================================================
    // 1. INITIALIZATION & SETUP FUNCTIONS
    // =========================================================================

    private fun initViews() {
        // Handle Edge-to-Edge Padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        // Layout Containers
        pullContainer = findViewById(R.id.pullContainer)
        tvPullToRefresh = findViewById(R.id.tvPullToRefresh)
        nestedScrollView = findViewById(R.id.nestedScrollView)

        // Main Weather Views
        ivBackground = findViewById(R.id.ivBackground)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)
        tvLocation = findViewById(R.id.tvLocation)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvCondition = findViewById(R.id.tvCondition)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)

        // Weather Details
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWind = findViewById(R.id.tvWind)
        tvPressure = findViewById(R.id.tvPressure)
        tvVisibility = findViewById(R.id.tvVisibility)
        tvSunrise = findViewById(R.id.tvSunrise)
        tvSunset = findViewById(R.id.tvSunset)

        // Search & Settings Views
        ivSearch = findViewById(R.id.btnSearch)
        ivSettings = findViewById(R.id.btnSettings) // Pastikan ID ini ada di XML activity_main kamu
        searchView = findViewById(R.id.searchView)

        // RecyclerViews
        rvHourlyForecast = findViewById(R.id.rvHourlyForecast)
        rvDailyForecast = findViewById(R.id.rvDailyForecast)
    }

    private fun setupRecyclerViews() {
        rvHourlyForecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvDailyForecast.layoutManager = LinearLayoutManager(this)
    }

    private fun setStatusBarTransparent() {
        window.apply {
            statusBarColor = Color.TRANSPARENT
            WindowCompat.getInsetsController(this, decorView).isAppearanceLightStatusBars = false
        }
    }

    private fun setupSettingsButton() {
        ivSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBackButtonHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (searchView.visibility == View.VISIBLE) {
                    closeSearch()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // Helper untuk mengambil preferensi unit suhu ("metric" atau "imperial") dari SharedPreferences
    private fun getTemperatureUnit(): String {
        val sharedPref = getSharedPreferences("WeatherPref", Context.MODE_PRIVATE)
        return sharedPref.getString("UNIT", "metric") ?: "metric"
    }

    private fun refreshCurrentWeatherData() {
        if (!currentCityName.isNullOrBlank()) {
            cariKota(currentCityName!!)
        } else if (currentLat != null && currentLon != null) {
            ambilDataCuaca(currentLat!!, currentLon!!)
        }
    }

    // =========================================================================
    // 2. LOCATION & PERMISSIONS
    // =========================================================================

    private fun cekAtauMintaIzinLokasi() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
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
                    currentLat = location.latitude
                    currentLon = location.longitude
                    currentCityName = null
                    ambilDataCuaca(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "GPS tidak aktif. Menampilkan lokasi default.", Toast.LENGTH_SHORT).show()
                    currentLat = defaultLat
                    currentLon = defaultLon
                    currentCityName = null
                    ambilDataCuaca(defaultLat, defaultLon)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mendapatkan lokasi GPS.", Toast.LENGTH_SHORT).show()
                currentLat = defaultLat
                currentLon = defaultLon
                currentCityName = null
                ambilDataCuaca(defaultLat, defaultLon)
            }
    }

    // =========================================================================
    // 3. NETWORK / API CALLS
    // =========================================================================

    private fun ambilDataCuaca(latitude: Double, longitude: Double) {
        val unit = getTemperatureUnit()

        RetrofitClient.api.getWeather(
            lat = latitude,
            lon = longitude,
            units = unit,
            apiKey = apiKey
        ).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                val wasPullRefreshing = isRefreshing
                resetPullPosition()

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    updateUI(data)
                    updateBackground(data.weather.firstOrNull()?.icon ?: "01d")
                    ambilForecast(data.coord.lat, data.coord.lon)

                    if (wasPullRefreshing) {
                        Toast.makeText(this@MainActivity, "Data cuaca ${data.name} diperbarui", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                resetPullPosition()
                Toast.makeText(this@MainActivity, "Gagal memperbarui data cuaca", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cariKota(city: String) {
        val unit = getTemperatureUnit()

        RetrofitClient.api.getWeatherByCity(
            city = city,
            units = unit,
            apiKey = apiKey
        ).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    currentCityName = data.name
                    currentLat = data.coord.lat
                    currentLon = data.coord.lon

                    updateUI(data)
                    updateBackground(data.weather.firstOrNull()?.icon ?: "01d")
                    ambilForecast(data.coord.lat, data.coord.lon)
                    closeSearch()

                    Toast.makeText(this@MainActivity, "Cuaca ${data.name} ditemukan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Kota \"$city\" tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun ambilForecast(latitude: Double, longitude: Double) {
        val unit = getTemperatureUnit()

        RetrofitClient.api.getForecast(
            lat = latitude,
            lon = longitude,
            units = unit,
            apiKey = apiKey
        ).enqueue(object : Callback<ForecastResponse> {
            override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val fullList = response.body()!!.list

                    // Forecast 24 Jam (8 Data Pertama)
                    rvHourlyForecast.adapter = HourlyAdapter(fullList.take(8))

                    // Forecast Harian (Pengelompokan per Hari)
                    val groupedByDate = fullList.groupBy { it.dtTxt.substring(0, 10) }
                    val dailyList = mutableListOf<ForecastItem>()

                    for ((_, itemsInDay) in groupedByDate) {
                        val realMinTemp = itemsInDay.minOfOrNull { it.main.tempMin } ?: 0.0
                        val realMaxTemp = itemsInDay.maxOfOrNull { it.main.tempMax } ?: 0.0

                        val sampleItem = itemsInDay.find { it.dtTxt.contains("09:00:00") }
                            ?: itemsInDay.find { it.dtTxt.contains("12:00:00") }
                            ?: itemsInDay.firstOrNull()

                        sampleItem?.let { item ->
                            val updatedMain = item.main.copy(
                                tempMin = realMinTemp,
                                tempMax = realMaxTemp
                            )
                            dailyList.add(item.copy(main = updatedMain))
                        }
                    }

                    rvDailyForecast.adapter = DailyAdapter(dailyList)
                }
            }

            override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {}
        })
    }

    // =========================================================================
    // 4. UI UPDATE & BINDING
    // =========================================================================

    private fun updateUI(data: WeatherResponse) {
        val weather = data.weather.firstOrNull()
        val unitSymbol = if (getTemperatureUnit() == "imperial") "°F" else "°C"
        val windUnitSymbol = if (getTemperatureUnit() == "imperial") "mph" else "m/s"

        tvLocation.text = data.name
        tvTemperature.text = "${data.main.temp.toInt()}$unitSymbol"
        tvCondition.text = translateWeather(weather?.main ?: "")
        tvFeelsLike.text = "Terasa seperti ${data.main.feelsLike.toInt()}$unitSymbol"
        tvHumidity.text = "${data.main.humidity}%"
        tvWind.text = "${data.wind.speed} $windUnitSymbol"
        tvPressure.text = "${data.main.pressure} hPa"
        tvVisibility.text = "${data.visibility / 1000} km"

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvSunrise.text = timeFormat.format(Date(data.sys.sunrise * 1000))
        tvSunset.text = timeFormat.format(Date(data.sys.sunset * 1000))
    }

    private fun translateWeather(weather: String): String {
        return when (weather) {
            "Clear" -> "Cerah"
            "Clouds" -> "Berawan"
            "Rain" -> "Hujan"
            "Thunderstorm" -> "Badai Petir"
            "Drizzle" -> "Gerimis"
            "Mist", "Fog", "Haze" -> "Berkabut"
            else -> weather
        }
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
            "11d", "11n" -> {
                ivBackground.setImageResource(R.drawable.thunderstorm)
                ivWeatherIcon.setImageResource(R.drawable.ic_thunderstorm)
            }
            "50d", "50n" -> {
                ivBackground.setImageResource(R.drawable.fog)
                ivWeatherIcon.setImageResource(R.drawable.ic_fog)
            }
            else -> {
                ivBackground.setImageResource(R.drawable.sun)
                ivWeatherIcon.setImageResource(R.drawable.ic_sun)
            }
        }
    }

    // =========================================================================
    // 5. ANIMATIONS & TOUCH INTERACTIONS
    // =========================================================================

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCustomPullToRefresh() {
        var startY = 0f
        var isPulling = false
        val pullResistance = 0.22f
        val pullThreshold = 180f

        nestedScrollView.setOnTouchListener { _, event ->
            if (isRefreshing) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    isPulling = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - startY

                    if (nestedScrollView.scrollY == 0 && deltaY > 0) {
                        if (!isPulling) {
                            startY = event.rawY
                            isPulling = true
                        }

                        val dampedDelta = ((event.rawY - startY) * pullResistance).coerceAtLeast(0f)
                        pullContainer.translationY = dampedDelta

                        val progress = (dampedDelta / (pullThreshold * 0.8f)).coerceIn(0f, 1f)
                        tvPullToRefresh.alpha = progress

                        tvPullToRefresh.text = if (dampedDelta >= pullThreshold * 0.8f) {
                            "Lepaskan untuk memperbarui"
                        } else {
                            "Tarik untuk memperbarui"
                        }

                        return@setOnTouchListener true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isPulling) {
                        val dampedDelta = (event.rawY - startY) * pullResistance

                        if (dampedDelta >= pullThreshold * 0.8f) {
                            isRefreshing = true
                            tvPullToRefresh.text = "Memperbarui data..."
                            pullContainer.animate().translationY(100f).setDuration(250).start()
                            dapatkanLokasiPerangkat()
                        } else {
                            resetPullPosition()
                        }
                        isPulling = false
                    }
                }
            }
            false
        }
    }

    private fun resetPullPosition() {
        pullContainer.animate().translationY(0f).setDuration(300).start()
        tvPullToRefresh.animate().alpha(0f).setDuration(200).start()
        isRefreshing = false
    }

    private fun setupScrollFadeAnimation() {
        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            tvTemperature.alpha = 1f - (scrollY / 180f).coerceIn(0f, 1f)

            val midAlpha = 1f - (scrollY / 260f).coerceIn(0f, 1f)
            ivWeatherIcon.alpha = midAlpha
            tvFeelsLike.alpha = midAlpha

            tvCondition.alpha = 1f - (scrollY / 320f).coerceIn(0f, 1f)
        })
    }

    // =========================================================================
    // 6. SEARCH BAR UI LOGIC
    // =========================================================================

    private fun setupSearchView() {
        searchView.queryHint = "Cari kota..."
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
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })
    }

    private fun openSearch() {
        if (searchView.visibility == View.VISIBLE) return

        ivSearch.visibility = View.GONE
        ivSettings.visibility = View.GONE
        tvLocation.visibility = View.INVISIBLE

        searchView.visibility = View.VISIBLE
        searchView.alpha = 0f
        searchView.translationY = -20f
        searchView.isIconified = false
        searchView.requestFocus()

        searchView.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
        }

        searchView.animate().alpha(1f).translationY(0f).setDuration(220).start()
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
                ivSettings.visibility = View.VISIBLE
                tvLocation.visibility = View.VISIBLE
            }
            .start()
    }
}