package com.badalbiswas.filevault

import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pathText: TextView
    private lateinit var adapter: FileAdapter
    private var currentDir: File = Environment.getExternalStorageDirectory()
    private var filterType: String = "ALL"

    private val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    private val videoExt = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm")
    private val docExt = setOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx")
    private val audioExt = setOf("mp3", "wav", "m4a", "ogg", "flac")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_list)

        filterType = intent.getStringExtra("filterType") ?: "ALL"
        val startPath = intent.getStringExtra("startPath")
        if (startPath != null) currentDir = File(startPath)

        recyclerView = findViewById(R.id.fileRecyclerView)
        pathText = findViewById(R.id.pathText)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FileAdapter(emptyList()) { item ->
            if (item.isDirectory) {
                currentDir = item.file
                loadFiles()
            } else {
                Toast.makeText(this, item.file.name, Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter
        loadFiles()
    }

    override fun onBackPressed() {
        val parent = currentDir.parentFile
        if (currentDir != Environment.getExternalStorageDirectory() && parent != null && filterType == "ALL") {
            currentDir = parent
            loadFiles()
        } else {
            super.onBackPressed()
        }
    }

    private fun matchesFilter(file: File): Boolean {
        if (filterType == "ALL") return true
        val ext = file.extension.lowercase()
        return when (filterType) {
            "IMAGES" -> ext in imageExt
            "VIDEOS" -> ext in videoExt
            "DOCUMENTS" -> ext in docExt
            "AUDIO" -> ext in audioExt
            "APKS" -> ext == "apk"
            "ZIPS" -> ext in setOf("zip", "rar", "7z")
            else -> true
        }
    }

    private fun loadFiles() {
        pathText.text = currentDir.absolutePath

        val files = if (filterType == "ALL") {
            currentDir.listFiles()?.map {
                FileItem(it, it.isDirectory, it.length(), it.lastModified())
            }?.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.file.name.lowercase() }) ?: emptyList()
        } else {
            val results = mutableListOf<FileItem>()
            fun scan(dir: File) {
                val list = dir.listFiles() ?: return
                for (f in list) {
                    if (f.isDirectory) scan(f)
                    else if (matchesFilter(f)) results.add(FileItem(f, false, f.length(), f.lastModified()))
                }
            }
            scan(Environment.getExternalStorageDirectory())
            results.sortedByDescending { it.lastModified }
        }
        adapter.updateList(files)
    }
}
