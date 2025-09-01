package com.example.rfidexample.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Import rememberScrollState
import androidx.compose.foundation.verticalScroll // Import verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
// import androidx.compose.material.icons.filled.Menu // Not used in this version
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Import Color
import androidx.compose.ui.input.nestedscroll.nestedScroll // Import nestedScroll
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

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
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("RFID Attendance") },
                actions = {
                    IconButton(onClick = {
                        updateHistory(emptyList())
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Attendance history cleared")
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear Attendance History")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // Nickname input section
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action Buttons section
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Button(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View Full Attendance History")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent()
                            intent.setClassName("com.example.rfidexample", "com.example.rfidexample.DictionaryActivity")
                            intent.putExtra("nicknames", HashMap(tagNicknames))
                            dictionaryLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manage Nicknames")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Attendance list section
            Text(
                "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TagAttendanceList(
                tagNicknames = tagNicknames,
                tagHistory = tagHistory
            )
            Spacer(modifier = Modifier.weight(1f))
            // Spacer to push the button to the bottom
            Button(
                onClick = {
                    clearNicknames()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("All nicknames cleared")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Clear All Nicknames")
            }
        }
    }
}