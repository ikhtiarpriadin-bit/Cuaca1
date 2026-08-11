package com.example.cuaca1.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cuaca1.R
import com.example.cuaca1.model.CityItem

class CityAdapter(
    private val cityList: MutableList<CityItem>,
    private val onCityClick: (String) -> Unit,
    private val onDeleteClick: (CityItem) -> Unit
) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCityName: TextView = itemView.findViewById(R.id.tvCityName)
        val tvCityCondition: TextView = itemView.findViewById(R.id.tvCityCondition)
        val tvCityTemp: TextView = itemView.findViewById(R.id.tvCityTemp)
        val tvCityTempRange: TextView = itemView.findViewById(R.id.tvCityTempRange)
        val btnDeleteCity: ImageView = itemView.findViewById(R.id.btnDeleteCity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_city, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val item = cityList[position]
        holder.tvCityName.text = item.name
        holder.tvCityCondition.text = item.condition
        holder.tvCityTemp.text = item.temp
        holder.tvCityTempRange.text = item.tempRange

        holder.itemView.setOnClickListener { onCityClick(item.name) }
        holder.btnDeleteCity.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = cityList.size
}