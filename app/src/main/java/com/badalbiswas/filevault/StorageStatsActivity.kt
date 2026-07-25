package com.badalbiswas.filevault

import android.os.Bundle
import android.os.Environment
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class StorageStatsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StatAdapter
    private lateinit var totalUsedText: TextView
    private lateinit var scanStatusText: TextView
    private lateinit var scanProgressBar: ProgressBar

    private val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    private val videoExt = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm")
    private val docExt = setOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx")
    private val audioExt = setOf("mp3", "wav", "m4a", "ogg", "flac")
    private val zipExt = setOf("zip", "rar", "7z")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_stats)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.statsRecyclerView)
        totalUsedText = findViewById(R.id.totalUsedText)
        scanStatusText = findViewById(R.id.scanStatusText)
        scanProgressBar = findViewById(R.id.scanProgressBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StatAdapter(emptyList())
        recyclerView.adapter = adapter

        startScan()
    }

    private fun startScan() {
        Thread {
            val sizes = LongArray(7)
            var totalScanned = 0L

            fun scan(dir: File) {
                val files = dir.listFiles() ?: return
                for (f in files) {
                    if (f.isDirectory) {
                        if (f.name.startsWith(".")) continue
                        scan(f)
                    } else {
                        val ext = f.extension.lowercase()
                        val size = f.length()
                        totalScanned += size
                        when {
                            ext in imageExt -> sizes[0] += size
                            ext in videoExt -> sizes[1] += size
                            ext in docExt -> sizes[2] += size
                            ext in audioExt -> sizes[3] += size
                            ext == "apk" -> sizes[4] += size
                            ext in zipExt -> sizes[5] += size
                            else -> sizes[6] += size
                        }
                    }
                }
            }
            scan(Environment.getExternalStorageDirectory())

            val labels = arrayOf("Images", "Videos", "Documents", "Audio", "APKs", "Zips", "Others")
            val colors = arrayOf(
                R.color.cat_images, R.color.cat_videos, R.color.cat_documents,
                R.color.cat_audio, R.color.cat_apks, R.color.cat_zips, R.color.accent_blue
            )

            val statItems = mutableListOf<StatItem>()
            for (i in labels.indices) {
                val percent = if (totalScanned > 0) ((sizes[i].toDouble() / totalScanned) * 100).toInt() else 0
                statItems.add(StatItem(labels[i], sizes[i], colors[i], percent))
            }
            val sortedItems = statItems.sortedByDescending { it.sizeBytes }

            runOnUiThread {
                adapter.updateList(sortedItems)
                totalUsedText.text = formatSize(totalScanned)
                scanStatusText.text = "Scan complete"
                scanProgressBar.visibility = android.view.View.GONE
            }
        }.start()
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
