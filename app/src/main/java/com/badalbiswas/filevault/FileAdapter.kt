package com.badalbiswas.filevault

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
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

    var selectionMode = false
        private set
    val selectedItems = mutableSetOf<String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.fileIcon)
        val name: TextView = view.findViewById(R.id.fileName)
        val info: TextView = view.findViewById(R.id.fileInfo)
        val checkbox: CheckBox = view.findViewById(R.id.fileCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.file.name

        val ext = item.file.extension.lowercase()
        val isImage = ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
        if (!item.isDirectory && isImage) {
            val thumb = loadThumbnail(item.file)
            if (thumb != null) {
                holder.icon.setImageBitmap(thumb)
            } else {
                holder.icon.setImageResource(getIconForFile(item))
            }
        } else {
            holder.icon.setImageResource(getIconForFile(item))
        }

        val sizeText = if (item.isDirectory) "Folder" else formatSize(item.size)
        val dateText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(item.lastModified))
        holder.info.text = "$sizeText  •  $dateText"

        holder.checkbox.visibility = if (selectionMode) View.VISIBLE else View.GONE
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = selectedItems.contains(item.file.absolutePath)
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            toggleSelection(item, isChecked)
        }

        holder.itemView.setOnClickListener {
            if (selectionMode) {
                holder.checkbox.isChecked = !holder.checkbox.isChecked
            } else {
                onClick(item)
            }
        }
        holder.itemView.setOnLongClickListener {
            if (!selectionMode) {
                onLongClick?.invoke(item)
            }
            true
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<FileItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelectionMode(enabled: Boolean) {
        selectionMode = enabled
        if (!enabled) selectedItems.clear()
        notifyDataSetChanged()
    }

    private fun toggleSelection(item: FileItem, selected: Boolean) {
        if (selected) selectedItems.add(item.file.absolutePath)
        else selectedItems.remove(item.file.absolutePath)
    }

    fun getSelectedFiles(): List<java.io.File> {
        return items.filter { selectedItems.contains(it.file.absolutePath) }.map { it.file }
    }

    private fun loadThumbnail(file: java.io.File): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            options.inSampleSize = 8
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            null
        }
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
