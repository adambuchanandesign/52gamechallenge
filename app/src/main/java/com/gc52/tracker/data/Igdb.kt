package com.gc52.tracker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneOffset

object Igdb {

    data class Hit(
        val id: Long,
        val name: String,
        val year: Int?,
        val platforms: List<String>,
        val coverUrl: String?
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("gc52", 0)

    fun clientId(ctx: Context): String = prefs(ctx).getString("igdb_id", "") ?: ""
    fun clientSecret(ctx: Context): String = prefs(ctx).getString("igdb_secret", "") ?: ""
    fun enabled(ctx: Context): Boolean = clientId(ctx).isNotBlank() && clientSecret(ctx).isNotBlank()

    fun saveCreds(ctx: Context, id: String, secret: String) {
        prefs(ctx).edit()
            .putString("igdb_id", id.trim())
            .putString("igdb_secret", secret.trim())
            .remove("igdb_token").remove("igdb_token_exp")
            .apply()
    }

    /** Returns a valid bearer token, refreshing via Twitch if needed. Null on failure. */
    private fun token(ctx: Context): String? {
        val p = prefs(ctx)
        val cached = p.getString("igdb_token", null)
        val exp = p.getLong("igdb_token_exp", 0)
        if (cached != null && System.currentTimeMillis() < exp - 60_000) return cached
        val id = clientId(ctx); val secret = clientSecret(ctx)
        if (id.isBlank() || secret.isBlank()) return null
        return try {
            val url = URL("https://id.twitch.tv/oauth2/token" +
                    "?client_id=$id&client_secret=$secret&grant_type=client_credentials")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 8000; readTimeout = 8000
            }
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val tok = json.getString("access_token")
            val expiresIn = json.optLong("expires_in", 3600)
            p.edit().putString("igdb_token", tok)
                .putLong("igdb_token_exp", System.currentTimeMillis() + expiresIn * 1000)
                .apply()
            tok
        } catch (e: Exception) { null }
    }

