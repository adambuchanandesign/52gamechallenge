@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)

package com.gc52.tracker

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gc52.tracker.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

enum class SortMode { NEWEST, OLDEST, AZ }
enum class ViewMode { LIST, GRID }

data class Filters(
    val sort: SortMode = SortMode.NEWEST,
    val year: Int? = null,
    val platform: String? = null,
    val view: ViewMode = ViewMode.LIST
)

data class Pace(val count: Int, val week: Int, val diff: Int)  // diff = count - week

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()

    val filters = MutableStateFlow(Filters())
    val total = dao.total().stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val platformCounts = dao.platformCounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val yearCounts = dao.yearCounts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val years = dao.years().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val platforms = dao.platforms().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val games: StateFlow<List<Game>> =
        combine(dao.newestFirst(), filters) { all, f ->
            var list = all
            f.year?.let { y -> list = list.filter { it.year == y } }
            f.platform?.let { p -> list = list.filter { it.platform == p } }
            when (f.sort) {
                SortMode.NEWEST -> list
                SortMode.OLDEST -> list.reversed()
                SortMode.AZ -> list.sortedBy { it.name.lowercase() }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pace: StateFlow<Pace> = dao.countForYear(LocalDate.now().year)
        .map { c ->
            val week = LocalDate.now().get(WeekFields.of(Locale.UK).weekOfWeekBasedYear())
            Pace(c, week, c - week)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Pace(0, 1, 0))

    // ---- search ----
    val query = MutableStateFlow("")
    val searchResults: StateFlow<List<Game>> = query
        .map { it.trim() }
        .debounce(150)
        .mapLatest { q ->
            if (q.length < 2) emptyList()
            else dao.searchNorm(normalizeTitle(q))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    suspend fun duplicatesOf(name: String): List<Game> {
        val n = normalizeTitle(name)
        if (n.isBlank()) return emptyList()
        return dao.searchNorm(n)
    }

    fun addGame(name: String, platform: String, year: Int, date: String?, notes: String?,
                replay: Boolean, pickedImage: Uri?, onDone: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val seq = dao.maxSeq(year) + 1
            var imageFile: String? = null
            if (pickedImage != null) {
                val ext = Storage.extFor(getApplication(), pickedImage)
                val fn = Storage.collageFileName(year, seq, name.trim(), platform.trim(), ext)
                if (Storage.copyIntoYear(getApplication(), pickedImage, year, fn)) imageFile = fn
            }
            val id = dao.insert(
                Game(year = year, seq = seq, name = name.trim(), platform = platform.trim(),
                    date = date, imageFile = imageFile, notes = notes?.ifBlank { null },
                    replay = replay, normName = normalizeTitle(name))
            )
            onDone(id)
        }
    }

    /** Archives the current image (if any) and installs the picked one with the proper name. */
    fun replaceImage(g: Game, picked: Uri, onDone: (Game?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            g.imageFile?.let { Storage.archiveImage(getApplication(), g.year, it) }
            val ext = Storage.extFor(getApplication(), picked)
            val fn = Storage.collageFileName(g.year, g.seq, g.name, g.platform, ext)
            if (Storage.copyIntoYear(getApplication(), picked, g.year, fn)) {
                val updated = g.copy(imageFile = fn)
                dao.update(updated)
                onDone(updated)
            } else onDone(null)
        }
    }

    fun update(g: Game, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(g.copy(normName = normalizeTitle(g.name)))
            onDone()
        }
    }

    fun delete(g: Game, onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) { dao.delete(g); onDone() }
    }

    suspend fun game(id: Long): Game? = dao.byId(id)

    // ---- import ----
    val importStatus = MutableStateFlow<String?>(null)
    fun runImport(csvUri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = csvUri ?: Storage.findCsv(getApplication())
                if (uri == null) { importStatus.value = "52gc-import.csv not found in the chosen folder"; return@launch }
                val n = Importer.importCsv(getApplication(), dao, uri)
                importStatus.value = if (n > 0) "Imported $n games" else "No rows found in CSV"
            } catch (e: Exception) {
                importStatus.value = "Import failed: ${e.message}"
            }
        }
    }
}
