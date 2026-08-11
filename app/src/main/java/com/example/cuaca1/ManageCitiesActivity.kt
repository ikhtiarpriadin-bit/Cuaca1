package com.example.cuaca1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cuaca1.adapter.CityAdapter

class ManageCitiesActivity : AppCompatActivity() {

    private lateinit var etNewCity: EditText
    private lateinit var btnAddCity: Button
    private lateinit var rvCities: RecyclerView

    private val savedCities = mutableListOf<String>()
    private lateinit var cityAdapter: CityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_cities)

        initViews()
        loadSavedCities()
        setupRecyclerView()

        btnAddCity.setOnClickListener {
            val newCity = etNewCity.text.toString().trim()
            if (newCity.isNotEmpty()) {
                addCity(newCity)
            } else {
                Toast.makeText(this, "Nama kota tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews() {
        etNewCity = findViewById(R.id.etNewCity)
        btnAddCity = findViewById(R.id.btnAddCity)
        rvCities = findViewById(R.id.rvCities)
    }

    private fun loadSavedCities() {
        val sharedPref = getSharedPreferences("WeatherPref", Context.MODE_PRIVATE)
        val citySet = sharedPref.getStringSet("SAVED_CITIES", emptySet()) ?: emptySet()

        savedCities.clear()
        savedCities.addAll(citySet)
    }

    private fun saveCitiesToPref() {
        val sharedPref = getSharedPreferences("WeatherPref", Context.MODE_PRIVATE)
        sharedPref.edit().putStringSet("SAVED_CITIES", savedCities.toSet()).apply()
    }

    private fun setupRecyclerView() {
        cityAdapter = CityAdapter(
            cityList = savedCities,
            onCityClick = { selectedCity ->
                // Kirim balik nama kota yang dipilih ke MainActivity
                val resultIntent = Intent().apply {
                    putExtra("SELECTED_CITY", selectedCity)
                }
                setResult(RESULT_OK, resultIntent)
                finish() // Tutup halaman ini
            },
            onDeleteClick = { cityToDelete ->
                deleteCity(cityToDelete)
            }
        )

        rvCities.layoutManager = LinearLayoutManager(this)
        rvCities.adapter = cityAdapter
    }

    private fun addCity(cityName: String) {
        if (savedCities.any { it.equals(cityName, ignoreCase = true) }) {
            Toast.makeText(this, "Kota sudah ada di daftar", Toast.LENGTH_SHORT).show()
            return
        }

        savedCities.add(cityName)
        saveCitiesToPref()
        cityAdapter.notifyDataSetChanged()
        etNewCity.text.clear()
        Toast.makeText(this, "Kota $cityName ditambahkan", Toast.LENGTH_SHORT).show()
    }

    private fun deleteCity(cityName: String) {
        savedCities.remove(cityName)
        saveCitiesToPref()
        cityAdapter.notifyDataSetChanged()
        Toast.makeText(this, "Kota $cityName dihapus", Toast.LENGTH_SHORT).show()
    }
}