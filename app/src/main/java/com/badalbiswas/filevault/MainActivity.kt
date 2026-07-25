package com.badalbiswas.filevault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var storageText: TextView
    private lateinit var storageBar: ProgressBar
    private lateinit var categoryGrid: RecyclerView
    private lateinit var locationsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storageText = findViewById(R.id.storageText)
        storageBar = findViewById(R.id.storageBar)
        categoryGrid = findViewById(R.id.categoryGrid)
        locationsContainer = findViewById(R.id.locationsContainer)

        setupCategories()
        setupLocations()
        checkPermission()

        findViewById<android.view.View>(R.id.localFilesCard).setOnClickListener {
            startActivity(Intent(this, StorageStatsActivity::class.java))
        }

        findViewById<android.view.View>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val searchBox = findViewById<android.widget.EditText>(R.id.searchBox)
        searchBox.setOnEditorActionListener { v, actionId, event ->
            val query = v.text.toString().trim()
            if (query.isNotEmpty()) {
                val intent = Intent(this, FileListActivity::class.java)
                intent.putExtra("filterType", "SEARCH")
                intent.putExtra("searchQuery", query)
                startActivity(intent)
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateStorageInfo()
    }

    private fun setupCategories() {
        val categories = listOf(
            CategoryItem("Images", android.R.drawable.ic_menu_gallery, R.color.cat_images, "IMAGES"),
            CategoryItem("Videos", android.R.drawable.ic_media_play, R.color.cat_videos, "VIDEOS"),
            CategoryItem("Documents", android.R.drawable.ic_menu_agenda, R.color.cat_documents, "DOCUMENTS"),
            CategoryItem("Audio", android.R.drawable.ic_lock_silent_mode_off, R.color.cat_audio, "AUDIO"),
            CategoryItem("APKs", android.R.drawable.sym_def_app_icon, R.color.cat_apks, "APKS"),
            CategoryItem("Zips", android.R.drawable.ic_menu_save, R.color.cat_zips, "ZIPS"),
            CategoryItem("Downloads", android.R.drawable.stat_sys_download, R.color.cat_downloads, "DOWNLOADS")
        )
        categoryGrid.layoutManager = GridLayoutManager(this, 4)
        categoryGrid.adapter = CategoryAdapter(categories) { cat ->
            val intent = Intent(this, FileListActivity::class.java)
            if (cat.filterType == "DOWNLOADS") {
                intent.putExtra("filterType", "ALL")
                intent.putExtra("startPath", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
            } else {
                intent.putExtra("filterType", cat.filterType)
            }
            startActivity(intent)
        }
    }

    private fun setupLocations() {
        val locations = listOf(
            Triple("My Phone", R.drawable.ic_phone, "ALL"),
            Triple("Recently Deleted", R.drawable.ic_trash, "TRASH"),
            Triple("Favorites", R.drawable.ic_star, "FAVORITES")
        )
        locationsContainer.removeAllViews()
        for ((label, icon, type) in locations) {
            val row = layoutInflater.inflate(R.layout.item_location, locationsContainer, false)
            row.findViewById<TextView>(R.id.locLabel).text = label
            row.findViewById<android.widget.ImageView>(R.id.locIcon).setImageResource(icon)
            row.setOnClickListener {
                if (type == "TRASH") {
                    startActivity(Intent(this, TrashActivity::class.java))
                } else {
                    val intent = Intent(this, FileListActivity::class.java)
                    intent.putExtra("filterType", type)
                    if (type == "ALL") {
                        intent.putExtra("startPath", Environment.getExternalStorageDirectory().absolutePath)
                    }
                    startActivity(intent)
                }
            }
            locationsContainer.addView(row)
        }
    }

    private fun updateStorageInfo() {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = totalBytes - availableBytes

        val totalGB = totalBytes / (1024.0 * 1024 * 1024)
        val usedGB = usedBytes / (1024.0 * 1024 * 1024)
        val percent = ((usedBytes.toDouble() / totalBytes) * 100).toInt()

        storageText.text = String.format("%.0f GB / %.0f GB", usedGB, totalGB)
        storageBar.progress = percent
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateStorageInfo()
        }
    }
}
