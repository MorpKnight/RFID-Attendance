package com.example.rfidexample.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rfidexample.data.model.BorrowLog
import com.example.rfidexample.ui.components.AppTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeminjamanBarangScreen(
    borrowLogs: List<BorrowLog>,
    onBorrow: (String, String) -> Unit,
    onReturn: (BorrowLog) -> Unit,
    scannedTagId: String?,
    nicknames: Map<String, String>, // Tambahan parameter
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null // Tambahkan opsional tombol kembali
) {
    var selectedItem by remember { mutableStateOf("") }
    val items = listOf("A", "B", "C", "D")
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Borrow Items",
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .padding(innerPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (scannedTagId == null) {
                            Text("Please scan the RFID card.", style = MaterialTheme.typography.bodyLarge)
                        } else {
                            val displayName = nicknames[scannedTagId]
                            if (displayName != null) {
                                Text("Name: $displayName", style = MaterialTheme.typography.titleLarge)
                            } else {
                                Text("RFID: $scannedTagId", style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Select item to borrow:", style = MaterialTheme.typography.bodyLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items.forEach { item ->
                                    Button(
                                        onClick = { selectedItem = item },
                                        modifier = Modifier,
                                        shape = MaterialTheme.shapes.medium,
                                        colors = if (selectedItem == item) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.buttonColors()
                                    ) {
                                        Text(item, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                            if (selectedItem.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        onBorrow(scannedTagId, selectedItem)
                                        selectedItem = ""
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Borrow recorded")
                                        }
                                    },
                                    modifier = Modifier.padding(top = 8.dp),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Record Borrow", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                Text("Active Borrows:", style = MaterialTheme.typography.titleLarge)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(borrowLogs.filter { !it.isReturned }) { log ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val nickname = nicknames[log.tagId]
                                if (nickname != null) {
                                    Text("Name: $nickname", style = MaterialTheme.typography.bodyLarge)
                                } else {
                                    Text("Tag ID: ${log.tagId}", style = MaterialTheme.typography.bodyLarge)
                                }
                                Text("Item: ${log.itemName}", style = MaterialTheme.typography.bodyMedium)
                                Text("Borrowed: ${log.borrowTimestamp}", style = MaterialTheme.typography.bodyMedium)
                                Button(
                                    onClick = { onReturn(log) },
                                    modifier = Modifier.padding(top = 4.dp),
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Confirm Return", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
