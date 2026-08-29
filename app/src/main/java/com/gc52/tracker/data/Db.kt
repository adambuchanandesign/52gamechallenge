package com.gc52.tracker.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val year: Int,
    val seq: Int,                 // 1..N within the year; shown as "seq/52"
    val name: String,
    val platform: String,
    val date: String?,            // "yyyy-MM-dd HH:mm" or "yyyy-MM-dd" or null
    val imageFile: String?,       // filename inside <folder>/<year>/
    val notes: String?,
    val replay: Boolean = false,
    val normName: String,         // normalized for search/dup detection
    // IGDB enrichment (nullable = not yet fetched; igdbGenres "-" = looked up, no match)
    val igdbYear: Int? = null,
    val igdbGenres: String? = null,   // "|"-separated
    val igdbCover: String? = null,
    val igdbRating: Double? = null,
    val igdbSummary: String? = null,
    val igdbId: Long? = null
)

@Entity(tableName = "ideas")
data class Idea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val done: Boolean = false
)

@Entity(tableName = "backlog")
data class Backlog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val platform: String,
    val added: String?,
    val notes: String?,
    val coverUrl: String? = null,
    val igdbId: Long? = null
)

@Entity(tableName = "playing")
data class Playing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val platform: String,
    val started: String?,
    val notes: String?,
    val coverUrl: String? = null,
    val igdbId: Long? = null
)

data class PlatformCount(val platform: String, val n: Int)
data class YearCount(val year: Int, val n: Int)

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY year DESC, seq DESC")
    fun newestFirst(): Flow<List<Game>>

    @Query("SELECT * FROM games ORDER BY name COLLATE NOCASE ASC")
    fun alphabetical(): Flow<List<Game>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun byId(id: Long): Game?

    @Query("SELECT * FROM games")
    suspend fun allOnce(): List<Game>

    @Query("SELECT COUNT(*) FROM games")
    fun total(): Flow<Int>

    @Query("SELECT COUNT(*) FROM games WHERE year = :year")
    fun countForYear(year: Int): Flow<Int>

    @Query("SELECT platform AS platform, COUNT(*) AS n FROM games GROUP BY platform ORDER BY n DESC")
    fun platformCounts(): Flow<List<PlatformCount>>

    @Query("SELECT year AS year, COUNT(*) AS n FROM games GROUP BY year ORDER BY year ASC")
    fun yearCounts(): Flow<List<YearCount>>

    @Query("SELECT DISTINCT platform FROM games ORDER BY platform COLLATE NOCASE ASC")
    fun platforms(): Flow<List<String>>

    @Query("SELECT DISTINCT year FROM games ORDER BY year DESC")
    fun years(): Flow<List<Int>>

    @Query("SELECT COALESCE(MAX(seq), 0) FROM games WHERE year = :year")
    suspend fun maxSeq(year: Int): Int

    @Query("SELECT * FROM games WHERE normName LIKE '%' || :norm || '%' ORDER BY year DESC, seq DESC")
    suspend fun searchNorm(norm: String): List<Game>

    @Insert suspend fun insert(g: Game): Long
    @Insert suspend fun insertAll(gs: List<Game>)
    @Update suspend fun update(g: Game)
    @Query("SELECT * FROM games WHERE igdbGenres IS NULL ORDER BY year, seq")
    suspend fun unenriched(): List<Game>
    @Query("DELETE FROM playing") suspend fun clearPlaying()
    @Query("DELETE FROM backlog") suspend fun clearBacklog()
    @Insert suspend fun insertGames(gs: List<Game>)
    @Insert suspend fun insertPlayingAll(ps: List<Playing>)
    @Insert suspend fun insertBacklogAll(bs: List<Backlog>)
    @Delete suspend fun delete(g: Game)
    @Query("DELETE FROM games") suspend fun clearGames()

    @Query("SELECT * FROM ideas ORDER BY done ASC, id ASC")
    fun ideas(): Flow<List<Idea>>
    @Insert suspend fun insertIdea(i: Idea)
    @Insert suspend fun insertIdeas(list: List<Idea>)
    @Update suspend fun updateIdea(i: Idea)
    @Delete suspend fun deleteIdea(i: Idea)
    @Query("DELETE FROM ideas") suspend fun clearIdeas()

    @Query("SELECT * FROM playing ORDER BY id DESC")
    fun playing(): Flow<List<Playing>>
    @Query("SELECT * FROM playing WHERE id = :id")
    suspend fun playingById(id: Long): Playing?
    @Insert suspend fun insertPlaying(p: Playing): Long
    @Update suspend fun updatePlaying(p: Playing)
    @Delete suspend fun deletePlaying(p: Playing)
    @Query("DELETE FROM playing WHERE id = :id") suspend fun deletePlayingById(id: Long)

    @Query("SELECT * FROM backlog ORDER BY id DESC")
    fun backlog(): Flow<List<Backlog>>
    @Query("SELECT * FROM backlog WHERE id = :id")
    suspend fun backlogById(id: Long): Backlog?
    @Insert suspend fun insertBacklog(b: Backlog): Long
    @Update suspend fun updateBacklog(b: Backlog)
    @Delete suspend fun deleteBacklog(b: Backlog)
    @Query("DELETE FROM backlog WHERE id = :id") suspend fun deleteBacklogById(id: Long)
}

@Database(entities = [Game::class, Idea::class, Playing::class, Backlog::class], version = 6, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): GameDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "gc52.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build().also { inst = it }
        }
    }
}

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `playing` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `platform` TEXT NOT NULL, `started` TEXT, `notes` TEXT)")
    }
}

val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `playing` ADD COLUMN `coverUrl` TEXT")
    }
}

val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `backlog` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `platform` TEXT NOT NULL, `added` TEXT, `notes` TEXT, `coverUrl` TEXT)")
    }
}

val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `games` ADD COLUMN `igdbYear` INTEGER")
        db.execSQL("ALTER TABLE `games` ADD COLUMN `igdbGenres` TEXT")
        db.execSQL("ALTER TABLE `games` ADD COLUMN `igdbCover` TEXT")
        db.execSQL("ALTER TABLE `games` ADD COLUMN `igdbRating` REAL")
        db.execSQL("ALTER TABLE `games` ADD COLUMN `igdbSummary` TEXT")
    }
}

val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `games` ADD COLUMN `igdbId` INTEGER")
        db.execSQL("ALTER TABLE `playing` ADD COLUMN `igdbId` INTEGER")
        db.execSQL("ALTER TABLE `backlog` ADD COLUMN `igdbId` INTEGER")
    }
}

/** Normalization used for search + duplicate detection. */
fun normalizeTitle(s: String): String {
    var t = s.lowercase().trim()
    t = t.replace(Regex("[^a-z0-9 ]"), "")
    t = t.replace(Regex("\\s+"), " ").trim()
    if (t.startsWith("the ")) t = t.removePrefix("the ")
    return t
}
