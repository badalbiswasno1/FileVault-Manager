package com.badalbiswas.filevault

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pathText: TextView
    private lateinit var adapter: FileAdapter
    private var currentDir: File = Environment.getExternalStorageDirectory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        checkPermissionAndLoad()
    }

    private fun checkPermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadFiles()
            } else {
                val intent = android.content.Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
            } else {
                loadFiles()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            loadFiles()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadFiles()
        }
    }

    override fun onBackPressed() {
        val parent = currentDir.parentFile
        if (currentDir != Environment.getExternalStorageDirectory() && parent != null) {
            currentDir = parent
            loadFiles()
        } else {
            super.onBackPressed()
        }
    }

    private fun loadFiles() {
        pathText.text = currentDir.absolutePath
        val files = currentDir.listFiles()?.map {
            FileItem(it, it.isDirectory, it.length(), it.lastModified())
        }?.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.file.name.lowercase() }) ?: emptyList()
        adapter.updateList(files)
    }
}
