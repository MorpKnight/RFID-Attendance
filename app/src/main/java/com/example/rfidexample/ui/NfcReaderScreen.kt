package com.example.rfidexample.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    updateHistory: (List<String>) -> Unit, // Parameter type changed to List
    clearNicknames: () -> Unit,
    updateAllNicknames: (updatedNicknames: Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newHistory = result.data?.getStringArrayListExtra("history")
            if (newHistory != null) {
                updateHistory(newHistory.toList()) // Convert to immutable List for state update
            }
        }
    }

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
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RFID Example") },
                actions = {
                    IconButton(onClick = {
                        updateHistory(emptyList())
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("History cleared")
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear History")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = tagData)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (currentTagId != null && !tagNicknames.containsKey(currentTagId)) {
                NicknameInput(
                    currentTagId = currentTagId,
                    onSave = { nickname ->
                        addNickname(currentTagId, nickname)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Nickname saved")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val intent = Intent(context, Class.forName("com.example.rfidexample.HistoryActivity"))
                    intent.putStringArrayListExtra("history", ArrayList(tagHistory))
                    val nicknamesBundle = android.os.Bundle()
                    tagNicknames.forEach { (id, nickname) ->
                        nicknamesBundle.putString(id, nickname)
                    }
                    intent.putExtra("nicknames", nicknamesBundle)
                    launcher.launch(intent)
                }) {
                    Icon(Icons.Filled.History, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(text = "View History")
                }
                Button(onClick = {
                    val intent = Intent(context, Class.forName("com.example.rfidexample.DictionaryActivity"))
                    intent.putExtra("nicknames", HashMap(tagNicknames))
                    dictionaryLauncher.launch(intent)
                }) {
                    Icon(Icons.Filled.Menu, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Dictionary")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                clearNicknames()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("All nicknames cleared")
                }
            }) {
                Text(text = "Clear All Nicknames")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TagAttendanceList(
                tagNicknames = tagNicknames,
                tagHistory = tagHistory
            )
        }
    }
}
