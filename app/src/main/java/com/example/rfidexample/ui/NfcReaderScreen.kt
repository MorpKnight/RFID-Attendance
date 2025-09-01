package com.example.rfidexample.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rfidexample.ui.components.NicknameInput
import com.example.rfidexample.ui.components.TagAttendanceList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcReaderScreen(
    tagData: String,
    currentTagId: String?,
    tagNicknames: Map<String, String>,
    addNickname: (String, String) -> Unit,
    tagHistory: List<String>,
    updateHistory: (List<String>) -> Unit,
    clearNicknames: () -> Unit,
    updateAllNicknames: (updatedNicknames: Map<String, String>) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val dictionaryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val serializableExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra("updated_nicknames", HashMap::class.java)
            } else {
                @Suppress("DEPRECATION", "UNCHECKED_CAST")
                result.data?.getSerializableExtra("updated_nicknames") as? HashMap<String, String>
            }
            val updatedNicknames = serializableExtra as? Map<String, String>
            if (updatedNicknames != null) {
                clearNicknames()
                updateAllNicknames(updatedNicknames)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Nicknames updated from Dictionary")
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("RFID Attendance") },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent()
                        intent.setClassName("com.example.rfidexample", "com.example.rfidexample.DictionaryActivity")
                        intent.putExtra("nicknames", HashMap(tagNicknames))
                        dictionaryLauncher.launch(intent)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Manage Nicknames")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Filled.History, contentDescription = "View Full Attendance History")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Jika tidak ada kartu yang di-scan, tampilkan prompt
                if (currentTagId == null && tagHistory.isEmpty()) {
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
                            "Silakan scan kartu RFID untuk absensi",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Form input nickname jika ada kartu baru
                if (currentTagId != null && !tagNicknames.containsKey(currentTagId)) {
                    NicknameInput(
                        currentTagId = currentTagId,
                        onSave = { nickname ->
                            addNickname(currentTagId, nickname)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Nickname saved: $nickname")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Daftar absensi
            if (tagHistory.isNotEmpty()) {
                item {
                    Text(
                        "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TagAttendanceList(
                        tagNicknames = tagNicknames,
                        tagHistory = tagHistory
                    )
                }
            }
        }
    }
}