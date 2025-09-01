package com.example.rfidexample.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BorrowLog(
    val tagId: String,
    val itemName: String,
    val borrowTimestamp: String, // yyyy-MM-dd HH:mm:ss
    val isReturned: Boolean = false,
    val returnTimestamp: String? = null
)

