package com.example.cuaca1.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cuaca1.R
import com.example.cuaca1.model.ForecastItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HourlyAdapter(private val list: List<ForecastItem>) :
    RecyclerView.Adapter<HourlyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvHourlyTime)
        val tvTemp: TextView = view.findViewById(R.id.tvHourlyTemp)
        val imgIcon: ImageView = view.findViewById(R.id.imgHourlyIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        // Format waktu misal "06:00"
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = Date(item.dt * 1000)
        holder.tvTime.text = sdf.format(date)

        holder.tvTemp.text = "${item.main.temp.toInt()}°"

        // Mapping Icon
        val iconCode = item.weather.firstOrNull()?.icon ?: ""
        holder.imgIcon.setImageResource(getWeatherIcon(iconCode))
    }

    override fun getItemCount() = list.size

    private fun getWeatherIcon(iconCode: String): Int {
        return when (iconCode) {
            "01d", "01n" -> R.drawable.ic_sun
            "02d", "02n", "03d", "03n", "04d", "04n" -> R.drawable.ic_cloud
            "09d", "09n", "10d", "10n" -> R.drawable.ic_rain
            else -> R.drawable.ic_cloud
        }
    }
}