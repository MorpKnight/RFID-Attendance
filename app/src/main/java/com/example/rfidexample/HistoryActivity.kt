package com.example.rfidexample

import android.app.Activity
import android.app.Activity.RESULT_OK // Import for direct use
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Arrangement // Added import
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.rfidexample.ui.theme.RFIDExampleTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults

@OptIn(ExperimentalMaterial3Api::class)
class HistoryActivity : ComponentActivity() {
    private var currentHistory: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val historyItems = intent.getStringArrayListExtra("history") ?: arrayListOf()
        currentHistory = ArrayList(historyItems) // currentHistory remains ArrayList
        val nicknamesBundle = intent.getBundleExtra("nicknames")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val resultIntent = Intent()
                resultIntent.putStringArrayListExtra("history", currentHistory)
                setResult(RESULT_OK, resultIntent) // Simplified
                finish()
            }
        })

        setContent {
            RFIDExampleTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("History") })
                    },
                    bottomBar = {
                        Button(
                            onClick = {
                                val resultIntent = Intent()
                                resultIntent.putStringArrayListExtra("history", currentHistory)
                                setResult(RESULT_OK, resultIntent) // Simplified
                                finish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(text = "Back")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    HistoryScreen(
                        historyItems = historyItems, // Pass the ArrayList
                        nicknamesBundle = nicknamesBundle,
                        onHistoryChanged = { updatedHistory -> // Receives List<String>
                            currentHistory = ArrayList(updatedHistory) // Convert back to ArrayList for currentHistory
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyItems: List<String>,
    nicknamesBundle: Bundle?,
    onHistoryChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var historyList by remember { mutableStateOf(historyItems.toList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()

    LaunchedEffect(historyList) {
        onHistoryChanged(historyList)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp), // Corrected typo
            modifier = Modifier.fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search UID/Nickname") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.DateRange, contentDescription = "Pick Date")
                    Text(selectedDate.ifBlank { "Select Date" }, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                }
                if (selectedDate.isNotBlank()) {
                    Button(onClick = { selectedDate = "" }, modifier = Modifier.padding(start = 8.dp), shape = MaterialTheme.shapes.medium) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear Date")
                        Text("Clear", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort by Nickname:", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { sortAscending = !sortAscending }, shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Filled.SortByAlpha, contentDescription = "Sort")
                    Text(if (sortAscending) "Ascending" else "Descending", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            val filteredSortedList = historyList.filter { itemString ->
                val parts = itemString.split(',')
                val timestampString = parts.getOrNull(1) ?: ""
                val matchesDate = if (selectedDate.isBlank()) true else timestampString.startsWith(selectedDate)
                val uid = parts.getOrNull(0) ?: ""
                val nickname = nicknamesBundle?.getString(uid) ?: ""
                (searchQuery.isBlank() ||
                    uid.contains(searchQuery, ignoreCase = true) ||
                    nickname.contains(searchQuery, ignoreCase = true)) && matchesDate
            }.sortedWith(compareBy
                { val uid = it.split(',').getOrNull(0) ?: ""; nicknamesBundle?.getString(uid) ?: uid }
            )
            val finalList = if (sortAscending) filteredSortedList else filteredSortedList.reversed()
            if (finalList.isEmpty()) {
                Text(
                    text = "No history available",
                    modifier = Modifier.fillMaxSize().padding(top = 64.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    items(finalList) { itemString ->
                        val parts = itemString.split(',')
                        val uid = parts.getOrNull(0) ?: "Unknown UID"
                        val timestampString = parts.getOrNull(1)
                        val displayTimestamp = timestampString?.let {
                            try {
                                val parser = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                val date = parser.parse(it)
                                if (date != null) {
                                    val formatter = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                                    formatter.format(date)
                                } else {
                                    it
                                }
                            } catch (_: Exception) {
                                it
                            }
                        } ?: "Unknown time"
                        val nickname = nicknamesBundle?.getString(uid)
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(2.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    if (!nickname.isNullOrBlank()) {
                                        Text(
                                            text = nickname,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = uid,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    } else {
                                        Text(
                                            text = uid,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = displayTimestamp,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Button(onClick = {
                                    itemToDelete = itemString
                                    showDialog = true
                                }, shape = MaterialTheme.shapes.small, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                                    Text("Delete", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
            if (showDialog && itemToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Delete Entry", style = MaterialTheme.typography.titleLarge) },
                    text = { Text("Are you sure you want to delete this history entry?", style = MaterialTheme.typography.bodyLarge) },
                    confirmButton = {
                        Button(onClick = {
                            historyList = historyList.filter { it != itemToDelete }
                            showDialog = false
                            itemToDelete = null
                        }, shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Delete", style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showDialog = false
                            itemToDelete = null
                        }) {
                            Text("Cancel", style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    shape = MaterialTheme.shapes.large
                )
            }
        }
    }
}
