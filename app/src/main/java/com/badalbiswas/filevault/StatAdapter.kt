package com.badalbiswas.filevault

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class StatAdapter(private var items: List<StatItem>) : RecyclerView.Adapter<StatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dot: View = view.findViewById(R.id.statColorDot)
        val label: TextView = view.findViewById(R.id.statLabel)
        val size: TextView = view.findViewById(R.id.statSize)
        val bar: ProgressBar = view.findViewById(R.id.statBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.size.text = formatSize(item.sizeBytes)
        holder.bar.progress = item.percent
        val color = ContextCompat.getColor(holder.itemView.context, item.colorRes)
        holder.dot.backgroundTintList = ColorStateList.valueOf(color)
        holder.bar.progressTintList = ColorStateList.valueOf(color)
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<StatItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var s = size.toDouble()
        var unitIndex = 0
        while (s >= 1024 && unitIndex < units.size - 1) {
            s /= 1024
            unitIndex++
        }
        return String.format("%.2f %s", s, units[unitIndex])
    }
}
