package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CounterSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MainApplication).repository

    val previousSessions: StateFlow<List<CounterSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessions = repository.allSessions.first()
                val jsonArray = org.json.JSONArray()
                for (session in sessions) {
                    val jsonObj = org.json.JSONObject().apply {
                        put("timestamp", session.timestamp)
                        put("checkCount", session.checkCount)
                        put("crossCount", session.crossCount)
                        put("totalCheckTimeMs", session.totalCheckTimeMs)
                        put("totalCrossTimeMs", session.totalCrossTimeMs)
                    }
                    jsonArray.put(jsonObj)
                }
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(jsonArray.toString().toByteArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSession(session: CounterSession) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSession(session)
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    val jsonArray = org.json.JSONArray(text)
                    for (i in 0 until jsonArray.length()) {
                        val jsonObj = jsonArray.getJSONObject(i)
                        val session = CounterSession(
                            timestamp = jsonObj.getLong("timestamp"),
                            checkCount = jsonObj.getInt("checkCount"),
                            crossCount = jsonObj.getInt("crossCount"),
                            totalCheckTimeMs = jsonObj.optLong("totalCheckTimeMs", 0L),
                            totalCrossTimeMs = jsonObj.optLong("totalCrossTimeMs", 0L)
                        )
                        repository.insertSession(session)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
