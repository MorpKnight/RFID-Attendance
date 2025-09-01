package com.example.rfidexample

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidexample.data.DataStoreManager
import com.example.rfidexample.data.model.BorrowLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext

    private val _tagData = MutableStateFlow("Scan a tag")
    val tagData: StateFlow<String> = _tagData

    private val _currentTagId = MutableStateFlow<String?>(null)
    val currentTagId: StateFlow<String?> = _currentTagId

    private val _tagHistory = MutableStateFlow<List<String>>(emptyList())
    val tagHistory: StateFlow<List<String>> = _tagHistory

    private val _tagNicknames = MutableStateFlow<Map<String, String>>(emptyMap())
    val tagNicknames: StateFlow<Map<String, String>> = _tagNicknames

    private val _borrowLogs = MutableStateFlow<List<BorrowLog>>(emptyList())
    val borrowLogs: StateFlow<List<BorrowLog>> = _borrowLogs

    private val _scannedBorrowTagId = MutableStateFlow<String?>(null)
    val scannedBorrowTagId: StateFlow<String?> = _scannedBorrowTagId

    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen

    init {
        viewModelScope.launch {
            val loadedNicknames = DataStoreManager.loadNicknames(context)
            _tagNicknames.value = loadedNicknames
            val loadedHistory = DataStoreManager.loadHistory(context)
            _tagHistory.value = loadedHistory
            val loadedBorrowLogs = DataStoreManager.loadBorrowLogs(context)
            _borrowLogs.value = loadedBorrowLogs
        }
    }

    fun setCurrentScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun addNickname(id: String, nickname: String) {
        val updated = _tagNicknames.value.toMutableMap()
        updated[id] = nickname
        _tagNicknames.value = updated
        viewModelScope.launch {
            DataStoreManager.saveNicknames(context, updated)
        }
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        _tagData.value = "Nickname: $nickname\nTag ID: $id\nTimestamp: $timestamp"
        _currentTagId.value = null
    }

    fun updateHistory(newHistory: List<String>) {
        _tagHistory.value = newHistory
        viewModelScope.launch {
            DataStoreManager.saveHistory(context, newHistory)
        }
    }

    fun clearNicknames() {
        _tagNicknames.value = emptyMap()
        viewModelScope.launch {
            DataStoreManager.saveNicknames(context, emptyMap())
        }
    }

    fun updateAllNicknames(updatedNicknamesMap: Map<String, String>) {
        _tagNicknames.value = updatedNicknamesMap
        viewModelScope.launch {
            DataStoreManager.saveNicknames(context, updatedNicknamesMap)
        }
    }

    fun onBorrow(tagId: String, itemName: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val newLog = BorrowLog(tagId, itemName, timestamp, false, null)
        val updatedLogs = _borrowLogs.value + newLog
        _borrowLogs.value = updatedLogs
        viewModelScope.launch {
            DataStoreManager.saveBorrowLogs(context, updatedLogs)
        }
        _scannedBorrowTagId.value = null
    }

    fun onReturn(log: BorrowLog) {
        val updatedLogs = _borrowLogs.value.map {
            if (it == log) it.copy(isReturned = true, returnTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())) else it
        }
        _borrowLogs.value = updatedLogs
        viewModelScope.launch {
            DataStoreManager.saveBorrowLogs(context, updatedLogs)
        }
    }

    fun setScannedBorrowTagId(uid: String?) {
        _scannedBorrowTagId.value = uid
    }

    fun setTagData(data: String) {
        _tagData.value = data
    }

    fun setCurrentTagId(id: String?) {
        _currentTagId.value = id
    }

    fun handleNfcAbsensi(uid: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val dateOnly = timestamp.substring(0, 10)
        val historyMap = mutableMapOf<String, String>()
        _tagHistory.value.forEach { entry ->
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
        _tagHistory.value = newHistory
        viewModelScope.launch {
            DataStoreManager.saveHistory(context, newHistory)
        }
        val nickname = _tagNicknames.value[uid]
        if (nickname != null) {
            _tagData.value = "Nickname: $nickname\nTag ID: $uid\nTimestamp: $timestamp"
            _currentTagId.value = null
        } else {
            _tagData.value = "Tag ID: $uid\nTimestamp: $timestamp"
            _currentTagId.value = uid
        }
    }
}

