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
        holder.icon.setImageResource(getIconForFile(item))
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

    private fun getIconForFile(item: FileItem): Int {
        if (item.isDirectory) return R.drawable.ic_file_folder
        val ext = item.file.extension.lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> R.drawable.ic_file_image
            "mp4", "mkv", "avi", "mov", "3gp", "webm" -> R.drawable.ic_file_video
            "mp3", "wav", "m4a", "ogg", "flac" -> R.drawable.ic_file_audio
            "pdf" -> R.drawable.ic_file_pdf
            "doc", "docx" -> R.drawable.ic_file_doc
            "xls", "xlsx", "csv" -> R.drawable.ic_file_excel
            "ppt", "pptx" -> R.drawable.ic_file_ppt
            "zip", "rar", "7z" -> R.drawable.ic_file_zip
            "apk" -> R.drawable.ic_file_apk
            else -> R.drawable.ic_file_generic
        }
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
