package com.badalbiswas.filevault

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class PreviewActivity : AppCompatActivity() {

    private val textExt = setOf("txt", "log", "md", "json", "xml", "java", "kt", "py", "js", "html", "css", "gradle", "yml", "yaml", "csv")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val path = intent.getStringExtra("filePath")
        if (path == null) {
            finish()
            return
        }
        val file = File(path)
        toolbar.title = file.name

        val progress = findViewById<ProgressBar>(R.id.previewProgress)
        val textScroll = findViewById<ScrollView>(R.id.textScrollView)
        val textContent = findViewById<TextView>(R.id.textContent)
        val imageScroll = findViewById<ScrollView>(R.id.imageScrollView)
        val imageContent = findViewById<ImageView>(R.id.imageContent)
        val pdfRecycler = findViewById<RecyclerView>(R.id.pdfPagesRecyclerView)

        val ext = file.extension.lowercase()

        Thread {
            when {
                ext == "pdf" -> {
                    try {
                        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(pfd)
                        val pages = mutableListOf<Bitmap>()
                        for (i in 0 until renderer.pageCount) {
                            val page = renderer.openPage(i)
                            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pages.add(bitmap)
                            page.close()
                        }
                        renderer.close()
                        pfd.close()
                        runOnUiThread {
                            progress.visibility = android.view.View.GONE
                            pdfRecycler.visibility = android.view.View.VISIBLE
                            pdfRecycler.layoutManager = LinearLayoutManager(this)
                            pdfRecycler.adapter = PdfPageAdapter(pages)
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            progress.visibility = android.view.View.GONE
                            Toast.makeText(this, "Cannot preview this PDF", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
                ext in setOf("jpg", "jpeg", "png", "gif", "webp", "bmp") -> {
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 2
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    runOnUiThread {
                        progress.visibility = android.view.View.GONE
                        if (bitmap != null) {
                            imageScroll.visibility = android.view.View.VISIBLE
                            imageContent.setImageBitmap(bitmap)
                        } else {
                            Toast.makeText(this, "Cannot preview this image", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
                ext in textExt -> {
                    try {
                        val text = file.readText().let {
                            if (it.length > 200000) it.substring(0, 200000) + "\n\n... (truncated)" else it
                        }
                        runOnUiThread {
                            progress.visibility = android.view.View.GONE
                            textScroll.visibility = android.view.View.VISIBLE
                            textContent.text = text
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            progress.visibility = android.view.View.GONE
                            Toast.makeText(this, "Cannot read this file", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
                else -> {
                    runOnUiThread {
                        progress.visibility = android.view.View.GONE
                        Toast.makeText(this, "Preview not supported for this file type", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }.start()
    }
}
