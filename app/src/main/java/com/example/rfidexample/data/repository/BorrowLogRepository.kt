package com.example.rfidexample.data.repository

import com.example.rfidexample.data.model.BorrowLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BorrowLogRepository {
    private val logs = mutableListOf<BorrowLog>()
    // dateFormat might still be useful for recordReturn or if you display formatted dates from repository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun getLogs(): List<BorrowLog> = logs.toList()

    // Renamed and adjusted for creating NEW borrows with current time
    fun createNewBorrow(tagId: String, itemName: String) {
        logs.add(
            BorrowLog(
                tagId = tagId,
                itemName = itemName,
                borrowTimestamp = System.currentTimeMillis(), // Use Long for timestamp
                isReturned = false,
                returnTimestamp = null // returnTimestamp should be Long?
            )
        )
    }

    // New function to add any BorrowLog object (existing or newly created)
    fun addLog(log: BorrowLog) {
        // Ensure we don't add duplicates if logs are re-initialized multiple times
        // This is a simple check; a more robust check might involve unique IDs if logs have them.
        if (!logs.any { it.tagId == log.tagId && it.borrowTimestamp == log.borrowTimestamp && it.itemName == log.itemName }) {
            logs.add(log)
        }
    }

    fun recordReturn(log: BorrowLog) {
        val index = logs.indexOfFirst { it.tagId == log.tagId && it.borrowTimestamp == log.borrowTimestamp && it.itemName == log.itemName }
        if (index != -1) {
            logs[index] = log.copy(
                isReturned = true,
                returnTimestamp = System.currentTimeMillis() // Use Long for timestamp
            )
        }
    }

    fun filterByDate(date: String): List<BorrowLog> {
        // This will need adjustment if borrowTimestamp is Long.
        // For now, assuming it was meant to filter by a formatted date string.
        // If filtering by Long timestamp representing a date range, this logic would change.
        return logs.filter { 
            // Example: Format Long timestamp to "yyyy-MM-dd" string for comparison
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.borrowTimestamp)).startsWith(date)
        }
    }

    fun clear() {
        logs.clear()
    }

    fun deleteLog(log: BorrowLog) {
        logs.removeIf {
            it.tagId == log.tagId &&
            it.itemName == log.itemName &&
            it.borrowTimestamp == log.borrowTimestamp
        }
    }
}
