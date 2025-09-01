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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DictionaryScreen(
                        initialNicknames = nicknames,
                        onDone = { updatedNicknames ->
                            val resultIntent = Intent()
                            resultIntent.putExtra("updated_nicknames", HashMap(updatedNicknames))
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DictionaryScreen(
    initialNicknames: HashMap<String, String>,
    onDone: (Map<String, String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var nicknames by remember { mutableStateOf(initialNicknames.toMutableMap()) }
    var newUid by remember { mutableStateOf("") }
    var newNickname by remember { mutableStateOf("") }
    var editUid by remember { mutableStateOf<String?>(null) }
    var editNickname by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Dictionary / Contacts", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(onClick = { showImportDialog = true }, shape = MaterialTheme.shapes.medium) {
                    Text("Import", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (showImportDialog) {
                AlertDialog(
                    onDismissRequest = { showImportDialog = false; importUrl = ""; importError = null },
                    title = { Text("Import Dictionary from URL", style = MaterialTheme.typography.titleLarge) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = importUrl,
                                onValueChange = { importUrl = it },
                                label = { Text("Enter URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (importError != null) {
                                Text(importError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            importError = null
                            coroutineScope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        val url = URL(importUrl)
                                        val connection = url.openConnection() as HttpURLConnection
                                        connection.requestMethod = "GET"
                                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                                        connection.connectTimeout = 5000
                                        connection.readTimeout = 5000
                                        val responseCode = connection.responseCode
                                        if (responseCode == 200) {
                                            val stream = connection.inputStream.bufferedReader().use { it.readText() }
                                            val jsonArray = JSONArray(stream)
                                            val imported = mutableMapOf<String, String>()
                                            for (i in 0 until jsonArray.length()) {
                                                val obj = jsonArray.getJSONObject(i)
                                                val tag = obj.optString("rfid_tag")
                                                val nickname = obj.optString("nickname")
                                                if (tag.isNotBlank() && nickname.isNotBlank()) {
                                                    imported[tag] = nickname
                                                }
                                            }
                                            Pair(imported, null)
                                        } else {
                                            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                                            Pair(null, "Failed to fetch: HTTP $responseCode\n$errorBody")
                                        }
                                    }
                                    val imported = result.first
                                    val error = result.second
                                    if (imported != null && imported.isNotEmpty()) {
                                        nicknames = nicknames.toMutableMap().apply { putAll(imported) }
                                        showImportDialog = false
                                        importUrl = ""
                                        importError = null
                                    } else if (error != null) {
                                        importError = error
                                    } else {
                                        importError = "No valid entries found in JSON."
                                    }
                                } catch (e: Exception) {
                                    Log.e("ImportError", "Exception during import", e)
                                    importError = "Error: " + (e.localizedMessage ?: e.toString())
                                }
                            }
                        }, shape = MaterialTheme.shapes.medium) {
                            Text("Import", style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportDialog = false }) {
                            Text("Cancel", style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    shape = MaterialTheme.shapes.large
                )
            }
            // Add new entry row
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = newUid,
                        onValueChange = { newUid = it },
                        label = { Text("Tag ID") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = newNickname,
                        onValueChange = { newNickname = it },
                        label = { Text("Nickname") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (newUid.isNotBlank() && newNickname.isNotBlank()) {
                                nicknames[newUid] = newNickname
                                newUid = ""
                                newNickname = ""
                            }
                        },
                        enabled = newUid.isNotBlank() && newNickname.isNotBlank(),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Add", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(nicknames.entries.toList(), key = { it.key }) { entry ->
                    val uid = entry.key
                    val nickname = entry.value
                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (editUid == uid) {
                                OutlinedTextField(
                                    value = editNickname,
                                    onValueChange = { editNickname = it },
                                    label = { Text("Edit Nickname") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(onClick = {
                                    if (editNickname.isNotBlank()) {
                                        nicknames[uid] = editNickname
                                        editUid = null
                                        editNickname = ""
                                    }
                                }, enabled = editNickname.isNotBlank(), shape = MaterialTheme.shapes.small) {
                                    Text("Save", style = MaterialTheme.typography.labelLarge)
                                }
                                TextButton(onClick = {
                                    editUid = null
                                    editNickname = ""
                                }) {
                                    Text("Cancel", style = MaterialTheme.typography.labelLarge)
                                }
                            } else {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(uid, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                                    Text(nickname, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1)
                                }
                                Button(onClick = {
                                    editUid = uid
                                    editNickname = nickname
                                }, shape = MaterialTheme.shapes.small) {
                                    Text("Edit", style = MaterialTheme.typography.labelLarge)
                                }
                                Button(onClick = {
                                    nicknames = nicknames.toMutableMap().apply { remove(uid) }
                                }, shape = MaterialTheme.shapes.small, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                    Text("Delete", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { onDone(nicknames) },
                modifier = Modifier.align(Alignment.End),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Done", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
