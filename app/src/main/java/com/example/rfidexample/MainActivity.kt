package com.example.rfidexample

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
// import androidx.activity.enableEdgeToEdge // Dihilangkan untuk konsistensi layout
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.rfidexample.data.DataStoreManager
import com.example.rfidexample.data.model.BorrowLog
import com.example.rfidexample.data.repository.BorrowLogRepository
import com.example.rfidexample.ui.NfcReaderScreen
import com.example.rfidexample.ui.PeminjamanBarangScreen
import com.example.rfidexample.ui.HistoryScreen
import com.example.rfidexample.ui.AttendanceHistoryScreen
import com.example.rfidexample.ui.HomeMenu
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
    private var currentScreen by mutableStateOf("home")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Dihilangkan untuk mengatasi masalah jarak/padding
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
                            "home" to 0, "attendance" to 1, "attendance_history" to 2,
                            "borrow" to 1, "borrow_history" to 2
                        )
                        val initialIdx = screenOrder[initialState] ?: 0
                        val targetIdx = screenOrder[targetState] ?: 0
                        val forward = targetIdx > initialIdx

                        if (forward) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }
                    }
                ) { screen ->
                    when (screen) {
                        "home" -> {
                            HomeMenu(
                                onAttendanceClick = { currentScreen = "attendance" },
                                onBorrowClick = { currentScreen = "borrow" },
                                tagHistory = tagHistory,
                                borrowLogs = borrowLogs
                            )
                        }
                        "attendance" -> {
                            androidx.activity.compose.BackHandler { currentScreen = "home" }
                            NfcReaderScreen(
                                tagData = tagData,
                                currentTagId = currentTagId,
                                tagNicknames = tagNicknames,
                                addNickname = { id, nickname ->
                                    tagNicknames[id] = nickname
                                    lifecycleScope.launch { DataStoreManager.saveNicknames(applicationContext, tagNicknames.toMap()) }
                                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                    tagData = "Nickname: $nickname\nTag ID: $id\nTimestamp: $timestamp"
                                    currentTagId = null
                                },
                                tagHistory = tagHistory,
                                updateHistory = { newHistory ->
                                    tagHistory = newHistory
                                    lifecycleScope.launch { DataStoreManager.saveHistory(applicationContext, tagHistory) }
                                },
                                clearNicknames = {
                                    tagNicknames.clear()
                                    lifecycleScope.launch { DataStoreManager.saveNicknames(applicationContext, tagNicknames.toMap()) }
                                },
                                updateAllNicknames = { updatedNicknamesMap ->
                                    tagNicknames.clear()
                                    tagNicknames.putAll(updatedNicknamesMap)
                                    lifecycleScope.launch { DataStoreManager.saveNicknames(applicationContext, tagNicknames.toMap()) }
                                },
                                onNavigateToHistory = { currentScreen = "attendance_history" }
                            )
                        }
                        "attendance_history" -> {
                            androidx.activity.compose.BackHandler { currentScreen = "attendance" }
                            AttendanceHistoryScreen(
                                tagHistory = tagHistory,
                                nicknames = tagNicknames,
                                onBack = { currentScreen = "attendance" },
                                onClearHistory = {
                                    tagHistory = emptyList()
                                    lifecycleScope.launch { DataStoreManager.saveHistory(applicationContext, tagHistory) }
                                }
                            )
                        }
                        "borrow" -> {
                            androidx.activity.compose.BackHandler { currentScreen = "home" }
                            PeminjamanBarangScreen(
                                borrowLogs = borrowLogs,
                                onBorrow = { tagId, itemName ->
                                    val newLog = BorrowLog(tagId = tagId, itemName = itemName, borrowTimestamp = System.currentTimeMillis())
                                    BorrowLogRepository.addLog(newLog)
                                    borrowLogs = BorrowLogRepository.getLogs()
                                    lifecycleScope.launch { DataStoreManager.saveBorrowLogs(applicationContext, borrowLogs) }
                                    scannedBorrowTagId = null
                                },
                                onReturn = { logToReturn ->
                                    BorrowLogRepository.recordReturn(logToReturn)
                                    borrowLogs = BorrowLogRepository.getLogs()
                                    lifecycleScope.launch { DataStoreManager.saveBorrowLogs(applicationContext, borrowLogs) }
                                },
                                scannedTagId = scannedBorrowTagId,
                                nicknames = tagNicknames,
                                modifier = Modifier.fillMaxSize(),
                                onBack = { currentScreen = "home" },
                                onNavigateToHistory = { currentScreen = "borrow_history" }
                            )
                        }
                        "borrow_history" -> {
                            androidx.activity.compose.BackHandler { currentScreen = "borrow" }
                            HistoryScreen(
                                borrowLogs = borrowLogs,
                                nicknames = tagNicknames,
                                onBack = { currentScreen = "borrow" },
                                onClearHistory = {
                                    BorrowLogRepository.clear()
                                    borrowLogs = BorrowLogRepository.getLogs()
                                    lifecycleScope.launch { DataStoreManager.saveBorrowLogs(applicationContext, borrowLogs) }
                                },
                                onDeleteLog = { logToDelete ->
                                    BorrowLogRepository.deleteLog(logToDelete)
                                    borrowLogs = BorrowLogRepository.getLogs()
                                    lifecycleScope.launch { DataStoreManager.saveBorrowLogs(applicationContext, borrowLogs) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ... (Sisa kode MainActivity tetap sama)
    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntentFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
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
                handleNfcAttendance(uid)
            }
        }
    }

    private fun handleNfcAttendance(uid: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val dateOnly = timestamp.substring(0, 10)

        val historyMap = tagHistory.associate {
            val parts = it.split(",")
            val entryUid = parts[0]
            val entryDate = parts[1].substring(0, 10)
            "$entryUid|$entryDate" to it
        }.toMutableMap()

        historyMap["$uid|$dateOnly"] = "$uid,$timestamp"

        tagHistory = historyMap.values.sortedByDescending { it.split(",")[1] }

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