    /** Search IGDB. Returns null on error (no creds / network / auth), empty list for no hits. */
    fun search(ctx: Context, query: String, limit: Int = 15): List<Hit>? {
        val tok = token(ctx) ?: return null
        return try {
            val url = URL("https://api.igdb.com/v4/games")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000; readTimeout = 8000
                doOutput = true
                setRequestProperty("Client-ID", clientId(ctx))
                setRequestProperty("Authorization", "Bearer $tok")
                setRequestProperty("Accept", "application/json")
            }
            val q = query.replace("\"", "")
            val body = "search \"$q\"; fields name, first_release_date, platforms.name, cover.image_id; limit $limit;"
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode != 200) return null
            val text = conn.inputStream.bufferedReader().readText()
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val year = if (o.has("first_release_date"))
                    Instant.ofEpochSecond(o.getLong("first_release_date"))
                        .atZone(ZoneOffset.UTC).year else null
                val plats = if (o.has("platforms")) {
                    val pa = o.getJSONArray("platforms")
                    (0 until pa.length()).mapNotNull { j -> pa.getJSONObject(j).optString("name").ifBlank { null } }
                } else emptyList()
                val cover = if (o.has("cover"))
                    o.getJSONObject("cover").optString("image_id").ifBlank { null }
                        ?.let { "https://images.igdb.com/igdb/image/upload/t_cover_small/$it.jpg" }
                else null
                Hit(o.getLong("id"), o.getString("name"), year, plats, cover)
            }
        } catch (e: Exception) { null }
    }

    data class Details(
        val name: String, val year: Int?, val coverBig: String?,
        val summary: String?, val genres: List<String>, val platforms: List<String>,
        val screenshots: List<String>, val rating: Double? = null
    )

    val GENRES = linkedMapOf(
        "Platformer" to 8, "Shooter" to 5, "RPG" to 12, "Racing" to 10, "Fighting" to 4,
        "Puzzle" to 9, "Adventure" to 31, "Strategy" to 15, "Sport" to 14, "Arcade" to 33,
        "Point & click" to 2, "Hack and slash" to 25, "Indie" to 32
    )
    val ERAS = linkedMapOf(
        "1980s" to (1980 to 1989), "1990s" to (1990 to 1999), "2000s" to (2000 to 2009),
        "2010s" to (2010 to 2019), "2020s" to (2020 to 2029)
    )

    private fun query(ctx: Context, body: String): JSONArray? {
        val tok = token(ctx) ?: return null
        return try {
            val url = URL("https://api.igdb.com/v4/games")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 8000; readTimeout = 8000; doOutput = true
                setRequestProperty("Client-ID", clientId(ctx))
                setRequestProperty("Authorization", "Bearer $tok")
                setRequestProperty("Accept", "application/json")
            }
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode != 200) null
            else JSONArray(conn.inputStream.bufferedReader().readText())
        } catch (e: Exception) { null }
    }

    private fun parseDetails(o: JSONObject): Details {
        val year = if (o.has("first_release_date"))
            Instant.ofEpochSecond(o.getLong("first_release_date")).atZone(ZoneOffset.UTC).year else null
        fun names(key: String): List<String> =
            if (o.has(key)) {
                val a = o.getJSONArray(key)
                (0 until a.length()).mapNotNull { a.getJSONObject(it).optString("name").ifBlank { null } }
            } else emptyList()
        val cover = if (o.has("cover"))
            o.getJSONObject("cover").optString("image_id").ifBlank { null }
                ?.let { "https://images.igdb.com/igdb/image/upload/t_cover_big/$it.jpg" } else null
        val shots = if (o.has("screenshots")) {
            val a = o.getJSONArray("screenshots")
            (0 until minOf(a.length(), 6)).mapNotNull {
                a.getJSONObject(it).optString("image_id").ifBlank { null }
                    ?.let { id -> "https://images.igdb.com/igdb/image/upload/t_screenshot_med/$id.jpg" }
            }
        } else emptyList()
        return Details(
            o.optString("name"), year, cover,
            o.optString("summary").ifBlank { null }, names("genres"), names("platforms"), shots,
            if (o.has("total_rating")) o.getDouble("total_rating") else null
        )
    }

    private const val DETAIL_FIELDS =
        "fields name, first_release_date, summary, genres.name, platforms.name, cover.image_id, screenshots.image_id, total_rating;"

    /** Best single match for a game name, with summary/genres/screenshots. */
    fun details(ctx: Context, name: String): Details? {
        val q = name.replace("\"", "")
        val arr = query(ctx, "search \"$q\"; $DETAIL_FIELDS limit 1;") ?: return null
        if (arr.length() == 0) return null
        return parseDetails(arr.getJSONObject(0))
    }

    /** Random reasonably-known game, optionally filtered by genre id and/or release decade. */
    fun randomPick(ctx: Context, genreId: Int?, era: Pair<Int, Int>?): Details? {
        val where = buildList {
            add("rating_count > 5"); add("cover != null")
            genreId?.let { add("genres = ($it)") }
            era?.let { (a, b) ->
                val from = java.time.LocalDate.of(a, 1, 1).toEpochDay() * 86400
                val to = java.time.LocalDate.of(b, 12, 31).toEpochDay() * 86400
                add("first_release_date > $from & first_release_date < $to")
            }
        }.joinToString(" & ")
        val offset = (0..300).random()
        var arr = query(ctx, "$DETAIL_FIELDS where $where; sort rating_count desc; limit 40; offset $offset;")
        if (arr == null || arr.length() == 0)
            arr = query(ctx, "$DETAIL_FIELDS where $where; sort rating_count desc; limit 40;") ?: return null
        if (arr.length() == 0) return null
        return parseDetails(arr.getJSONObject((0 until arr.length()).random()))
    }

    fun testConnection(ctx: Context): String {
        if (!enabled(ctx)) return "Enter both Client ID and Secret first"
        prefs(ctx).edit().remove("igdb_token").remove("igdb_token_exp").apply()
        val r = search(ctx, "zelda", 1)
        return when {
            r == null -> "Failed — check the credentials (and internet)"
            else -> "Connected! IGDB is ready."
        }
    }

    /** Best-effort IGDB platform name -> the user's platform naming. */
    fun mapPlatform(igdb: String): String = when (igdb) {
        "Super Nintendo Entertainment System" -> "Nintendo SNES"
        "Nintendo Entertainment System" -> "Nintendo NES"
        "Family Computer Disk System" -> "Nintendo Famicom Disk System"
        "Sega Mega Drive/Genesis" -> "Sega Mega Drive"
        "Sega Master System/Mark III" -> "Sega Master System"
        "Sega CD" -> "Sega Mega CD"
        "Sega 32X" -> "Sega Mega Drive 32X"
        "PlayStation" -> "Sony PlayStation"
        "PlayStation 2" -> "Sony PlayStation 2"
        "PlayStation Vita" -> "Sony PlayStation Vita"
        "Game Boy" -> "Nintendo Game Boy"
        "Game Boy Color" -> "Nintendo Game Boy Color"
        "Game Boy Advance" -> "Nintendo Game Boy Advance"
        "Nintendo GameCube" -> "Nintendo GameCube"
        "Wii" -> "Nintendo Wii"
        "Wii U" -> "Nintendo WiiU"
        "Nintendo Switch" -> "Nintendo Switch"
        "Nintendo 3DS" -> "Nintendo 3DS"
        "Nintendo DS" -> "Nintendo DS"
        "Virtual Boy" -> "Nintendo Virtual Boy"
        "Xbox" -> "Microsoft Xbox"
        "Xbox 360" -> "Xbox 360"
        "PC (Microsoft Windows)" -> "PC"
        "DOS" -> "DOS"
        "Amiga" -> "Commodore Amiga"
        "Amiga CD32" -> "Commodore Amiga CD32"
        "Amstrad CPC" -> "Amstrad CPC"
        "ZX Spectrum" -> "ZX Spectrum"
        "BBC Microcomputer System" -> "BBC Micro"
        "MSX" -> "MSX"
        "Sharp X68000" -> "X68000"
        "TurboGrafx-16/PC Engine" -> "PC Engine"
        "Turbografx-16/PC Engine CD" -> "PC Engine CD"
        "Dreamcast" -> "Sega Dreamcast"
        "Sega Saturn" -> "Sega Saturn"
        "Sega Game Gear" -> "Sega Game Gear"
        "Arcade" -> "Arcade"
        "Android" -> "Android"
        "WonderSwan" -> "WonderSwan"
        "3DO Interactive Multiplayer" -> "Panasonic 3DO"
        else -> igdb
    }
}
