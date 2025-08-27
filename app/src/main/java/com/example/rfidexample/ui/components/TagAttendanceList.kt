package com.example.rfidexample.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = modifier.fillMaxWidth(),
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
        Spacer(modifier = Modifier.height(0.dp))
    }
}
