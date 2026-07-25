package com.badalbiswas.filevault

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileAdapter(
    private var items: List<FileItem>,
    private val onClick: (FileItem) -> Unit,
    private val onLongClick: ((FileItem) -> Unit)? = null
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.fileIcon)
        val name: TextView = view.findViewById(R.id.fileName)
        val info: TextView = view.findViewById(R.id.fileInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.file.name
        holder.icon.setImageResource(
            if (item.isDirectory) android.R.drawable.ic_menu_agenda
            else android.R.drawable.ic_menu_save
        )
        val sizeText = if (item.isDirectory) "Folder" else formatSize(item.size)
        val dateText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(item.lastModified))
        holder.info.text = "$sizeText  •  $dateText"
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(item)
            true
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<FileItem>) {
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
        return String.format("%.1f %s", s, units[unitIndex])
    }
}
