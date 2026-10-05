package com.example.markpoint.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.markpoint.domain.MarkOrientation
import com.example.markpoint.domain.MarkPointCalculator
import com.example.markpoint.domain.MarkPointMode
import com.example.markpoint.domain.MeasurementUnit
import com.example.markpoint.domain.SpacingStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files

class PersistenceTest {

    private lateinit var dir: File
    private val scopes = mutableListOf<CoroutineScope>()

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("markpoint-test").toFile()
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        dir.deleteRecursively()
    }

    private fun newDataStore(): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(scope = scope) { File(dir, "markpoint.preferences_pb") }
    }

    private fun record(id: Long): HistoryRecord {
        val result = MarkPointCalculator.calculateFixedSpacing(BigDecimal.ZERO, BigDecimal("1000"), BigDecimal("300"), addEndpoint = true)
        return HistoryRecord.from(result, "0", "100", MeasurementUnit.CENTIMETER, 3, "30", addEndpoint = true, timestamp = id)
    }

    @Test
    fun historySerializationRoundTrip() {
        val original = listOf(record(1), record(2))
        val decoded = HistoryCodec.decode(HistoryCodec.encode(original))
        assertEquals(original, decoded)
        val restored = decoded.first()
        assertEquals(MarkPointMode.SPACING, restored.mode)
        assertEquals(SpacingStrategy.FIXED_SPACING, restored.strategy)
        assertEquals(3, restored.spacedMarkCount)
        assertTrue(restored.marks.last().isEndpoint)
        assertEquals(0, BigDecimal(restored.remainingMm).compareTo(BigDecimal("100")))
    }

    @Test
    fun corruptHistoryDecodesToEmpty() {
        assertEquals(emptyList<HistoryRecord>(), HistoryCodec.decode("{not json"))
        assertEquals(emptyList<HistoryRecord>(), HistoryCodec.decode(null))
    }

    @Test
    fun historyKeepsNewestFiftyAndSupportsDelete() = runBlocking {
        val repository = HistoryRepository(newDataStore())
        (1L..51L).forEach { repository.add(record(it)) }
        val history = repository.history.first()
        assertEquals(HistoryRepository.MAX_RECORDS, history.size)
        assertEquals(51L, history.first().id)
        assertEquals(2L, history.last().id) // record #1 (oldest) was removed

        repository.delete(51L)
        assertEquals(50L, repository.history.first().first().id)
        repository.clear()
        assertTrue(repository.history.first().isEmpty())
    }

    @Test
    fun settingsPersistence() = runBlocking {
        val repository = SettingsRepository(newDataStore())
        assertEquals(AppSettings(), repository.settings.first())

        repository.update {
            it.copy(
                themeMode = ThemeMode.DARK,
                defaultMode = MarkPointMode.SPACING,
                defaultUnit = MeasurementUnit.INCH,
                defaultStart = "5",
                defaultEnd = "-20",
                defaultMarks = 8,
                defaultSpacing = "2.5",
                decimalPlaces = 3,
                orientation = MarkOrientation.VERTICAL,
                showSteps = false,
                haptics = false
            )
        }
        val saved = repository.settings.first()
        assertEquals(ThemeMode.DARK, saved.themeMode)
        assertEquals(MeasurementUnit.INCH, saved.defaultUnit)
        assertEquals("-20", saved.defaultEnd)
        assertEquals(8, saved.defaultMarks)
        assertEquals(3, saved.decimalPlaces)
        assertEquals(MarkOrientation.VERTICAL, saved.orientation)

        repository.reset()
        assertEquals(AppSettings(), repository.settings.first())
    }
}
