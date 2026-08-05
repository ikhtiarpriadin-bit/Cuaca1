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

        // Format tanggal hari ini (YYYY-MM-DD)
        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val itemDateStr = item.dtTxt.substring(0, 10)

        // Ubah baris pertama menjadi "Hari Ini", baris berikutnya nama Hari (misal: "Kamis")
        if (itemDateStr == todayDateStr || position == 0) {
            holder.tvDay.text = "Hari Ini"
        } else {
            val sdfDay = SimpleDateFormat("EEEE", Locale("id", "ID"))
            val date = Date(item.dt * 1000)
            holder.tvDay.text = sdfDay.format(date)
        }

        // Tampilkan Min / Max dengan pembulatan toInt()
        val minTempInt = item.main.tempMin.toInt()
        val maxTempInt = item.main.tempMax.toInt()
        holder.tvTempMinMax.text = "$minTempInt° / $maxTempInt°"

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