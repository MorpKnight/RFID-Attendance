package com.example.rfidexample.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BorrowLog(
    val tagId: String,
    val itemName: String,
    val borrowTimestamp: Long, // Changed to Long
    val isReturned: Boolean = false,
    val returnTimestamp: Long? = null // Changed to Long?
)
