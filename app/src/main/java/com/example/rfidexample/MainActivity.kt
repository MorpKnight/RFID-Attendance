package com.example.rfidexample

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
// import androidx.compose.material.icons.filled.Nfc // Already imported by HomeMenuItem if it used an Icon, but not directly by HomeMenu for the logo anymore.
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
// import androidx.compose.ui.res.painterResource // No longer needed for the logo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage // Added for Coil
import com.example.rfidexample.data.DataStoreManager
import com.example.rfidexample.data.model.BorrowLog
import com.example.rfidexample.ui.NfcReaderScreen
import com.example.rfidexample.ui.PeminjamanBarangScreen
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

    private var borrowLogs by mutableStateOf(listOf<BorrowLog>())
    private var scannedBorrowTagId by mutableStateOf<String?>(null)
    private var currentScreen by mutableStateOf("home") // "home", "attendance", "borrow"

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
            val loadedBorrowLogs = DataStoreManager.loadBorrowLogs(context)
            borrowLogs = loadedBorrowLogs
        }

        setContent {
            RFIDExampleTheme {
                AnimatedContent(
                    targetState = currentScreen,
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        "home" -> {
                            HomeMenu(
                                onAttendanceClick = { currentScreen = "attendance" },
                                onBorrowClick = { currentScreen = "borrow" }
                            )
                        }
                        "attendance" -> {
                            androidx.activity.compose.BackHandler(true) {
                                currentScreen = "home"
                            }
                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                NfcReaderScreen(
                                    tagData = tagData,
                                    currentTagId = currentTagId,
                                    tagNicknames = tagNicknames,
                                    addNickname = { id: String, nickname: String ->
                                        tagNicknames[id] = nickname
                                        lifecycleScope.launch {
                                            DataStoreManager.saveNicknames(applicationContext, tagNicknames.toMap())
                                        }
                                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                        tagData = "Nickname: $nickname\nTag ID: $id\nTimestamp: $timestamp"
                                        currentTagId = null
                                    },
                                    tagHistory = tagHistory,
                                    updateHistory = { newHistory: List<String> ->
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
                        "borrow" -> {
                            androidx.activity.compose.BackHandler(true) {
                                currentScreen = "home"
                            }
                            PeminjamanBarangScreen(
                                borrowLogs = borrowLogs,
                                onBorrow = { tagId: String, itemName: String ->
                                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                    val newLog = BorrowLog(tagId, itemName, timestamp, false, null)
                                    borrowLogs = borrowLogs + newLog
                                    lifecycleScope.launch {
                                        DataStoreManager.saveBorrowLogs(applicationContext, borrowLogs)
                                    }
                                    scannedBorrowTagId = null
                                },
                                onReturn = { log: BorrowLog ->
                                    val updatedLogs = borrowLogs.map {
                                        if (it == log) it.copy(isReturned = true, returnTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())) else it
                                    }
                                    borrowLogs = updatedLogs
                                    lifecycleScope.launch {
                                        DataStoreManager.saveBorrowLogs(applicationContext, borrowLogs)
                                    }
                                },
                                scannedTagId = scannedBorrowTagId,
                                nicknames = tagNicknames,
                                modifier = Modifier.fillMaxSize(),
                                onBack = { currentScreen = "home" }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntentFlags = PendingIntent.FLAG_MUTABLE
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: android.nfc.Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)

        if (tag != null) {
            val uid = tag.id.joinToString(":") { b -> "%02X".format(b) }
            if (currentScreen == "borrow") {
                scannedBorrowTagId = uid
            } else if (currentScreen == "attendance") {
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

    @Deprecated("Use BackHandler in Composable instead or manage currentScreen directly")
    override fun onBackPressed() {
        if (currentScreen == "attendance" || currentScreen == "borrow") {
            currentScreen = "home"
            // super.onBackPressed() // Removed as BackHandler is used
        } else {
            super.onBackPressed()
        }
    }
}

@Composable
fun HomeMenuItem(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp), // Reduced padding around Surface
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 16.dp) // Adjusted padding inside
                .fillMaxWidth() // Ensure items in Row take available width if weight is used
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp)) // Increased spacer
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium, // Slightly larger text
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HomeMenu(
    onAttendanceClick: () -> Unit,
    onBorrowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top 
    ) {
        Spacer(modifier = Modifier.height(32.dp)) // Spacer at the top
        AsyncImage(
            model = "https://cdn.digilabdte.com/u/mdh8f2.png",
            contentDescription = "App Logo",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(32.dp)) // Increased spacer
        Text(
            text = "Pilih Menu:",
            style = MaterialTheme.typography.titleLarge, // Larger title
            modifier = Modifier.padding(bottom = 24.dp) // Increased bottom padding
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top 
        ) {
            HomeMenuItem(
                text = "Absensi",
                icon = Icons.Filled.PlaylistAddCheck,
                contentDescription = "Menu Absensi",
                onClick = onAttendanceClick,
                modifier = Modifier.weight(1f)
            )
            HomeMenuItem(
                text = "Peminjaman",
                icon = Icons.Filled.Inventory2,
                contentDescription = "Menu Peminjaman Barang",
                onClick = onBorrowClick,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.weight(1f)) // Pushes content up
    }
}
