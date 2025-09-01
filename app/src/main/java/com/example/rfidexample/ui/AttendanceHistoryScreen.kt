package com.example.rfidexample.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rfidexample.ui.components.AppTopBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    tagHistory: List<String>,
    nicknames: Map<String, String>,
    onBack: () -> Unit,
    onClearHistory: () -> Unit
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val sdfFull = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = sdf.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val filteredAndSortedHistory = remember(tagHistory, searchQuery, selectedDate, sortOption) {
        val mappedHistory = tagHistory.mapNotNull {
            val parts = it.split(",")
            if (parts.size == 2) {
                try {
                    Triple(parts[0], sdfFull.parse(parts[1]), parts[1])
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        val filtered = mappedHistory
            .filter { (uid, date, _) ->
                val nickname = nicknames[uid] ?: ""
                val matchesSearch = nickname.contains(searchQuery, ignoreCase = true) || uid.contains(searchQuery, ignoreCase = true)
                val matchesDate = selectedDate == null || sdf.format(date) == selectedDate
                matchesSearch && matchesDate
            }

        val sorted = when (sortOption) {
            SortOption.DATE_ASC -> filtered.sortedBy { it.second }
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.second }
            SortOption.NAME_ASC -> filtered.sortedBy { nicknames[it.first]?.lowercase() ?: it.first }
            SortOption.NAME_DESC -> filtered.sortedByDescending { nicknames[it.first]?.lowercase() ?: it.first }
        }
        sorted.map { "${it.first},${it.third}" }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Riwayat Absensi",
                onBack = onBack,
                actions = {
                    if (tagHistory.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Riwayat")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // FilterAndSortControls composable remains unchanged
            FilterAndSortControls(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedDate = selectedDate,
                onDateClick = { datePickerDialog.show() },
                onClearDate = { selectedDate = null },
                sortOption = sortOption,
                onSortOptionChange = { sortOption = it }
            )

            if (filteredAndSortedHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada riwayat yang cocok.", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredAndSortedHistory) { historyEntry ->
                        val parts = historyEntry.split(",")
                        if (parts.size == 2) {
                            val uid = parts[0]
                            val timestamp = parts[1]
                            val nickname = nicknames[uid] ?: "(Tanpa Nama)"

                            ListItem(
                                headlineContent = { Text(nickname) },
                                supportingContent = { Text("ID: $uid") },
                                trailingContent = { Text(timestamp, style = MaterialTheme.typography.bodySmall) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Konfirmasi") },
            text = { Text("Apakah Anda yakin ingin menghapus semua riwayat absensi?") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Semua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}