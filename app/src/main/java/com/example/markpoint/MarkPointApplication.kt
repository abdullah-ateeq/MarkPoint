package com.example.markpoint

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.markpoint.data.HistoryRepository
import com.example.markpoint.data.SettingsRepository

private val Context.markPointDataStore: DataStore<Preferences> by preferencesDataStore(name = "markpoint")

class MarkPointApplication : Application() {
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(markPointDataStore) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(markPointDataStore) }
}
