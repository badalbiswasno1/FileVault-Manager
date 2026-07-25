package com.badalbiswas.filevault

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileOperations {

    fun renameFile(file: File, newName: String): Boolean {
        val newFile = File(file.parentFile, newName)
        if (newFile.exists()) return false
        return file.renameTo(newFile)
    }

    fun deleteFile(file: File): Boolean {
        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun copyFile(source: File, destDir: File): Boolean {
        return try {
            if (source.isDirectory) {
                val newDir = File(destDir, source.name)
                newDir.mkdirs()
                source.listFiles()?.forEach { copyFile(it, newDir) }
            } else {
                val destFile = File(destDir, source.name)
                FileInputStream(source).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun moveFile(source: File, destDir: File): Boolean {
        val destFile = File(destDir, source.name)
        return source.renameTo(destFile)
    }
}
