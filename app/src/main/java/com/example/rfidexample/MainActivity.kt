package com.example.rfidexample

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.rfidexample.ui.*
import com.example.rfidexample.ui.theme.RFIDExampleTheme

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private val viewModel: MainViewModel by viewModels()
    private var currentRoute: String? by mutableStateOf(null) // Store current route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            RFIDExampleTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                currentRoute = navBackStackEntry?.destination?.route // Update currentRoute

                val tagData by viewModel.tagData.collectAsStateWithLifecycle()
                val currentTagId by viewModel.currentTagId.collectAsStateWithLifecycle()
                val tagHistory by viewModel.tagHistory.collectAsStateWithLifecycle()
                val tagNicknames by viewModel.tagNicknames.collectAsStateWithLifecycle()
                val borrowLogs by viewModel.borrowLogs.collectAsStateWithLifecycle()
                val scannedBorrowTagId by viewModel.scannedBorrowTagId.collectAsStateWithLifecycle()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeMenu(
                            onAttendanceClick = { navController.navigate("attendance") },
                            onBorrowClick = { navController.navigate("borrow") },
                            tagHistory = tagHistory,
                            borrowLogs = borrowLogs
                        )
                    }
                    composable("attendance") {
                        NfcReaderScreen(
                            tagData = tagData,
                            currentTagId = currentTagId,
                            tagNicknames = tagNicknames,
                            addNickname = viewModel::addNickname,
                            tagHistory = tagHistory,
                            updateHistory = viewModel::updateHistory,
                            clearNicknames = viewModel::clearNicknames,
                            updateAllNicknames = viewModel::updateAllNicknames,
                            onNavigateToHistory = { navController.navigate("attendance_history") }
                        )
                    }
                    composable("attendance_history") {
                        AttendanceHistoryScreen(
                            tagHistory = tagHistory,
                            nicknames = tagNicknames,
                            onBack = { navController.popBackStack() },
                            onClearHistory = { viewModel.updateHistory(emptyList()) }
                        )
                    }
                    composable("borrow") {
                        PeminjamanBarangScreen(
                            borrowLogs = borrowLogs,
                            onBorrow = viewModel::onBorrow,
                            onReturn = viewModel::onReturn,
                            scannedTagId = scannedBorrowTagId,
                            nicknames = tagNicknames,
                            onBack = { navController.popBackStack() },
                            onNavigateToHistory = { navController.navigate("borrow_history") }
                        )
                    }
                    composable("borrow_history") {
                        HistoryScreen(
                            borrowLogs = borrowLogs,
                            nicknames = tagNicknames,
                            onBack = { navController.popBackStack() },
                            onClearHistory = viewModel::onClearBorrowHistory,
                            onDeleteLog = viewModel::onDeleteBorrowLog
                        )
                    }
                }
            }
        }
    }

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
        val tag: android.nfc.Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        tag?.let {
            val uid = it.id.joinToString(":") { b -> "%02X".format(b) }
            // Use the currentRoute property that is updated by Compose
            viewModel.handleNfcTag(uid, currentRoute)
        }
    }

    // You can remove getTopmostRoute() if you use the approach above
    // private fun getTopmostRoute(): String? {
    //     // Implementasi ini adalah workaround dan mungkin perlu disesuaikan
    //     // jika struktur navigasi menjadi lebih kompleks (misalnya nested navigation).
    //     val navHostFragment = supportFragmentManager.findFragmentById(android.R.id.content)
    //     val currentNavController = navHostFragment?.findNavController()
    //     return currentNavController?.currentDestination?.route
    // }
}