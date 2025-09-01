package com.example.rfidexample

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rfidexample.data.DataStoreManager
import com.example.rfidexample.data.model.BorrowLog
import com.example.rfidexample.data.repository.BorrowLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext

    private val _tagData = MutableStateFlow("Scan a tag")
    val tagData: StateFlow<String> = _tagData.asStateFlow()

    private val _currentTagId = MutableStateFlow<String?>(null)
    val currentTagId: StateFlow<String?> = _currentTagId.asStateFlow()

    private val _tagHistory = MutableStateFlow<List<String>>(emptyList())
    val tagHistory: StateFlow<List<String>> = _tagHistory.asStateFlow()

    private val _tagNicknames = MutableStateFlow<Map<String, String>>(emptyMap())
    val tagNicknames: StateFlow<Map<String, String>> = _tagNicknames.asStateFlow()

    private val _borrowLogs = MutableStateFlow<List<BorrowLog>>(emptyList())
    val borrowLogs: StateFlow<List<BorrowLog>> = _borrowLogs.asStateFlow()

    private val _scannedBorrowTagId = MutableStateFlow<String?>(null)
    val scannedBorrowTagId: StateFlow<String?> = _scannedBorrowTagId.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _tagNicknames.value = DataStoreManager.loadNicknames(context)
            _tagHistory.value = DataStoreManager.loadHistory(context)
            val loadedLogs = DataStoreManager.loadBorrowLogs(context)
            _borrowLogs.value = loadedLogs
            // Initialize repository with loaded data
            BorrowLogRepository.clear()
            loadedLogs.forEach { BorrowLogRepository.addLog(it) }
        }
    }

    fun addNickname(id: String, nickname: String) {
        val updatedNicknames = _tagNicknames.value.toMutableMap().apply { this[id] = nickname }
        _tagNicknames.value = updatedNicknames
        viewModelScope.launch {
            DataStoreManager.saveNicknames(context, updatedNicknames)
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
        val newLog = BorrowLog(tagId, itemName, System.currentTimeMillis())
        BorrowLogRepository.addLog(newLog)
        val updatedLogs = BorrowLogRepository.getLogs()
        _borrowLogs.value = updatedLogs
        viewModelScope.launch {
            DataStoreManager.saveBorrowLogs(context, updatedLogs)
        }
        _scannedBorrowTagId.value = null
    }

    fun onReturn(logToReturn: BorrowLog) {
        BorrowLogRepository.recordReturn(logToReturn)
        val updatedLogs = BorrowLogRepository.getLogs()
        _borrowLogs.value = updatedLogs
        viewModelScope.launch {
            DataStoreManager.saveBorrowLogs(context, updatedLogs)
        }
    }

    fun onClearBorrowHistory() {
        BorrowLogRepository.clear()
        _borrowLogs.value = emptyList()
        viewModelScope.launch {
            DataStoreManager.saveBorrowLogs(context, emptyList())
        }
    }

    fun onDeleteBorrowLog(logToDelete: BorrowLog) {
        BorrowLogRepository.deleteLog(logToDelete)
        val updatedLogs = BorrowLogRepository.getLogs()
        _borrowLogs.value = updatedLogs
        viewModelScope.launch {
            DataStoreManager.saveBorrowLogs(context, updatedLogs)
        }
    }

    fun handleNfcTag(uid: String, currentRoute: String?) {
        when (currentRoute) {
            "borrow" -> _scannedBorrowTagId.value = uid
            "attendance" -> processAttendance(uid)
        }
    }

    private fun processAttendance(uid: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val dateOnly = timestamp.substring(0, 10)

        val historyMap = _tagHistory.value.associate {
            val parts = it.split(",")
            val entryUid = parts[0]
            val entryDate = parts[1].substring(0, 10)
            "$entryUid|$entryDate" to it
        }.toMutableMap()

        historyMap["$uid|$dateOnly"] = "$uid,$timestamp"

        _tagHistory.value = historyMap.values.sortedByDescending { it.split(",")[1] }

        viewModelScope.launch {
            DataStoreManager.saveHistory(context, _tagHistory.value)
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