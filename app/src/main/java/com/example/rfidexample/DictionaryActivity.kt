package com.example.rfidexample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // For back navigation if needed
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudDownload // For Import
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.rfidexample.ui.theme.RFIDExampleTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log

class DictionaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nicknames = intent.getSerializableExtra("nicknames") as? HashMap<String, String> ?: hashMapOf()
        setContent {
            RFIDExampleTheme {
                // Scaffold is now in DictionaryScreen
                DictionaryScreen(
                    initialNicknames = nicknames,
                    onDone = { updatedNicknames ->
                        val resultIntent = Intent()
                        resultIntent.putExtra("updated_nicknames", HashMap(updatedNicknames))
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    },
                    // Pass a lambda to handle back press if TopAppBar needs it
                    // onNavigateBack = { finish() } // Example
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    initialNicknames: Map<String, String>, // Changed to Map for immutability from caller
    onDone: (Map<String, String>) -> Unit,
    // onNavigateBack: () -> Unit, // Add if TopAppBar needs a back button
    modifier: Modifier = Modifier
) {
    var nicknamesState by remember { mutableStateOf(initialNicknames.toMutableMap()) }
    
    // States for Add/Edit Dialog
    var showAddEditDialog by remember { mutableStateOf(false) }
    var currentEditUid by remember { mutableStateOf<String?>(null) } // null for Add, non-null for Edit
    var dialogUidInput by remember { mutableStateOf("") }
    var dialogNicknameInput by remember { mutableStateOf("") }
    var dialogInputError by remember { mutableStateOf<String?>(null) }

    // States for Import Dialog
    var showImportDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }
    var importInProgress by remember { mutableStateOf(false) }
    var importDialogError by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Manage Nicknames") },
                // navigationIcon = { // Optional: if you want a back button
                //     IconButton(onClick = onNavigateBack) {
                //         Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                //     }
                // },
                actions = {
                    TextButton(onClick = { showImportDialog = true }) {
                        Text("Import")
                    }
                    IconButton(onClick = { onDone(nicknamesState) }) {
                        Icon(Icons.Filled.Check, contentDescription = "Done")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                currentEditUid = null // Set to Add mode
                dialogUidInput = ""
                dialogNicknameInput = ""
                dialogInputError = null
                showAddEditDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Nickname")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp) // Add horizontal padding for content
                .fillMaxSize()
        ) {
            if (nicknamesState.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No nicknames yet. Tap the '+' button to add.", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp) // Padding for the list itself
                ) {
                    items(nicknamesState.entries.toList(), key = { it.key }) { entry ->
                        val uid = entry.key
                        val nickname = entry.value
                        ListItem(
                            headlineContent = { Text(nickname, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(uid) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = {
                                        currentEditUid = uid
                                        dialogUidInput = uid // UID is not editable here, but shown for context
                                        dialogNicknameInput = nickname
                                        dialogInputError = null
                                        showAddEditDialog = true
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit Nickname")
                                    }
                                    IconButton(onClick = {
                                        nicknamesState = nicknamesState.toMutableMap().apply { remove(uid) }
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Nickname for $uid deleted")
                                        }
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete Nickname", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            modifier = Modifier.padding(vertical = 4.dp) // Optional: for spacing between items
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    // Add/Edit Nickname Dialog
    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { Text(if (currentEditUid == null) "Add New Nickname" else "Edit Nickname") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dialogUidInput,
                        onValueChange = { if (currentEditUid == null) dialogUidInput = it else {} /* UID not editable on edit */ },
                        label = { Text("Tag ID") },
                        singleLine = true,
                        readOnly = currentEditUid != null, // UID is read-only when editing
                        isError = dialogInputError?.contains("ID") == true
                    )
                    OutlinedTextField(
                        value = dialogNicknameInput,
                        onValueChange = { dialogNicknameInput = it },
                        label = { Text("Nickname") },
                        singleLine = true,
                        isError = dialogInputError?.contains("Nickname") == true
                    )
                    if (dialogInputError != null) {
                        Text(dialogInputError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    dialogInputError = null
                    if (dialogUidInput.isBlank()) {
                        dialogInputError = "Tag ID cannot be empty."
                        return@Button
                    }
                    if (dialogNicknameInput.isBlank()) {
                        dialogInputError = "Nickname cannot be empty."
                        return@Button
                    }

                    if (currentEditUid == null && nicknamesState.containsKey(dialogUidInput)) {
                         dialogInputError = "Tag ID already exists."
                         return@Button
                    }

                    nicknamesState = nicknamesState.toMutableMap().apply { this[dialogUidInput] = dialogNicknameInput }
                    val message = if (currentEditUid == null) "Nickname added" else "Nickname updated"
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                    showAddEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { if (!importInProgress) showImportDialog = false },
            title = { Text("Import Nicknames via URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a URL pointing to a JSON array, where each object has 'rfid_tag' and 'nickname' keys.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = importUrl,
                        onValueChange = { importUrl = it },
                        label = { Text("JSON URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = importDialogError != null && !importInProgress
                    )
                    if (importInProgress) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                           CircularProgressIndicator(modifier = Modifier.size(24.dp))
                           Spacer(Modifier.width(8.dp))
                           Text("Importing...")
                        }
                    }
                    importDialogError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        importDialogError = null
                        importInProgress = true
                        coroutineScope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    // Basic validation, more robust validation would be needed
                                    if (!importUrl.startsWith("http://") && !importUrl.startsWith("https://")) {
                                        return@withContext Pair(null, "Invalid URL format. Must start with http:// or https://")
                                    }
                                    val url = URL(importUrl)
                                    val connection = url.openConnection() as HttpURLConnection
                                    connection.requestMethod = "GET"
                                    connection.setRequestProperty("User-Agent", "RFIDExampleApp/1.0")
                                    connection.connectTimeout = 10000 // Increased timeout
                                    connection.readTimeout = 10000    // Increased timeout
                                    
                                    val responseCode = connection.responseCode
                                    if (responseCode == HttpURLConnection.HTTP_OK) {
                                        val stream = connection.inputStream.bufferedReader().use { it.readText() }
                                        val jsonArray = JSONArray(stream)
                                        val imported = mutableMapOf<String, String>()
                                        var count = 0
                                        for (i in 0 until jsonArray.length()) {
                                            val obj = jsonArray.getJSONObject(i)
                                            val tag = obj.optString("rfid_tag")
                                            val nickname = obj.optString("nickname")
                                            if (tag.isNotBlank() && nickname.isNotBlank()) {
                                                imported[tag] = nickname
                                                count++
                                            }
                                        }
                                        if (count > 0) Pair(imported, null) else Pair(null, "No valid entries (rfid_tag, nickname) found in JSON.")
                                    } else {
                                        Pair(null, "Failed to fetch: HTTP $responseCode")
                                    }
                                }
                                val importedData = result.first
                                val error = result.second

                                if (importedData != null && importedData.isNotEmpty()) {
                                    nicknamesState = nicknamesState.toMutableMap().apply { putAll(importedData) }
                                    showImportDialog = false
                                    importUrl = ""
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("${'$'}{importedData.size} nicknames imported successfully.")
                                    }
                                } else {
                                    importDialogError = error ?: "Import failed or no data."
                                }
                            } catch (e: Exception) {
                                Log.e("ImportError", "Exception during import", e)
                                importDialogError = "Error: ${'$'}{e.localizedMessage ?: 'An unknown error occurred'}"
                            } finally {
                                importInProgress = false
                            }
                        }
                    },
                    enabled = !importInProgress && importUrl.isNotBlank()
                ) { Text("Import") }
            },
            dismissButton = {
                if (!importInProgress) {
                    TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}
