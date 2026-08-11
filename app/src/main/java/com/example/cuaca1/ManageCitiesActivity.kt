package com.example.cuaca1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cuaca1.adapter.CityAdapter
import com.example.cuaca1.api.RetrofitClient
import com.example.cuaca1.model.CityItem
import com.example.cuaca1.model.WeatherResponse
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ManageCitiesActivity : AppCompatActivity() {

    private lateinit var tvManageTitle: TextView
    private lateinit var searchViewCity: SearchView
    private lateinit var btnCancelSearch: TextView
    private lateinit var layoutPopularCities: View
    private lateinit var layoutSavedCities: View
    private lateinit var chipGroupPopular: ChipGroup
    private lateinit var rvCities: RecyclerView

    private val savedCityList = mutableListOf<CityItem>()
    private lateinit var cityAdapter: CityAdapter
    private val apiKey = "277e22a7fbc5477aedc466d91c974316"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_cities)

        initViews()
        setupRecyclerView()
        setupSearchLogic()
        setupPopularChips()

        // Memuat daftar kota yang tersimpan
        loadSavedCities()
    }

    private fun initViews() {
        tvManageTitle = findViewById(R.id.tvManageTitle)
        searchViewCity = findViewById(R.id.searchViewCity)
        btnCancelSearch = findViewById(R.id.btnCancelSearch)
        layoutPopularCities = findViewById(R.id.layoutPopularCities)
        layoutSavedCities = findViewById(R.id.layoutSavedCities)
        chipGroupPopular = findViewById(R.id.chipGroupPopular)
        rvCities = findViewById(R.id.rvCities)
    }

    private fun setupRecyclerView() {
        cityAdapter = CityAdapter(
            cityList = savedCityList,
            onCityClick = { cityName -> selectCityAndReturn(cityName) },
            onDeleteClick = { item -> deleteCity(item) }
        )
        // Optimasi performa RecyclerView
        rvCities.apply {
            layoutManager = LinearLayoutManager(this@ManageCitiesActivity)
            adapter = cityAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchLogic() {
        searchViewCity.setOnQueryTextFocusChangeListener { _, hasFocus ->
            showSearchMode(hasFocus)
        }

        searchViewCity.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    addAndSelectCity(query.trim())
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })

        btnCancelSearch.setOnClickListener {
            searchViewCity.setQuery("", false)
            searchViewCity.clearFocus()
            showSearchMode(false)
        }
    }

    private fun setupPopularChips() {
        try {
            for (i in 0 until chipGroupPopular.childCount) {
                val chip = chipGroupPopular.getChildAt(i) as? Chip
                chip?.setOnClickListener {
                    addAndSelectCity(chip.text.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSearchMode(isSearch: Boolean) {
        if (isSearch) {
            tvManageTitle.visibility = View.GONE
            btnCancelSearch.visibility = View.VISIBLE
            layoutPopularCities.visibility = View.VISIBLE
            layoutSavedCities.visibility = View.GONE
        } else {
            tvManageTitle.visibility = View.VISIBLE
            btnCancelSearch.visibility = View.GONE
            layoutPopularCities.visibility = View.GONE
            layoutSavedCities.visibility = View.VISIBLE
        }
    }

    private fun loadSavedCities() {
        val sharedPref = getSharedPreferences("WeatherPref", Context.MODE_PRIVATE)
        val citySet = sharedPref.getStringSet("SAVED_CITIES", setOf("Banjarmasin", "Jakarta")) ?: emptySet()

        savedCityList.clear()

        // Tampilkan dulu skeleton/nama kota dengan cepat tanpa menunggu API
        citySet.forEach { cityName ->
            savedCityList.add(CityItem(name = cityName))
        }
        cityAdapter.notifyDataSetChanged()

        // Ambil detail cuaca di background dan perbarui per baris (smooth update)
        savedCityList.forEachIndexed { index, item ->
            fetchCityWeatherDetails(item, index)
        }
    }

    private fun fetchCityWeatherDetails(item: CityItem, position: Int) {
        RetrofitClient.api.getWeatherByCity(
            city = item.name,
            units = "metric",
            apiKey = apiKey
        ).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    item.temp = "${data.main.temp.toInt()}°"
                    item.condition = translateWeather(data.weather.firstOrNull()?.main ?: "")
                    item.tempRange = "${data.main.tempMax.toInt()}° / ${data.main.tempMin.toInt()}°"

                    // Hanya perbarui baris/item yang bersangkutan saja (bebas lag/patah-patah)
                    cityAdapter.notifyItemChanged(position)
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {}
        })
    }

    private fun addAndSelectCity(cityName: String) {
        val sharedPref = getSharedPreferences("WeatherPref", Context.MODE_PRIVATE)
        val currentSet = sharedPref.getStringSet("SAVED_CITIES", emptySet())?.toMutableSet() ?: mutableSetOf()

        currentSet.add(cityName)
        sharedPref.edit().putStringSet("SAVED_CITIES", currentSet).apply()

        selectCityAndReturn(cityName)
    }

    private fun deleteCity(item: CityItem) {
        val position = savedCityList.indexOf(item)
        if (position != -1) {
            savedCityList.removeAt(position)
            cityAdapter.notifyItemRemoved(position)

            val sharedPref = getSharedPreferences("WeatherPref", Context.MODE_PRIVATE)
            val currentSet = savedCityList.map { it.name }.toSet()
            sharedPref.edit().putStringSet("SAVED_CITIES", currentSet).apply()

            Toast.makeText(this, "Kota ${item.name} dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectCityAndReturn(cityName: String) {
        val resultIntent = Intent().apply {
            putExtra("SELECTED_CITY", cityName)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
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
}