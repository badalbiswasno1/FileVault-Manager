package com.badalbiswas.filevault

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TrashActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrashAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.trashRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = TrashAdapter(emptyList()) { item -> showTrashActionsDialog(item) }
        recyclerView.adapter = adapter
        loadTrash()
    }

    private fun loadTrash() {
        adapter.updateList(TrashManager.getTrashItems(this))
    }

    private fun showTrashActionsDialog(item: Triple<String, String, Long>) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_trash_actions, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(Gravity.BOTTOM)

        view.findViewById<TextView>(R.id.trashActionFileName).text = item.second

        view.findViewById<TextView>(R.id.trashActionRestore).setOnClickListener {
            dialog.dismiss()
            val success = TrashManager.restoreFromTrash(this, item.first)
            if (success) {
                Toast.makeText(this, "Restored", Toast.LENGTH_SHORT).show()
                loadTrash()
            } else {
                Toast.makeText(this, "Restore failed. Original folder may not exist.", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<TextView>(R.id.trashActionDelete).setOnClickListener {
            dialog.dismiss()
            AlertDialog.Builder(this)
                .setTitle("Delete Permanently")
                .setMessage("This cannot be undone. Continue?")
                .setPositiveButton("Delete") { _, _ ->
                    val success = TrashManager.permanentDelete(this, item.first)
                    if (success) {
                        Toast.makeText(this, "Deleted permanently", Toast.LENGTH_SHORT).show()
                        loadTrash()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.show()
    }
}
