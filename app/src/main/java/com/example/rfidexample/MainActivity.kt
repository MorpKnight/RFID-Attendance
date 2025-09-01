package com.example.rfidexample

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rfidexample.ui.*
import com.example.rfidexample.ui.theme.RFIDExampleTheme

// Variabel global untuk menyimpan NavController
// Ini adalah workaround untuk mengakses NavController dari onNewIntent
// yang berada di luar Composable.
private var globalNavController: NavHostController? = null

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            RFIDExampleTheme {
                val navController = rememberNavController()
                // Set NavController ke variabel global
                globalNavController = navController

                val tagData by viewModel.tagData.collectAsStateWithLifecycle()
                val currentTagId by viewModel.currentTagId.collectAsStateWithLifecycle()
                val tagHistory by viewModel.tagHistory.collectAsStateWithLifecycle()
                val tagNicknames by viewModel.tagNicknames.collectAsStateWithLifecycle()
                val borrowLogs by viewModel.borrowLogs.collectAsStateWithLifecycle()
                val scannedBorrowTagId by viewModel.scannedBorrowTagId.collectAsStateWithLifecycle()

                NavHost(
                    navController = navController,
                    startDestination = "home",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth }, // Slide in from the right
                            animationSpec = tween(300)
                        )
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth }, // Slide out to the left
                            animationSpec = tween(300)
                        )
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth }, // Slide in from the left
                            animationSpec = tween(300)
                        )
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth }, // Slide out to the right
                            animationSpec = tween(300)
                        )
                    }
                ) {
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

            // Ambil rute saat ini dari globalNavController
            val currentRoute = globalNavController?.currentBackStackEntry?.destination?.route
            viewModel.handleNfcTag(uid, currentRoute)
        }
    }
}