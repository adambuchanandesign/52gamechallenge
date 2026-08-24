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
enum class ViewMode { LIST, GRID, LARGE }

data class Filters(
    val sort: SortMode = SortMode.NEWEST,
    val year: Int? = null,
    val platform: String? = null,
    val view: ViewMode = ViewMode.GRID
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

    // ---- IGDB live search ----
    sealed class IgdbUi {
        object Off : IgdbUi()
        object Idle : IgdbUi()
        object Loading : IgdbUi()
        data class Loaded(val query: String, val hits: List<Igdb.Hit>) : IgdbUi()
        object Error : IgdbUi()
    }
    val igdbEnabled = MutableStateFlow(Igdb.enabled(app))
    val igdbUi = MutableStateFlow<IgdbUi>(if (Igdb.enabled(app)) IgdbUi.Idle else IgdbUi.Off)
    var lastIgdbQuery: String = ""
        private set
    var lastIgdbHits: List<Igdb.Hit> = emptyList()
        private set

    init {
        viewModelScope.launch {
            query.map { it.trim() }.debounce(350).collectLatest { q ->
                if (!igdbEnabled.value) { igdbUi.value = IgdbUi.Off; return@collectLatest }
                if (q.length < 2) { igdbUi.value = IgdbUi.Idle; return@collectLatest }
                igdbUi.value = IgdbUi.Loading
                val hits = kotlinx.coroutines.withContext(Dispatchers.IO) { Igdb.search(getApplication(), q) }
                if (hits == null) igdbUi.value = IgdbUi.Error
                else {
                    lastIgdbQuery = q; lastIgdbHits = hits
                    igdbUi.value = IgdbUi.Loaded(q, hits)
                }
            }
        }
    }

    val igdbTestStatus = MutableStateFlow<String?>(null)
    fun saveIgdbCreds(id: String, secret: String) {
        Igdb.saveCreds(getApplication(), id, secret)
        igdbEnabled.value = Igdb.enabled(getApplication())
        if (!igdbEnabled.value) igdbUi.value = IgdbUi.Off else igdbUi.value = IgdbUi.Idle
        igdbTestStatus.value = "Saved"
    }
    fun testIgdb() {
        viewModelScope.launch(Dispatchers.IO) {
            igdbTestStatus.value = "Testing…"
            igdbTestStatus.value = Igdb.testConnection(getApplication())
        }
    }

    // ---- now playing ----
    val playing = dao.playing().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    data class PendingNp(val name: String, val platforms: List<String>, val coverUrl: String?,
                         val target: String = "playing")  // "playing" | "backlog"
    var pendingNp: PendingNp? = null

    fun addPlaying(name: String, platform: String, notes: String?, coverUrl: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlaying(Playing(name = name.trim(), platform = platform.trim(),
                started = java.time.LocalDate.now().toString(), notes = notes?.ifBlank { null },
                coverUrl = coverUrl))
        }
    }
    fun updatePlaying(p: Playing) { viewModelScope.launch(Dispatchers.IO) { dao.updatePlaying(p) } }

    // ---- backlog ----
    val backlog = dao.backlog().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    fun addBacklog(name: String, platform: String, notes: String?, coverUrl: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertBacklog(Backlog(name = name.trim(), platform = platform.trim(),
                added = java.time.LocalDate.now().toString(), notes = notes?.ifBlank { null },
                coverUrl = coverUrl))
        }
    }
    fun updateBacklog(b: Backlog) { viewModelScope.launch(Dispatchers.IO) { dao.updateBacklog(b) } }
    fun removeBacklog(b: Backlog) { viewModelScope.launch(Dispatchers.IO) { dao.deleteBacklog(b) } }
    suspend fun backlogItem(id: Long): Backlog? = dao.backlogById(id)
    fun consumeBacklog(id: Long) { viewModelScope.launch(Dispatchers.IO) { dao.deleteBacklogById(id) } }
    /** Promote a backlog entry into Now playing. */
    fun startPlayingFromBacklog(b: Backlog) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlaying(Playing(name = b.name, platform = b.platform,
                started = java.time.LocalDate.now().toString(), notes = b.notes, coverUrl = b.coverUrl))
            dao.deleteBacklog(b)
        }
    }

    // ---- history: on this week / month in past years ----
    private fun weekOf(d: java.time.LocalDate) =
        d.get(java.time.temporal.WeekFields.of(java.util.Locale.UK).weekOfWeekBasedYear())
    val onThisWeek: StateFlow<List<Game>> = dao.newestFirst().map { all ->
        val now = java.time.LocalDate.now()
        val w = weekOf(now)
        all.filter { g ->
            g.year != now.year && g.date != null &&
                runCatching { java.time.LocalDate.parse(g.date.take(10)) }.getOrNull()
                    ?.let { weekOf(it) == w } == true
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val onThisMonth: StateFlow<List<Game>> = dao.newestFirst().map { all ->
        val now = java.time.LocalDate.now()
        all.filter { g ->
            g.year != now.year && g.date != null &&
                g.date.take(7).endsWith("-%02d".format(now.monthValue))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- IGDB details cache (game detail page block) ----
    private val detailsCache = HashMap<String, Igdb.Details?>()
    suspend fun igdbDetails(name: String): Igdb.Details? {
        if (!igdbEnabled.value) return null
        detailsCache[name]?.let { return it }
        if (detailsCache.containsKey(name)) return null
        val d = kotlinx.coroutines.withContext(Dispatchers.IO) { Igdb.details(getApplication(), name) }
        detailsCache[name] = d
        return d
    }

    // ---- random picker ----
    suspend fun randomGame(genreId: Int?, era: Pair<Int, Int>?): Igdb.Details? =
        kotlinx.coroutines.withContext(Dispatchers.IO) { Igdb.randomPick(getApplication(), genreId, era) }
    suspend fun beatenMatch(name: String): Game? {
        val n = normalizeTitle(name)
        return dao.allOnce().firstOrNull { it.normName == n }
    }
    fun removePlaying(p: Playing) { viewModelScope.launch(Dispatchers.IO) { dao.deletePlaying(p) } }
    suspend fun playingItem(id: Long): Playing? = dao.playingById(id)
    fun consumePlaying(id: Long) { viewModelScope.launch(Dispatchers.IO) { dao.deletePlayingById(id) } }

    // ---- series (stored in prefs) ----
    private val prefs = app.getSharedPreferences("gc52", 0)
    val series = MutableStateFlow(loadSeries())
    private fun loadSeries(): List<String> =
        prefs.getStringSet("series", setOf("Sonic", "Mario", "Zelda", "Final Fantasy", "Castlevania",
            "Mega Man", "Metroid", "Kirby", "Pokemon", "StarFox"))!!.sorted()
    fun addSeries(name: String) {
        val set = loadSeries().toMutableSet(); set.add(name.trim())
        prefs.edit().putStringSet("series", set).apply(); series.value = set.sorted()
    }
    fun removeSeries(name: String) {
        val set = loadSeries().toMutableSet(); set.remove(name)
        prefs.edit().putStringSet("series", set).apply(); series.value = set.sorted()
    }
    suspend fun seriesCounts(): List<Pair<String, Int>> {
        val all = dao.allOnce()
        return series.value.map { s ->
            val n = normalizeTitle(s)
            s to all.count { it.normName.contains(n) }
        }.sortedByDescending { it.second }
    }
    suspend fun allGamesOnce(): List<Game> = dao.allOnce()

    // ---- collage save ----
    fun saveCollage(g: Game, bmp: android.graphics.Bitmap, onDone: (Game?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            g.imageFile?.let { Storage.archiveImage(getApplication(), g.year, it) }
            val fn = Storage.collageFileName(g.year, g.seq, g.name, g.platform, "jpg")
            if (Storage.saveBitmapIntoYear(getApplication(), bmp, g.year, fn)) {
                val updated = g.copy(imageFile = fn)
                dao.update(updated); onDone(updated)
            } else onDone(null)
        }
    }

    // ---- export ----
    val exportStatus = MutableStateFlow<String?>(null)
    fun runExport() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
                val all = dao.allOnce().sortedWith(compareBy({ it.year }, { it.seq }))
                fun q(v: String?): String {
                    val t = v ?: ""
                    return if (t.contains(',') || t.contains('"'))
                        "\"" + t.replace("\"", "\"\"") + "\"" else t
                }
                val csv = buildString {
                    append("Year,Number,Name,Console,Date,ImageFile,Notes,Replay\n")
                    all.forEach { g ->
                        append("${g.year},${g.seq}/52,${q(g.name)},${q(g.platform)},${q(g.date)},${q(g.imageFile)},${q(g.notes)},${if (g.replay) "yes" else ""}\n")
                    }
                }
                val okCsv = Storage.writeExport(getApplication(), "52gc-export-$stamp.csv", "text/csv", csv)
                exportStatus.value = if (okCsv)
                    "Exported ${all.size} games to exports/52gc-export-$stamp.csv"
                else "Export failed — check the data folder in Settings"
            } catch (e: Exception) { exportStatus.value = "Export failed: ${e.message}" }
        }
    }

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
