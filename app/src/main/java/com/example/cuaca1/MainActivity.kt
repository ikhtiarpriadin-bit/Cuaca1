package com.example.cuaca1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

    // Status Banner Components
    private lateinit var layoutStatus: LinearLayout
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvStatusText: TextView

    // Detail Views
    private lateinit var tvHumidity: TextView
    private lateinit var tvWind: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvVisibility: TextView
    private lateinit var tvSunrise: TextView
    private lateinit var tvSunset: TextView

    // Top Navigation & Containers
    private lateinit var ivManageCities: ImageView
    private lateinit var ivSettings: ImageView
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var pullContainer: LinearLayout

    // RecyclerViews
    private lateinit var rvHourlyForecast: RecyclerView
    private lateinit var rvDailyForecast: RecyclerView

    // --- STATE VARIABLES ---
    private var currentCityName: String? = null
    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var isCustomCitySelected = false
    private var isRefreshing = false

    // --- ACTIVITY LAUNCHERS ---

    // Perbaikan Bug 1 & 2: Set nilai default saat izin ditolak agar tidak memicu dialog berulang
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

        resetPullPosition()

        if (isGranted) {
            dapatkanLokasiPerangkat()
        } else {
            // Set koordinat ke default agar state lokasi aktif dan tidak memicu cekAtauMintaIzinLokasi lagi
            currentLat = defaultLat
            currentLon = defaultLon
            currentCityName = null

            showStatusBanner(isLoading = false, isSuccess = false, message = "Izin lokasi ditolak, memakai lokasi default")
            ambilDataCuaca(defaultLat, defaultLon)
        }
    }

    private val manageCitiesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedCity = result.data?.getStringExtra("SELECTED_CITY")
            if (!selectedCity.isNullOrEmpty()) {
                resetPullPosition()

                if (selectedCity == "CURRENT_LOCATION") {
                    isCustomCitySelected = false
                    currentCityName = null
                    showStatusBanner(isLoading = true, isSuccess = false, message = "Mendeteksi lokasi GPS...")
                    cekAtauMintaIzinLokasi()
                } else {
                    isCustomCitySelected = true
                    currentCityName = selectedCity
                    cariKota(selectedCity)
                }
            }
        }
    }

    // =========================================================================
    // LIFECYCLE METHODS
    // =========================================================================

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
        setupHeaderButtons()

        // Meminta izin lokasi saat aplikasi dibuka pertama kali
        cekAtauMintaIzinLokasi()
    }

    override fun onResume() {
        super.onResume()
        if (!isCustomCitySelected) {
            refreshCurrentWeatherData()
        }
    }

    // =========================================================================
    // 1. INITIALIZATION & SETUP FUNCTIONS
    // =========================================================================

    private fun initViews() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        pullContainer = findViewById(R.id.pullContainer)
        nestedScrollView = findViewById(R.id.nestedScrollView)

        ivBackground = findViewById(R.id.ivBackground)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)
        tvLocation = findViewById(R.id.tvLocation)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvCondition = findViewById(R.id.tvCondition)
        tvFeelsLike = findViewById(R.id.tvFeelsLike)

        // Status Banner Views
        layoutStatus = findViewById(R.id.layoutStatus)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        tvStatusText = findViewById(R.id.tvStatusText)

        // Weather Details
        tvHumidity = findViewById(R.id.tvHumidity)
        tvWind = findViewById(R.id.tvWind)
        tvPressure = findViewById(R.id.tvPressure)
        tvVisibility = findViewById(R.id.tvVisibility)
        tvSunrise = findViewById(R.id.tvSunrise)
        tvSunset = findViewById(R.id.tvSunset)

        // Header Buttons
        ivManageCities = findViewById(R.id.btnManageCities)
        ivSettings = findViewById(R.id.btnSettings)

        // Forecast Lists
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

    private fun setupHeaderButtons() {
        ivSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        ivManageCities.setOnClickListener {
            val intent = Intent(this, ManageCitiesActivity::class.java)
            manageCitiesLauncher.launch(intent)
        }
    }

    private fun getTemperatureUnit(): String {
        val sharedPref = getSharedPreferences("WeatherPref", Context.MODE_PRIVATE)
        return sharedPref.getString("UNIT", "metric") ?: "metric"
    }

    // Perbaikan Bug 2: Memastikan fungsi refresh tidak memanggil izin lagi jika koordinat belum ada
    private fun refreshCurrentWeatherData() {
        if (!currentCityName.isNullOrBlank()) {
            cariKota(currentCityName!!)
        } else if (currentLat != null && currentLon != null) {
            ambilDataCuaca(currentLat!!, currentLon!!)
        } else {
            // Jika izin ditolak dan koordinat kosong, langsung gunakan lokasi default tanpa minta izin lagi
            currentLat = defaultLat
            currentLon = defaultLon
            ambilDataCuaca(defaultLat, defaultLon)
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

    // Perbaikan Bug 3: Tangani kondisi jika GPS mati/null dengan fallback lokasi default
    @SuppressLint("MissingPermission")
    private fun dapatkanLokasiPerangkat() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (isCustomCitySelected) return@addOnSuccessListener

                if (location != null) {
                    currentLat = location.latitude
                    currentLon = location.longitude
                    currentCityName = null
                    ambilDataCuaca(location.latitude, location.longitude)
                } else {
                    currentLat = defaultLat
                    currentLon = defaultLon
                    currentCityName = null
                    showStatusBanner(isLoading = false, isSuccess = false, message = "GPS tidak aktif, memakai lokasi default")
                    ambilDataCuaca(defaultLat, defaultLon)
                }
            }
            .addOnFailureListener {
                if (isCustomCitySelected) return@addOnFailureListener
                currentLat = defaultLat
                currentLon = defaultLon
                currentCityName = null
                showStatusBanner(isLoading = false, isSuccess = false, message = "Gagal mengambil GPS, memakai lokasi default")
                ambilDataCuaca(defaultLat, defaultLon)
            }
    }

    // =========================================================================
    // 3. NETWORK / API CALLS
    // =========================================================================

    private fun ambilDataCuaca(latitude: Double, longitude: Double) {
        val unit = getTemperatureUnit()
        showStatusBanner(isLoading = true, isSuccess = false, message = "Memperbarui...")

        RetrofitClient.api.getWeather(
            lat = latitude,
            lon = longitude,
            units = unit,
            apiKey = apiKey
        ).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                resetPullPosition()

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    updateUI(data)
                    updateBackground(data.weather.firstOrNull()?.icon ?: "01d")
                    forceRedrawUI()

                    ambilForecast(data.coord.lat, data.coord.lon)

                    showStatusBanner(isLoading = false, isSuccess = true, message = "Berhasil diperbarui")
                } else {
                    showStatusBanner(isLoading = false, isSuccess = false, message = "Gagal memuat data")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                resetPullPosition()
                showStatusBanner(isLoading = false, isSuccess = false, message = "Koneksi terputus")
            }
        })
    }

    private fun cariKota(city: String) {
        val unit = getTemperatureUnit()
        showStatusBanner(isLoading = true, isSuccess = false, message = "Memperbarui...")

        RetrofitClient.api.getWeatherByCity(
            city = city,
            units = unit,
            apiKey = apiKey
        ).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                resetPullPosition()

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    currentCityName = data.name
                    currentLat = data.coord.lat
                    currentLon = data.coord.lon

                    updateUI(data)
                    updateBackground(data.weather.firstOrNull()?.icon ?: "01d")
                    forceRedrawUI()

                    ambilForecast(data.coord.lat, data.coord.lon)

                    showStatusBanner(isLoading = false, isSuccess = true, message = "Berhasil diperbarui")
                } else {
                    showStatusBanner(isLoading = false, isSuccess = false, message = "Kota \"$city\" tidak ditemukan")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                resetPullPosition()
                showStatusBanner(isLoading = false, isSuccess = false, message = "Gagal terhubung ke server")
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

                    rvHourlyForecast.adapter = HourlyAdapter(fullList.take(8))

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
    // 4. UI UPDATE & HELPER BANNER
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

    private fun showStatusBanner(isLoading: Boolean, isSuccess: Boolean = false, message: String) {
        if (!::layoutStatus.isInitialized) return

        layoutStatus.visibility = View.VISIBLE
        layoutStatus.alpha = 1f
        tvStatusText.text = message

        when {
            isLoading -> {
                ivStatusIcon.setImageResource(android.R.drawable.ic_popup_sync)
            }
            isSuccess -> {
                ivStatusIcon.setImageResource(R.drawable.ic_check)

                Handler(Looper.getMainLooper()).postDelayed({
                    ivStatusIcon.setImageResource(android.R.drawable.ic_popup_sync)
                    tvStatusText.text = "Baru saja diperbarui"
                }, 2000)
            }
            else -> {
                ivStatusIcon.setImageResource(android.R.drawable.stat_notify_error)
            }
        }
    }

    private fun forceRedrawUI() {
        nestedScrollView.scrollTo(0, 0)

        tvTemperature.alpha = 1f
        ivWeatherIcon.alpha = 1f
        tvFeelsLike.alpha = 1f
        tvCondition.alpha = 1f

        nestedScrollView.invalidate()
        ivBackground.invalidate()
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
                            layoutStatus.visibility = View.VISIBLE
                            ivStatusIcon.setImageResource(android.R.drawable.ic_popup_sync)
                            tvStatusText.text = "Baru saja diperbarui"
                        }

                        val dampedDelta = ((event.rawY - startY) * pullResistance).coerceAtLeast(0f)
                        pullContainer.translationY = dampedDelta

                        val progress = (dampedDelta / (pullThreshold * 0.8f)).coerceIn(0f, 1f)
                        layoutStatus.alpha = progress

                        return@setOnTouchListener true
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isPulling) {
                        val dampedDelta = (event.rawY - startY) * pullResistance

                        if (dampedDelta >= pullThreshold * 0.8f) {
                            isRefreshing = true
                            showStatusBanner(isLoading = true, isSuccess = false, message = "Memperbarui...")
                            pullContainer.animate().translationY(0f).setDuration(250).start()
                            refreshCurrentWeatherData()
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
        if (!isRefreshing) {
            layoutStatus.animate().alpha(0f).setDuration(200).withEndAction {
                layoutStatus.visibility = View.GONE
                layoutStatus.alpha = 1f
            }.start()
        }
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
}