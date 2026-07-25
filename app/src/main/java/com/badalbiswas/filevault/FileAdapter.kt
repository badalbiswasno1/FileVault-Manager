package com.badalbiswas.filevault

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
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
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var selectionMode = false
        private set
    var isGridMode = false
        private set
    val selectedItems = mutableSetOf<String>()

    private val imageExts = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    private val videoExts = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm")

    companion object {
        private const val TYPE_LIST = 0
        private const val TYPE_GRID = 1
    }

    class ListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.fileIcon)
        val playOverlay: ImageView = view.findViewById(R.id.playOverlay)
        val name: TextView = view.findViewById(R.id.fileName)
        val info: TextView = view.findViewById(R.id.fileInfo)
        val checkbox: CheckBox = view.findViewById(R.id.fileCheckbox)
    }

    class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.gridIcon)
        val playOverlay: ImageView = view.findViewById(R.id.gridPlayOverlay)
        val name: TextView = view.findViewById(R.id.gridName)
        val checkbox: CheckBox = view.findViewById(R.id.gridCheckbox)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGridMode) TYPE_GRID else TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_GRID) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file_grid, parent, false)
            GridViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
            ListViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val ext = item.file.extension.lowercase()
        val isImage = !item.isDirectory && ext in imageExts
        val isVideo = !item.isDirectory && ext in videoExts
        val iconRes = getIconForFile(item)

        val thumb = when {
            isImage -> loadImageThumbnail(item.file)
            isVideo -> loadVideoThumbnail(item.file)
            else -> null
        }

        when (holder) {
            is ListViewHolder -> {
                holder.name.text = item.file.name
                if (thumb != null) holder.icon.setImageBitmap(thumb) else holder.icon.setImageResource(iconRes)
                holder.playOverlay.visibility = if (isVideo) View.VISIBLE else View.GONE

                val sizeText = if (item.isDirectory) "Folder" else formatSize(item.size)
                val dateText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(item.lastModified))
                holder.info.text = "$sizeText  •  $dateText"

                holder.checkbox.visibility = if (selectionMode) View.VISIBLE else View.GONE
                holder.checkbox.setOnCheckedChangeListener(null)
                holder.checkbox.isChecked = selectedItems.contains(item.file.absolutePath)
                holder.checkbox.setOnCheckedChangeListener { _, isChecked -> toggleSelection(item, isChecked) }

                holder.itemView.setOnClickListener {
                    if (selectionMode) holder.checkbox.isChecked = !holder.checkbox.isChecked
                    else onClick(item)
                }
                holder.itemView.setOnLongClickListener {
                    if (!selectionMode) onLongClick?.invoke(item)
                    true
                }
            }
            is GridViewHolder -> {
                holder.name.text = item.file.name
                if (thumb != null) holder.icon.setImageBitmap(thumb) else holder.icon.setImageResource(iconRes)
                holder.playOverlay.visibility = if (isVideo) View.VISIBLE else View.GONE

                holder.checkbox.visibility = if (selectionMode) View.VISIBLE else View.GONE
                holder.checkbox.setOnCheckedChangeListener(null)
                holder.checkbox.isChecked = selectedItems.contains(item.file.absolutePath)
                holder.checkbox.setOnCheckedChangeListener { _, isChecked -> toggleSelection(item, isChecked) }

                holder.itemView.setOnClickListener {
                    if (selectionMode) holder.checkbox.isChecked = !holder.checkbox.isChecked
                    else onClick(item)
                }
                holder.itemView.setOnLongClickListener {
                    if (!selectionMode) onLongClick?.invoke(item)
                    true
                }
            }
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

    fun setGridMode(enabled: Boolean) {
        isGridMode = enabled
        notifyDataSetChanged()
    }

    private fun toggleSelection(item: FileItem, selected: Boolean) {
        if (selected) selectedItems.add(item.file.absolutePath)
        else selectedItems.remove(item.file.absolutePath)
    }

    fun getSelectedFiles(): List<java.io.File> {
        return items.filter { selectedItems.contains(it.file.absolutePath) }.map { it.file }
    }

    private fun loadImageThumbnail(file: java.io.File): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            options.inSampleSize = 8
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadVideoThumbnail(file: java.io.File): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val frame = retriever.getFrameAtTime(0)
            retriever.release()
            frame
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
