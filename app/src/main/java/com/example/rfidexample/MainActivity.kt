package com.example.rfidexample

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.rfidexample.data.DataStoreManager
import com.example.rfidexample.data.model.BorrowLog
import com.example.rfidexample.data.repository.BorrowLogRepository
import com.example.rfidexample.ui.NfcReaderScreen
import com.example.rfidexample.ui.PeminjamanBarangScreen
import com.example.rfidexample.ui.HistoryScreen
import com.example.rfidexample.ui.AttendanceHistoryScreen // Import AttendanceHistoryScreen
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
    // Add "attendance_history" to possible screens
    private var currentScreen by mutableStateOf("home") // "home", "attendance", "attendance_history", "borrow", "borrow_history"

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
            BorrowLogRepository.clear()
            loadedBorrowLogs.forEach { log ->
                BorrowLogRepository.addLog(log)
            }
        }

        setContent {
            RFIDExampleTheme {
                AnimatedContent(
                    targetState = currentScreen,
                    label = "ScreenTransition",
                    transitionSpec = {
                        val screenOrder = mapOf(
                            "home" to 0,
                            "attendance" to 1,
                            "attendance_history" to 2, // Attendance history is child of attendance
                            "borrow" to 1,
                            "borrow_history" to 2 // Borrow history is child of borrow
                        )

                        val initialScreenIndex = screenOrder[initialState] ?: -1
                        val targetScreenIndex = screenOrder[targetState] ?: -1
                        
                        // Determine direction based on defined hierarchy first
                        val goingForward = when {
                            // Home to direct children
                            initialState == "home" && (targetState == "attendance" || targetState == "borrow") -> true
                            // Attendance to its history
                            initialState == "attendance" && targetState == "attendance_history" -> true
                            // Borrow to its history
                            initialState == "borrow" && targetState == "borrow_history" -> true
                            // Attendance history back to attendance
                            initialState == "attendance_history" && targetState == "attendance" -> false
                             // Borrow history back to borrow
                            initialState == "borrow_history" && targetState == "borrow" -> false
                            // Direct children back to Home
                            (initialState == "attendance" || initialState == "borrow") && targetState == "home" -> false
                            // Fallback based on indices if hierarchy is not explicitly defined above
                            initialScreenIndex != -1 && targetScreenIndex != -1 -> targetScreenIndex > initialScreenIndex
                            // Default (e.g. if one screen is not in screenOrder)
                            else -> true 
                        }

                        if (goingForward) {
                            slideInHorizontally { fullWidth -> fullWidth } togetherWith
                                    slideOutHorizontally { fullWidth -> -fullWidth }
                        } else {
                            slideInHorizontally { fullWidth -> -fullWidth } togetherWith
                                    slideOutHorizontally { fullWidth -> fullWidth }
                        }
                    }
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
                                    updateHistory = { newHistory -> // This is for clearing history from NfcReaderScreen directly
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
                                    // Add navigation to attendance history
                                    onNavigateToHistory = { currentScreen = "attendance_history" },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                        "attendance_history" -> {
                            androidx.activity.compose.BackHandler(true) {
                                currentScreen = "attendance"
                            }
                            AttendanceHistoryScreen(
                                tagHistory = tagHistory,
                                nicknames = tagNicknames,
                                onBack = { currentScreen = "attendance" },
                                onClearHistory = {
                                    tagHistory = emptyList()
                                    lifecycleScope.launch {
                                        DataStoreManager.saveHistory(applicationContext, tagHistory)
                                    }
                                }
                            )
                        }
                        "borrow" -> {
                            androidx.activity.compose.BackHandler(true) {
                                currentScreen = "home"
                            }
                            PeminjamanBarangScreen(
                                borrowLogs = borrowLogs,
                                onBorrow = { tagId, itemName ->
                                    val newLog = BorrowLog(tagId = tagId, itemName = itemName, borrowTimestamp = System.currentTimeMillis())
                                    BorrowLogRepository.addLog(newLog)
                                    borrowLogs = BorrowLogRepository.getLogs()
                                    lifecycleScope.launch {
                                        DataStoreManager.saveBorrowLogs(applicationContext, borrowLogs)
                                    }
                                    scannedBorrowTagId = null
                                },
                                onReturn = { logToReturn ->
                                    BorrowLogRepository.recordReturn(logToReturn)
                                    borrowLogs = BorrowLogRepository.getLogs()
                                    lifecycleScope.launch {
                                        DataStoreManager.saveBorrowLogs(applicationContext, borrowLogs)
                                    }
                                },
                                scannedTagId = scannedBorrowTagId,
                                nicknames = tagNicknames,
                                modifier = Modifier.fillMaxSize(),
                                onBack = { currentScreen = "home" },
                                onNavigateToHistory = { currentScreen = "borrow_history" }
                            )
                        }
                        "borrow_history" -> {
                            androidx.activity.compose.BackHandler(true) {
                                currentScreen = "borrow"
                            }
                            HistoryScreen( // This is for borrow history
                                borrowLogs = borrowLogs,
                                nicknames = tagNicknames,
                                onBack = { currentScreen = "borrow" },
                                onClearHistory = {
                                    BorrowLogRepository.clear()
                                    borrowLogs = BorrowLogRepository.getLogs()
                                    lifecycleScope.launch {
                                        DataStoreManager.saveBorrowLogs(applicationContext, borrowLogs)
                                    }
                                },
                                onDeleteLog = { logToDelete ->
                                    BorrowLogRepository.deleteLog(logToDelete)
                                    borrowLogs = BorrowLogRepository.getLogs()
                                    lifecycleScope.launch {
                                        DataStoreManager.saveBorrowLogs(applicationContext, borrowLogs)
                                    }
                                }
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
                val dateOnly = timestamp.substring(0, 10)
                val historyMap = mutableMapOf<String, String>()
                tagHistory.forEach { entry ->
                    val parts = entry.split(",")
                    if (parts.size == 2) {
                        val entryUid = parts[0]
                        val entryDate = parts[1].substring(0, 10)
                        historyMap["$entryUid|$entryDate"] = parts[1]
                    }
                }
                historyMap["$uid|$dateOnly"] = timestamp
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
        // Updated BackHandler logic for new screens
        when (currentScreen) {
            "attendance_history" -> currentScreen = "attendance"
            "borrow_history" -> currentScreen = "borrow"
            "attendance", "borrow" -> currentScreen = "home"
            else -> super.onBackPressed() // Fallback to default for "home" or other unexpected states
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
            .padding(4.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
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
        Spacer(modifier = Modifier.height(32.dp))
        AsyncImage(
            model = "https://cdn.digilabdte.com/u/mdh8f2.png",
            contentDescription = "App Logo",
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Pilih Menu:",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
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
        Spacer(modifier = Modifier.weight(1f))
    }
}
