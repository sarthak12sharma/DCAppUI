package com.example.dcappui.models

import android.net.Uri

data class FoldersModel(
    val id: Long,
    val name: String,
    val lastModified: String,
    val size: Long,
    val contentUri: Uri
)