package com.badalbiswas.filevault

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TrashManager {
    private const val PREF_NAME = "trash_pref"
    private const val KEY_ENTRIES = "trash_entries"

    private fun trashDir(): File {
        val dir = File(Environment.getExternalStorageDirectory(), ".filevault_trash")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun moveToTrash(context: Context, file: File): Boolean {
        val originalPath = file.absolutePath
        val trashName = "${System.currentTimeMillis()}_${file.name}"
        val destFile = File(trashDir(), trashName)
        val success = file.renameTo(destFile)
        if (success) {
            addEntry(context, trashName, originalPath, file.name, System.currentTimeMillis())
        }
        return success
    }

    fun restoreFromTrash(context: Context, trashName: String): Boolean {
        val entries = getEntries(context)
        var originalPath: String? = null
        for (i in 0 until entries.length()) {
            val e = entries.getJSONObject(i)
            if (e.optString("trashName") == trashName) {
                originalPath = e.optString("originalPath")
                break
            }
        }
        if (originalPath == null) return false
        val trashFile = File(trashDir(), trashName)
        val restoreFile = File(originalPath)
        restoreFile.parentFile?.mkdirs()
        val success = trashFile.renameTo(restoreFile)
        if (success) removeEntry(context, trashName)
        return success
    }

    fun permanentDelete(context: Context, trashName: String): Boolean {
        val trashFile = File(trashDir(), trashName)
        val success = if (trashFile.isDirectory) trashFile.deleteRecursively() else trashFile.delete()
        if (success) removeEntry(context, trashName)
        return success
    }

    fun getTrashItems(context: Context): List<Triple<String, String, Long>> {
        val entries = getEntries(context)
        val result = mutableListOf<Triple<String, String, Long>>()
        for (i in 0 until entries.length()) {
            val e = entries.getJSONObject(i)
            result.add(Triple(e.optString("trashName"), e.optString("originalName"), e.optLong("deletedAt")))
        }
        return result.sortedByDescending { it.third }
    }

    private fun getEntries(context: Context): JSONArray {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ENTRIES, "[]") ?: "[]"
        return JSONArray(raw)
    }

    private fun addEntry(context: Context, trashName: String, originalPath: String, originalName: String, deletedAt: Long) {
        val entries = getEntries(context)
        val obj = JSONObject()
        obj.put("trashName", trashName)
        obj.put("originalPath", originalPath)
        obj.put("originalName", originalName)
        obj.put("deletedAt", deletedAt)
        entries.put(obj)
        save(context, entries)
    }

    private fun removeEntry(context: Context, trashName: String) {
        val entries = getEntries(context)
        val newArr = JSONArray()
        for (i in 0 until entries.length()) {
            val e = entries.getJSONObject(i)
            if (e.optString("trashName") != trashName) newArr.put(e)
        }
        save(context, newArr)
    }

    private fun save(context: Context, entries: JSONArray) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ENTRIES, entries.toString()).apply()
    }

    fun getTrashFile(trashName: String): File {
        return File(trashDir(), trashName)
    }
}
