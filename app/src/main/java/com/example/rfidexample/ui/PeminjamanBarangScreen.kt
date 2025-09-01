package com.example.rfidexample.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rfidexample.data.model.BorrowLog
import com.example.rfidexample.ui.components.AppTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PeminjamanBarangScreen(
    borrowLogs: List<BorrowLog>,
    onBorrow: (String, String) -> Unit,
    onReturn: (BorrowLog) -> Unit,
    scannedTagId: String?,
    nicknames: Map<String, String>,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onNavigateToHistory: () -> Unit
) {
    var selectedItem by remember { mutableStateOf("") }
    val items = listOf("Proyektor", "Kabel HDMI", "Laptop", "Charger")
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Peminjaman Barang",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "Riwayat Peminjaman"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Section for scanning and user info
            item {
                if (scannedTagId == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "Scan RFID",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Silakan scan kartu RFID Anda",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val displayName = nicknames[scannedTagId] ?: scannedTagId
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Peminjam:", style = MaterialTheme.typography.titleSmall)
                            Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (nicknames.containsKey(scannedTagId)) {
                                Text("RFID: $scannedTagId", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Pilih barang untuk dipinjam:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items.forEach { item ->
                            FilterChip(
                                selected = selectedItem == item,
                                onClick = { selectedItem = item },
                                label = { Text(item) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onBorrow(scannedTagId, selectedItem)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Peminjaman '$selectedItem' berhasil dicatat.")
                            }
                            selectedItem = ""
                        },
                        enabled = selectedItem.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Catat Peminjaman")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Section for active borrows
            val activeBorrows = borrowLogs.filter { !it.isReturned }
            if (activeBorrows.isNotEmpty()) {
                item {
                    Text(
                        "Sedang Dipinjam",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(activeBorrows, key = { it.borrowTimestamp }) { log ->
                    BorrowLogItem(
                        log = log,
                        nickname = nicknames[log.tagId],
                        onReturn = { onReturn(log) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BorrowLogItem(
    log: BorrowLog,
    nickname: String?,
    onReturn: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = log.itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Peminjam: ${nickname ?: log.tagId}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Waktu: ${dateFormat.format(Date(log.borrowTimestamp))}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onReturn,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Konfirmasi Pengembalian")
            }
        }
    }
}