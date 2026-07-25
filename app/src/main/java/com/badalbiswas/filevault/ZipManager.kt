package com.badalbiswas.filevault

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipManager {

    fun extractZip(zipFile: File, destDir: File): Boolean {
        return try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    val newFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun compressToZip(sourceFile: File, zipFile: File): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                addToZip(sourceFile, sourceFile.name, zos)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun compressMultipleToZip(files: List<File>, zipFile: File): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for (f in files) {
                    addToZip(f, f.name, zos)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun addToZip(file: File, entryName: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children == null || children.isEmpty()) {
                zos.putNextEntry(ZipEntry("$entryName/"))
                zos.closeEntry()
            } else {
                for (child in children) {
                    addToZip(child, "$entryName/${child.name}", zos)
                }
            }
        } else {
            FileInputStream(file).use { fis ->
                zos.putNextEntry(ZipEntry(entryName))
                val buffer = ByteArray(8192)
                var len: Int
                while (fis.read(buffer).also { len = it } > 0) {
                    zos.write(buffer, 0, len)
                }
                zos.closeEntry()
            }
        }
    }
}
