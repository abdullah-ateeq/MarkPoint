package com.example.markpoint.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.markpoint.domain.MarkPointMode
import com.example.markpoint.domain.MarkPointResult
import com.example.markpoint.domain.MarkPosition
import com.example.markpoint.domain.MeasurementUnit
import com.example.markpoint.domain.SpacingStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class SavedMark(
    val index: Int,
    val positionMm: String,
    val isEndpoint: Boolean
)

/** A saved calculation. Start, end and spacing are kept exactly as entered, in [unit]. */
@Serializable
data class HistoryRecord(
    val id: Long,
    val timestamp: Long,
    val mode: MarkPointMode,
    val start: String,
    val end: String,
    val unit: MeasurementUnit,
    val markCount: Int,
    val spacing: String,
    val strategy: SpacingStrategy,
    val addEndpoint: Boolean,
    val intervalMm: String?,
    val marks: List<SavedMark>,
    val completeIntervals: Int? = null,
    val remainingMm: String? = null
) {
    val spacedMarkCount: Int get() = marks.count { !it.isEndpoint }

    companion object {
        fun from(
            result: MarkPointResult,
            start: String,
            end: String,
            unit: MeasurementUnit,
            markCount: Int,
            spacing: String,
            addEndpoint: Boolean,
            timestamp: Long
        ) = HistoryRecord(
            id = timestamp,
            timestamp = timestamp,
            mode = result.mode,
            start = start,
            end = end,
            unit = unit,
            markCount = markCount,
            spacing = spacing,
            strategy = result.strategy ?: SpacingStrategy.EXACT_ENDPOINT,
            addEndpoint = addEndpoint,
            intervalMm = result.intervalMm?.toPlainString(),
            marks = result.marks.map { SavedMark(it.index, it.positionMm.toPlainString(), it.isEndpoint) },
            completeIntervals = result.completeIntervals,
            remainingMm = result.remainingDistanceMm?.toPlainString()
        )
    }
}

fun SavedMark.toMarkPosition(): MarkPosition? =
    positionMm.toBigDecimalOrNull()?.let { MarkPosition(index, it, isEndpoint) }

object HistoryCodec {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(HistoryRecord.serializer())

    fun encode(records: List<HistoryRecord>): String = json.encodeToString(serializer, records)

    /** Corrupt or incompatible data yields an empty history instead of a crash. */
    fun decode(text: String?): List<HistoryRecord> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(serializer, text)
        } catch (_: Exception) {
            emptyList()
        }
    }
}

class HistoryRepository(private val dataStore: DataStore<Preferences>) {

    private val key = stringPreferencesKey("history_json")

    /** Newest first. */
    val history: Flow<List<HistoryRecord>> = dataStore.data.map { HistoryCodec.decode(it[key]) }

    suspend fun add(record: HistoryRecord) {
        dataStore.edit { prefs ->
            val updated = (listOf(record) + HistoryCodec.decode(prefs[key])).take(MAX_RECORDS)
            prefs[key] = HistoryCodec.encode(updated)
        }
    }

    suspend fun delete(id: Long) {
        dataStore.edit { prefs ->
            prefs[key] = HistoryCodec.encode(HistoryCodec.decode(prefs[key]).filterNot { it.id == id })
        }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }

    companion object {
        const val MAX_RECORDS = 50
    }
}
