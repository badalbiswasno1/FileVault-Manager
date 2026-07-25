package com.badalbiswas.filevault

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pathText: TextView
    private lateinit var adapter: FileAdapter
    private lateinit var batchBar: View
    private lateinit var emptyStateText: TextView

    private var currentDir: File = Environment.getExternalStorageDirectory()
    private var filterType: String = "ALL"
    private var sortMode: String = "NAME"
    private var isGridMode: Boolean = false

    private val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    private val videoExt = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm")
    private val docExt = setOf("pdf", "doc", "docx", "txt", "xls", "xlsx", "ppt", "pptx")
    private val audioExt = setOf("mp3", "wav", "m4a", "ogg", "flac")

    private val previewableExt = setOf(
        "pdf", "jpg", "jpeg", "png", "gif", "webp", "bmp",
        "txt", "log", "md", "json", "xml", "java", "kt", "py", "js", "html", "css", "gradle", "yml", "yaml", "csv"
    )

    companion object {
        var clipboardFile: File? = null
        var clipboardIsCut: Boolean = false
        var batchClipboard: List<File>? = null
        var batchClipboardCut: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_list)

        filterType = intent.getStringExtra("filterType") ?: "ALL"
        val startPath = intent.getStringExtra("startPath")
        if (startPath != null) currentDir = File(startPath)

        recyclerView = findViewById(R.id.fileRecyclerView)
        pathText = findViewById(R.id.pathText)
        emptyStateText = findViewById(R.id.emptyStateText)
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

        batchBar = findViewById(R.id.batchActionsBar)
        setupBatchBar()

        val fab = findViewById<TextView>(R.id.fabNewFolder)
        fab.visibility = if (filterType == "ALL") View.VISIBLE else View.GONE
        fab.setOnClickListener {
            showNewFolderDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadFiles()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_file_list, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_paste)?.isVisible = (clipboardFile != null || batchClipboard != null)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_filter -> {
                showViewSortDialog(findViewById(android.R.id.content))
                return true
            }
            R.id.action_paste -> {
                pasteClipboard()
                return true
            }
            R.id.action_select -> {
                enterSelectionMode()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showViewSortDialog(anchor: View) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_view_sort, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.TOP or Gravity.END)
        val params = dialog.window?.attributes
        params?.y = 150
        params?.x = 16
        dialog.window?.attributes = params

        fun refreshChecks() {
            view.findViewById<TextView>(R.id.checkList).text = if (!isGridMode) "✓" else ""
            view.findViewById<TextView>(R.id.checkGrid).text = if (isGridMode) "✓" else ""
        }
        refreshChecks()

        view.findViewById<View>(R.id.viewList).setOnClickListener {
            isGridMode = false
            recyclerView.layoutManager = LinearLayoutManager(this)
            adapter.setGridMode(false)
            refreshChecks()
        }
        view.findViewById<View>(R.id.viewGrid).setOnClickListener {
            isGridMode = true
            recyclerView.layoutManager = GridLayoutManager(this, 3)
            adapter.setGridMode(true)
            refreshChecks()
        }
        view.findViewById<View>(R.id.sortName).setOnClickListener {
            sortMode = "NAME"
            loadFiles()
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.sortSize).setOnClickListener {
            sortMode = "SIZE"
            loadFiles()
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.sortDate).setOnClickListener {
            sortMode = "DATE"
            loadFiles()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onBackPressed() {
        if (adapter.selectionMode) {
            exitSelectionMode()
            return
        }
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
        val showHidden = AppPreferences.getShowHidden(this)

        if (filterType == "FAVORITES") {
            pathText.text = "Favorites"
            val favPaths = FavoritesManager.getFavorites(this)
            val files = favPaths.mapNotNull { p ->
                val f = File(p)
                if (f.exists()) FileItem(f, f.isDirectory, f.length(), f.lastModified()) else null
            }.sortedBy { it.file.name.lowercase() }
            adapter.updateList(files)
            emptyStateText.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
            return
        }

        if (filterType == "SEARCH") {
            val query = intent.getStringExtra("searchQuery")?.lowercase() ?: ""
            pathText.text = "Search: $query"
            val results = mutableListOf<FileItem>()
            fun scan(dir: File) {
                if (dir.name == "Android") return
                val list = try { dir.listFiles() } catch (e: Exception) { null } ?: return
                for (f in list) {
                    try {
                        if (f.name.lowercase().contains(query)) {
                            results.add(FileItem(f, f.isDirectory, f.length(), f.lastModified()))
                        }
                        if (f.isDirectory) scan(f)
                    } catch (e: Exception) {
                        // skip inaccessible file/folder
                    }
                }
            }
            scan(Environment.getExternalStorageDirectory())
            val files = results.sortedBy { it.file.name.lowercase() }
            adapter.updateList(files)
            emptyStateText.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
            return
        }

        pathText.text = currentDir.absolutePath

        val files = if (filterType == "ALL") {
            currentDir.listFiles()?.filter {
                showHidden || !it.name.startsWith(".")
            }?.map {
                FileItem(it, it.isDirectory, it.length(), it.lastModified())
            }?.sortedWith(
                compareByDescending<FileItem> { it.isDirectory }.thenBy {
                    when (sortMode) {
                        "DATE" -> -it.lastModified
                        "SIZE" -> -it.size
                        else -> 0L
                    }
                }.thenBy { it.file.name.lowercase() }
            ) ?: emptyList()
        } else {
            val results = mutableListOf<FileItem>()
            fun scan(dir: File) {
                if (dir.name == "Android") return
                val list = try { dir.listFiles() } catch (e: Exception) { null } ?: return
                for (f in list) {
                    try {
                        if (f.isDirectory) scan(f)
                        else if (matchesFilter(f)) results.add(FileItem(f, false, f.length(), f.lastModified()))
                    } catch (e: Exception) {
                        // skip inaccessible file/folder
                    }
                }
            }
            scan(Environment.getExternalStorageDirectory())
            results.sortedByDescending { it.lastModified }
        }
        adapter.updateList(files)
        emptyStateText.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openFile(file: File) {
        val ext = file.extension.lowercase()
        if (ext in previewableExt) {
            val intent = Intent(this, PreviewActivity::class.java)
            intent.putExtra("filePath", file.absolutePath)
            startActivity(intent)
            return
        }
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

        view.findViewById<TextView>(R.id.actionInfo).setOnClickListener {
            dialog.dismiss()
            showFileInfoDialog(item.file)
        }

        view.findViewById<TextView>(R.id.actionCopyPath).setOnClickListener {
            dialog.dismiss()
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("File Path", item.file.absolutePath)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Path copied", Toast.LENGTH_SHORT).show()
        }

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

        val extractLabel = view.findViewById<TextView>(R.id.actionExtract)
        if (item.file.extension.lowercase() == "zip") {
            extractLabel.visibility = View.VISIBLE
            extractLabel.setOnClickListener {
                dialog.dismiss()
                extractZipFile(item.file)
            }
        }

        view.findViewById<TextView>(R.id.actionCompress).setOnClickListener {
            dialog.dismiss()
            compressFile(item.file)
        }

        val favLabel = view.findViewById<TextView>(R.id.actionFavorite)
        val isFav = FavoritesManager.isFavorite(this, item.file.absolutePath)
        favLabel.text = if (isFav) "Remove from Favorites" else "Add to Favorites"
        favLabel.setOnClickListener {
            if (FavoritesManager.isFavorite(this, item.file.absolutePath)) {
                FavoritesManager.removeFavorite(this, item.file.absolutePath)
                Toast.makeText(this, "Removed from Favorites", Toast.LENGTH_SHORT).show()
            } else {
                FavoritesManager.addFavorite(this, item.file.absolutePath)
                Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
            if (filterType == "FAVORITES") loadFiles()
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

    private fun showFileInfoDialog(file: File) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_file_info, null)
        view.findViewById<TextView>(R.id.infoName).text = file.name
        view.findViewById<TextView>(R.id.infoType).text = if (file.isDirectory) {
            val count = file.listFiles()?.size ?: 0
            "Type: Folder ($count items)"
        } else {
            "Type: ${file.extension.uppercase().ifEmpty { "File" }}"
        }
        val sizeStr = if (file.isDirectory) {
            var total = 0L
            fun sumSize(f: File) {
                val list = try { f.listFiles() } catch (e: Exception) { null } ?: return
                list.forEach {
                    try {
                        if (it.isDirectory) sumSize(it) else total += it.length()
                    } catch (e: Exception) {
                        // skip
                    }
                }
            }
            sumSize(file)
            formatSizeStr(total)
        } else {
            formatSizeStr(file.length())
        }
        view.findViewById<TextView>(R.id.infoSize).text = "Size: $sizeStr"
        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))
        view.findViewById<TextView>(R.id.infoModified).text = "Modified: $dateStr"
        view.findViewById<TextView>(R.id.infoPath).text = "Path: ${file.absolutePath}"

        AlertDialog.Builder(this)
            .setTitle("File Info")
            .setView(view)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatSizeStr(size: Long): String {
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

    private fun showNewFolderDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_rename, null)
        val input = view.findViewById<EditText>(R.id.renameInput)
        input.hint = "Folder name"

        AlertDialog.Builder(this)
            .setTitle("New Folder")
            .setView(view)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newDir = File(currentDir, name)
                    if (newDir.exists()) {
                        Toast.makeText(this, "Folder already exists", Toast.LENGTH_SHORT).show()
                    } else if (newDir.mkdirs()) {
                        Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show()
                        loadFiles()
                    } else {
                        Toast.makeText(this, "Failed to create folder", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Move \"${file.name}\" to Recently Deleted?")
            .setPositiveButton("Delete") { _, _ ->
                val success = TrashManager.moveToTrash(this, file)
                if (success) {
                    Toast.makeText(this, "Moved to Recently Deleted", Toast.LENGTH_SHORT).show()
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

    private fun extractZipFile(zipFile: File) {
        Toast.makeText(this, "Extracting...", Toast.LENGTH_SHORT).show()
        Thread {
            val destDir = File(currentDir, zipFile.nameWithoutExtension)
            destDir.mkdirs()
            val success = ZipManager.extractZip(zipFile, destDir)
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Extracted to ${destDir.name}", Toast.LENGTH_SHORT).show()
                    loadFiles()
                } else {
                    Toast.makeText(this, "Extraction failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun compressFile(file: File) {
        Toast.makeText(this, "Compressing...", Toast.LENGTH_SHORT).show()
        Thread {
            val zipFile = File(currentDir, "${file.nameWithoutExtension.ifEmpty { file.name }}.zip")
            val success = ZipManager.compressToZip(file, zipFile)
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Compressed to ${zipFile.name}", Toast.LENGTH_SHORT).show()
                    loadFiles()
                } else {
                    Toast.makeText(this, "Compression failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun setupBatchBar() {
        findViewById<TextView>(R.id.batchCopy).setOnClickListener {
            val files = adapter.getSelectedFiles()
            if (files.isNotEmpty()) {
                Toast.makeText(this, "${files.size} items copied. Open destination and Paste.", Toast.LENGTH_LONG).show()
                batchClipboard = files
                batchClipboardCut = false
                exitSelectionMode()
            }
        }
        findViewById<TextView>(R.id.batchMove).setOnClickListener {
            val files = adapter.getSelectedFiles()
            if (files.isNotEmpty()) {
                Toast.makeText(this, "${files.size} items ready to move. Open destination and Paste.", Toast.LENGTH_LONG).show()
                batchClipboard = files
                batchClipboardCut = true
                exitSelectionMode()
            }
        }
        findViewById<TextView>(R.id.batchCompress).setOnClickListener {
            val files = adapter.getSelectedFiles()
            if (files.isNotEmpty()) {
                Toast.makeText(this, "Compressing ${files.size} items...", Toast.LENGTH_SHORT).show()
                Thread {
                    val zipFile = File(currentDir, "Archive_${System.currentTimeMillis()}.zip")
                    val success = ZipManager.compressMultipleToZip(files, zipFile)
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Compressed to ${zipFile.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Compression failed", Toast.LENGTH_SHORT).show()
                        }
                        exitSelectionMode()
                    }
                }.start()
            }
        }
        findViewById<TextView>(R.id.batchShare).setOnClickListener {
            val files = adapter.getSelectedFiles()
            if (files.isNotEmpty()) shareMultiple(files)
            exitSelectionMode()
        }
        findViewById<TextView>(R.id.batchDelete).setOnClickListener {
            val files = adapter.getSelectedFiles()
            if (files.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Delete ${files.size} items")
                    .setMessage("Move selected items to Recently Deleted?")
                    .setPositiveButton("Delete") { _, _ ->
                        files.forEach { TrashManager.moveToTrash(this, it) }
                        Toast.makeText(this, "Moved to Recently Deleted", Toast.LENGTH_SHORT).show()
                        exitSelectionMode()
                        loadFiles()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun enterSelectionMode() {
        adapter.setSelectionMode(true)
        batchBar.visibility = View.VISIBLE
    }

    private fun exitSelectionMode() {
        adapter.setSelectionMode(false)
        batchBar.visibility = View.GONE
        loadFiles()
    }

    private fun shareMultiple(files: List<File>) {
        try {
            val uris = ArrayList<android.net.Uri>()
            for (f in files) {
                uris.add(FileProvider.getUriForFile(this, "$packageName.fileprovider", f))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "*/*"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Share via"))
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot share files", Toast.LENGTH_SHORT).show()
        }
    }

    fun pasteClipboard() {
        if (batchClipboard != null) {
            val files = batchClipboard!!
            var allSuccess = true
            for (f in files) {
                val ok = if (batchClipboardCut) FileOperations.moveFile(f, currentDir) else FileOperations.copyFile(f, currentDir)
                if (!ok) allSuccess = false
            }
            Toast.makeText(this, if (allSuccess) "Done" else "Some items failed", Toast.LENGTH_SHORT).show()
            if (batchClipboardCut) batchClipboard = null
            loadFiles()
            return
        }
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
