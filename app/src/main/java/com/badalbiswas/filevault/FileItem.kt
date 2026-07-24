package com.badalbiswas.filevault

import java.io.File

data class FileItem(
    val file: File,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)
