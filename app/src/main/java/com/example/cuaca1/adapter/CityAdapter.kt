package com.example.cuaca1.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cuaca1.R

class CityAdapter(
    private val cityList: MutableList<String>,
    private val onCityClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCityName: TextView = itemView.findViewById(R.id.tvCityName)
        val btnDeleteCity: ImageView = itemView.findViewById(R.id.btnDeleteCity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_city, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val cityName = cityList[position]
        holder.tvCityName.text = cityName

        // Saat item kota diklik (pilih kota)
        holder.itemView.setOnClickListener {
            onCityClick(cityName)
        }

        // Saat tombol hapus diklik
        holder.btnDeleteCity.setOnClickListener {
            onDeleteClick(cityName)
        }
    }

    override fun getItemCount(): Int = cityList.size
}