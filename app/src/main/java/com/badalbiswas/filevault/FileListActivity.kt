package com.badalbiswas.filevault

import android.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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

    companion object {
        var clipboardFile: File? = null
        var clipboardIsCut: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_list)

        filterType = intent.getStringExtra("filterType") ?: "ALL"
        val startPath = intent.getStringExtra("startPath")
        if (startPath != null) currentDir = File(startPath)

        recyclerView = findViewById(R.id.fileRecyclerView)
        pathText = findViewById(R.id.pathText)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FileAdapter(
            emptyList(),
            onClick = { item ->
                if (item.isDirectory) {
                    currentDir = item.file
                    loadFiles()
                } else {
                    openFile(item.file)
                }
            },
            onLongClick = { item -> showActionsDialog(item) }
        )
        recyclerView.adapter = adapter
        loadFiles()

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_file_list, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_paste)?.isVisible = (clipboardFile != null)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_paste) {
            pasteClipboard()
            return true
        }
        return super.onOptionsItemSelected(item)
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

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, contentResolver.getType(uri) ?: "*/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showActionsDialog(item: FileItem) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_file_actions, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.BOTTOM)

        view.findViewById<TextView>(R.id.actionFileName).text = item.file.name

        view.findViewById<TextView>(R.id.actionRename).setOnClickListener {
            dialog.dismiss()
            showRenameDialog(item.file)
        }

        view.findViewById<TextView>(R.id.actionCopy).setOnClickListener {
            clipboardFile = item.file
            clipboardIsCut = false
            Toast.makeText(this, "Copied. Open destination folder and use Paste.", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        view.findViewById<TextView>(R.id.actionMove).setOnClickListener {
            clipboardFile = item.file
            clipboardIsCut = true
            Toast.makeText(this, "Ready to move. Open destination folder and use Paste.", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        view.findViewById<TextView>(R.id.actionShare).setOnClickListener {
            dialog.dismiss()
            shareFile(item.file)
        }

        view.findViewById<TextView>(R.id.actionDelete).setOnClickListener {
            dialog.dismiss()
            confirmDelete(item.file)
        }

        dialog.show()
    }

    private fun showRenameDialog(file: File) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_rename, null)
        val input = view.findViewById<EditText>(R.id.renameInput)
        input.setText(file.name)

        AlertDialog.Builder(this)
            .setTitle("Rename")
            .setView(view)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val success = FileOperations.renameFile(file, newName)
                    if (success) {
                        Toast.makeText(this, "Renamed", Toast.LENGTH_SHORT).show()
                        loadFiles()
                    } else {
                        Toast.makeText(this, "Rename failed. Name may already exist.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete \"${file.name}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val success = FileOperations.deleteFile(file)
                if (success) {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    loadFiles()
                } else {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = contentResolver.getType(uri) ?: "*/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Share via"))
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot share file", Toast.LENGTH_SHORT).show()
        }
    }

    fun pasteClipboard() {
        val source = clipboardFile ?: return
        val success = if (clipboardIsCut) {
            FileOperations.moveFile(source, currentDir)
        } else {
            FileOperations.copyFile(source, currentDir)
        }
        if (success) {
            Toast.makeText(this, if (clipboardIsCut) "Moved" else "Copied", Toast.LENGTH_SHORT).show()
            if (clipboardIsCut) clipboardFile = null
            loadFiles()
        } else {
            Toast.makeText(this, "Operation failed", Toast.LENGTH_SHORT).show()
        }
    }
}
