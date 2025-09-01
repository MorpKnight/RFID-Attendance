package com.example.rfidexample.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rfidexample.data.model.BorrowLog
import com.example.rfidexample.ui.components.AppTopBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    borrowLogs: List<BorrowLog>,
    nicknames: Map<String, String>,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteLog: (BorrowLog) -> Unit
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            calendar.set(year, month, dayOfMonth)
            selectedDate = sdf.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val filteredAndSortedLogs = remember(borrowLogs, searchQuery, selectedDate, sortOption) {
        val filtered = borrowLogs
            .filter { log ->
                val nickname = nicknames[log.tagId] ?: ""
                val matchesSearch = nickname.contains(searchQuery, ignoreCase = true) || log.tagId.contains(searchQuery, ignoreCase = true)
                val matchesDate = selectedDate == null || SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(log.borrowTimestamp)) == selectedDate
                matchesSearch && matchesDate
            }

        when (sortOption) {
            SortOption.DATE_ASC -> filtered.sortedBy { it.borrowTimestamp }
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.borrowTimestamp }
            SortOption.NAME_ASC -> filtered.sortedBy { nicknames[it.tagId]?.lowercase() ?: it.tagId }
            SortOption.NAME_DESC -> filtered.sortedByDescending { nicknames[it.tagId]?.lowercase() ?: it.tagId }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Riwayat Peminjaman",
                onBack = onBack,
                actions = {
                    if (borrowLogs.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            FilterAndSortControls(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedDate = selectedDate,
                onDateClick = { datePickerDialog.show() },
                onClearDate = { selectedDate = null },
                sortOption = sortOption,
                onSortOptionChange = { sortOption = it }
            )

            if (filteredAndSortedLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada riwayat yang cocok.", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAndSortedLogs, key = { it.borrowTimestamp }) { log ->
                        HistoryLogItem(
                            log = log,
                            nickname = nicknames[log.tagId],
                            onDelete = { onDeleteLog(log) }
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Konfirmasi") },
            text = { Text("Anda yakin ingin menghapus semua riwayat peminjaman?") },
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

@Composable
fun FilterAndSortControls(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedDate: String?,
    onDateClick: () -> Unit,
    onClearDate: () -> Unit,
    sortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Cari berdasarkan nama...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onDateClick) {
                Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedDate ?: "Pilih Tanggal")
            }
            if (selectedDate != null) {
                IconButton(onClick = onClearDate) {
                    Icon(Icons.Default.Clear, contentDescription = "Hapus Tanggal")
                }
            }
            Box {
                OutlinedButton(onClick = { sortMenuExpanded = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Urutkan", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(sortOption.displayName)
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                onSortOptionChange(option)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class SortOption(val displayName: String) {
    DATE_DESC("Tanggal (Terbaru)"),
    DATE_ASC("Tanggal (Terlama)"),
    NAME_ASC("Nama (A-Z)"),
    NAME_DESC("Nama (Z-A)")
}

// HistoryLogItem composable (from previous response, no changes needed)
@Composable
private fun HistoryLogItem(
    log: BorrowLog,
    nickname: String?,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(log.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Peminjam: ${nickname ?: log.tagId}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Dipinjam: ${dateFormat.format(Date(log.borrowTimestamp))}", style = MaterialTheme.typography.bodySmall)
            if (log.isReturned && log.returnTimestamp != null) {
                Text("Dikembalikan: ${dateFormat.format(Date(log.returnTimestamp))}", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Status: Masih Dipinjam", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(
                onClick = { showDeleteConfirmDialog = true },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Log", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Log") },
            text = { Text("Anda yakin ingin menghapus log ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}