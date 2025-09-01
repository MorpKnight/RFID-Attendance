package com.example.rfidexample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TagAttendanceList(
    tagNicknames: Map<String, String>,
    tagHistory: List<String>,
    modifier: Modifier = Modifier
) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val allTagIds = (tagNicknames.keys + tagHistory.map { it.substringBefore(",") }).toSet().filter { it.isNotBlank() }

    if (allTagIds.isNotEmpty()) {
        Column(modifier = modifier.fillMaxWidth()) {
            allTagIds.forEach { tagId ->
                val nickname = tagNicknames[tagId] ?: "(No Nickname)"
                val todayEntry = tagHistory.find {
                    val parts = it.split(",")
                    parts.size == 2 && parts[0] == tagId && parts[1].startsWith(today)
                }
                val status = if (todayEntry != null) "Checked in today" else "Not checked in today"
                val statusColor = if (todayEntry != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = nickname, fontWeight = FontWeight.Bold)
                            Text(text = "ID: $tagId", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        }
                        Text(text = status, color = statusColor, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    } else {
        Text("No attendance data for today.", modifier = Modifier.padding(16.dp))
    }
}