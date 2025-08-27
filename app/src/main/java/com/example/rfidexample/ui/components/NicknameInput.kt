package com.example.rfidexample.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NicknameInput(
    currentTagId: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nicknameInput by remember { mutableStateOf("") }
    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.fillMaxWidth()
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
                    onSave(nicknameInput)
                    nicknameInput = ""
                }
            }) {
                Text(text = "Save Nickname")
            }
        }
    }
}

