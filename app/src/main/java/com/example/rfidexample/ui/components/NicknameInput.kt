package com.example.rfidexample.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NicknameInput(
    currentTagId: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nicknameInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    ElevatedCard(
        shape = CardDefaults.elevatedShape,
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add Nickname",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = nicknameInput,
                onValueChange = {
                    nicknameInput = it
                    if (showError && it.isNotBlank()) showError = false
                },
                label = { Text("Nickname for Tag: $currentTagId") },
                isError = showError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (showError) {
                Text(
                    text = "Nickname cannot be empty!",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start).padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    if (nicknameInput.isNotBlank()) {
                        onSave(nicknameInput.trim())
                        nicknameInput = ""
                        showError = false
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.material3.MaterialTheme.shapes.medium
            ) {
                Text(text = "Save Nickname")
            }
        }
    }
}
