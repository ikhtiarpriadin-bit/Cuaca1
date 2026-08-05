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

class DailyAdapter(private val list: List<ForecastItem>) :
    RecyclerView.Adapter<DailyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDailyDay)
        val tvTempMinMax: TextView = view.findViewById(R.id.tvDailyTemp)
        val imgIcon: ImageView = view.findViewById(R.id.imgDailyIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        // Format nama hari, misal "Rabu"
        val sdf = SimpleDateFormat("EEEE", Locale("id", "ID"))
        val date = Date(item.dt * 1000)
        holder.tvDay.text = sdf.format(date)

        holder.tvTempMinMax.text = "${item.main.tempMin.toInt()}° / ${item.main.tempMax.toInt()}°"

        val iconCode = item.weather.firstOrNull()?.icon ?: ""
        holder.imgIcon.setImageResource(getWeatherIcon(iconCode))
    }

    override fun getItemCount() = list.size

    private fun getWeatherIcon(iconCode: String): Int {
        return when (iconCode) {
            "01d", "01n" -> R.drawable.ic_sun
            "02d", "02n", "03d", "03n", "04d", "04n" -> R.drawable.ic_cloud
            "09d", "09n", "10d", "10n", "11d", "11n" -> R.drawable.ic_rain
            else -> R.drawable.ic_cloud
        }
    }
}