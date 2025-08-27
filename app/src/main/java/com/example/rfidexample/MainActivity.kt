package com.example.rfidexample

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import com.example.rfidexample.data.DataStoreManager
import com.example.rfidexample.data.model.Tag
import com.example.rfidexample.data.model.Attendance
import com.example.rfidexample.ui.theme.RFIDExampleTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var tagData by mutableStateOf("Scan a tag")
    private var currentTagId by mutableStateOf<String?>(null)
    private var tagHistory by mutableStateOf(listOf<String>())
    private val tagNicknames = mutableStateMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val context = this

        lifecycleScope.launch {
            val loadedNicknames = DataStoreManager.loadNicknames(context)
            tagNicknames.putAll(loadedNicknames)
            val loadedHistory = DataStoreManager.loadHistory(context)
            tagHistory = loadedHistory
        }

        setContent {
            RFIDExampleTheme {
                val coroutineScope = rememberCoroutineScope()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NfcReaderScreen(
                        tagData = tagData,
                        currentTagId = currentTagId,
                        tagNicknames = tagNicknames,
                        addNickname = { id, nickname ->
                            tagNicknames[id] = nickname
                            lifecycleScope.launch {
                                DataStoreManager.saveNicknames(applicationContext, tagNicknames.toMap())
                            }
                            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            tagData = "Nickname: $nickname\nTag ID: $id\nTimestamp: $timestamp"
                            currentTagId = null
                        },
                        tagHistory = tagHistory,
                        updateHistory = { newHistory ->
                            tagHistory = newHistory
                            lifecycleScope.launch {
                                DataStoreManager.saveHistory(applicationContext, tagHistory)
                            }
                        },
                        clearNicknames = {
                            tagNicknames.clear()
                            lifecycleScope.launch {
                                DataStoreManager.saveNicknames(applicationContext, tagNicknames.toMap())
                            }
                        },
                        updateAllNicknames = { updatedNicknamesMap ->
                            tagNicknames.clear()
                            tagNicknames.putAll(updatedNicknamesMap)
                            lifecycleScope.launch {
                                DataStoreManager.saveNicknames(applicationContext, tagNicknames.toMap())
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        // Ensure PendingIntent flags are set correctly based on Android version
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0 // For older versions, 0 is often fine or use PendingIntent.FLAG_UPDATE_CURRENT if needed
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: android.nfc.Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag != null) {
            val uid = tag.id.joinToString(":") { b -> "%02X".format(b) }
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val dateOnly = timestamp.substring(0, 10) // yyyy-MM-dd
            // Buat map untuk cek UID+date
            val historyMap = mutableMapOf<String, String>() // key: UID+date, value: timestamp
            tagHistory.forEach { entry ->
                val parts = entry.split(",")
                if (parts.size == 2) {
                    val entryUid = parts[0]
                    val entryDate = parts[1].substring(0, 10)
                    historyMap["$entryUid|$entryDate"] = parts[1]
                }
            }
            // Update/replace jika sudah ada, atau tambah jika belum
            historyMap["$uid|$dateOnly"] = timestamp
            // Konversi kembali ke list format "UID,timestamp", urutkan dari yang terbaru
            val newHistory = historyMap.entries
                .sortedByDescending { it.value }
                .map { entry ->
                    val (key, value) = entry
                    val entryUid = key.substringBefore("|")
                    "$entryUid,$value"
                }
            tagHistory = newHistory
            lifecycleScope.launch {
                DataStoreManager.saveHistory(applicationContext, tagHistory)
            }
            val nickname = tagNicknames[uid]
            if (nickname != null) {
                tagData = "Nickname: $nickname\nTag ID: $uid\nTimestamp: $timestamp"
                currentTagId = null
            } else {
                tagData = "Tag ID: $uid\nTimestamp: $timestamp"
                currentTagId = uid
            }
        }
    }
}

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
    var nicknameInput by remember { mutableStateOf("") }
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
            // Updated way to get serializable extra
            val serializableExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra("updated_nicknames", HashMap::class.java)
            } else {
                @Suppress("DEPRECATION", "UNCHECKED_CAST")
                result.data?.getSerializableExtra("updated_nicknames") as? HashMap<String, String>
            }
            val updatedNicknames = serializableExtra as? Map<String, String> // Cast to Map

            if (updatedNicknames != null) {
                clearNicknames()
                updateAllNicknames(updatedNicknames)
                // Optionally, save nicknames if DictionaryActivity modified them and they should persist
                // val coroutineScope = rememberCoroutineScope() // If needed here
                // coroutineScope.launch { saveNicknames(context, tagNicknames.toMap()) }
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
                Card(
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = nicknameInput,
                            onValueChange = { nicknameInput = it },
                            label = { Text("Enter Nickname for Tag ID: $currentTagId") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            if (nicknameInput.isNotBlank()) {
                                addNickname(currentTagId, nicknameInput)
                                nicknameInput = ""
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Nickname saved")
                                }
                            }
                        }) {
                            Text(text = "Save Nickname")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val intent = Intent(context, HistoryActivity::class.java)
                    intent.putStringArrayListExtra("history", ArrayList(tagHistory))
                    val nicknamesBundle = Bundle()
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
                    val intent = Intent(context, DictionaryActivity::class.java)
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
            // --- Tag Attendance Indicator List (English, only indicator, hide if no tags, scrollable) ---
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val allTagIds = (tagNicknames.keys + tagHistory.map { it.substringBefore(",") }).toSet().filter { it.isNotBlank() }
            if (allTagIds.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    content = {
                        item {
                            Text(text = "Today's Attendance Status:", modifier = Modifier.padding(bottom = 8.dp))
                        }
                        items(allTagIds.size) { idx ->
                            val tagId = allTagIds.elementAt(idx)
                            val nickname = tagNicknames[tagId] ?: "(No Nickname)"
                            val todayEntry = tagHistory.find {
                                val parts = it.split(",")
                                parts.size == 2 && parts[0] == tagId && parts[1].startsWith(today)
                            }
                            val status = if (todayEntry != null) "Checked in today" else "Not checked in today"
                            Card(
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "${nickname} (ID: $tagId)", modifier = Modifier.weight(1f))
                                    Text(text = status)
                                }
                            }
                        }
                    }
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
