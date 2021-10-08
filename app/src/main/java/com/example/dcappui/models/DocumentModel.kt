package com.example.dcappui.models

import android.net.Uri

data class DocumentModel(
    val id: Long,
    val mimeType: String,
    val name: String,
    val lastModified: Long,
    val size: Long,
    val contentUri: Uri,
    val data: String,
    val bucketId: String,
    val bucketName: String
    )




