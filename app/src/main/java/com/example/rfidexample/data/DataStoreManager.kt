package com.example.rfidexample.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.rfidexample.data.model.Tag
import com.example.rfidexample.data.model.Attendance
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DataStoreManager {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rfid_nicknames")
    private val NICKNAMES_KEY = stringPreferencesKey("tag_nicknames_map")
    private val HISTORY_KEY = stringPreferencesKey("tag_history_list")

    suspend fun saveNicknames(context: Context, nicknames: Map<String, String>) {
        val jsonString = Json.encodeToString(nicknames)
        context.dataStore.edit { preferences ->
            preferences[NICKNAMES_KEY] = jsonString
        }
    }

    suspend fun loadNicknames(context: Context): Map<String, String> {
        val preferences = context.dataStore.data.first()
        val jsonString = preferences[NICKNAMES_KEY]
        return if (jsonString != null) {
            Json.decodeFromString(jsonString)
        } else {
            emptyMap()
        }
    }

    suspend fun saveHistory(context: Context, history: List<String>) {
        val jsonString = Json.encodeToString(history)
        context.dataStore.edit { preferences ->
            preferences[HISTORY_KEY] = jsonString
        }
    }

    suspend fun loadHistory(context: Context): List<String> {
        val preferences = context.dataStore.data.first()
        val jsonString = preferences[HISTORY_KEY]
        return if (jsonString != null) {
            Json.decodeFromString(jsonString)
        } else {
            emptyList()
        }
    }
}

