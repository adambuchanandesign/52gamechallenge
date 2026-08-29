package com.gc52.tracker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Backup {

    const val VERSION = 1

    // ---------- JSON backup ----------

    fun buildJson(
        games: List<Game>, playing: List<Playing>, backlog: List<Backlog>,
        series: List<String>
    ): String {
        val root = JSONObject()
        root.put("format", "52gc-backup")
        root.put("version", VERSION)
        root.put("exportedAt", java.time.LocalDateTime.now().toString())
        root.put("games", JSONArray().apply {
            games.forEach { g ->
                put(JSONObject().apply {
                    put("year", g.year); put("seq", g.seq); put("name", g.name)
                    put("platform", g.platform); putOpt("date", g.date)
                    putOpt("imageFile", g.imageFile); putOpt("notes", g.notes)
                    put("replay", g.replay)
                    putOpt("igdbYear", g.igdbYear); putOpt("igdbGenres", g.igdbGenres)
                    putOpt("igdbCover", g.igdbCover); putOpt("igdbRating", g.igdbRating)
                    putOpt("igdbSummary", g.igdbSummary)
                    putOpt("igdbId", g.igdbId)
                })
            }
        })
        root.put("playing", JSONArray().apply {
            playing.forEach { p ->
                put(JSONObject().apply {
                    put("name", p.name); put("platform", p.platform)
                    putOpt("started", p.started); putOpt("notes", p.notes)
                    putOpt("coverUrl", p.coverUrl); putOpt("igdbId", p.igdbId)
                })
            }
        })
        root.put("backlog", JSONArray().apply {
            backlog.forEach { b ->
                put(JSONObject().apply {
                    put("name", b.name); put("platform", b.platform)
                    putOpt("added", b.added); putOpt("notes", b.notes)
                    putOpt("coverUrl", b.coverUrl); putOpt("igdbId", b.igdbId)
                })
            }
        })
        root.put("series", JSONArray(series))
        return root.toString(1)
    }

    data class Parsed(
        val games: List<Game>, val playing: List<Playing>, val backlog: List<Backlog>,
        val series: List<String>, val exportedAt: String?
    )

    fun parseJson(text: String): Parsed? {
        return try {
            val root = JSONObject(text)
            if (root.optString("format") != "52gc-backup") return null
            fun JSONObject.strOrNull(k: String): String? =
                if (has(k) && !isNull(k)) getString(k) else null
            val games = ArrayList<Game>()
            val ga = root.optJSONArray("games") ?: JSONArray()
            for (i in 0 until ga.length()) {
                val o = ga.getJSONObject(i)
                val name = o.getString("name")
                games.add(Game(
                    year = o.getInt("year"), seq = o.getInt("seq"), name = name,
                    platform = o.getString("platform"), date = o.strOrNull("date"),
                    imageFile = o.strOrNull("imageFile"), notes = o.strOrNull("notes"),
                    replay = o.optBoolean("replay", false), normName = normalizeTitle(name),
                    igdbYear = if (o.has("igdbYear") && !o.isNull("igdbYear")) o.getInt("igdbYear") else null,
                    igdbGenres = o.strOrNull("igdbGenres"), igdbCover = o.strOrNull("igdbCover"),
                    igdbRating = if (o.has("igdbRating") && !o.isNull("igdbRating")) o.getDouble("igdbRating") else null,
                    igdbSummary = o.strOrNull("igdbSummary"),
                    igdbId = if (o.has("igdbId") && !o.isNull("igdbId")) o.getLong("igdbId") else null
                ))
            }
            val playing = ArrayList<Playing>()
            val pa = root.optJSONArray("playing") ?: JSONArray()
            for (i in 0 until pa.length()) {
                val o = pa.getJSONObject(i)
                playing.add(Playing(name = o.getString("name"), platform = o.getString("platform"),
                    started = o.strOrNull("started"), notes = o.strOrNull("notes"),
                    coverUrl = o.strOrNull("coverUrl"),
                    igdbId = if (o.has("igdbId") && !o.isNull("igdbId")) o.getLong("igdbId") else null))
            }
            val backlog = ArrayList<Backlog>()
            val ba = root.optJSONArray("backlog") ?: JSONArray()
            for (i in 0 until ba.length()) {
                val o = ba.getJSONObject(i)
                backlog.add(Backlog(name = o.getString("name"), platform = o.getString("platform"),
                    added = o.strOrNull("added"), notes = o.strOrNull("notes"),
                    coverUrl = o.strOrNull("coverUrl"),
                    igdbId = if (o.has("igdbId") && !o.isNull("igdbId")) o.getLong("igdbId") else null))
            }
            val series = ArrayList<String>()
            val sa = root.optJSONArray("series") ?: JSONArray()
            for (i in 0 until sa.length()) series.add(sa.getString(i))
            Parsed(games, playing, backlog, series, root.optString("exportedAt").ifBlank { null })
        } catch (e: Exception) { null }
    }

    // ---------- xlsx writer (hand-rolled: an xlsx is a zip of XML parts) ----------

    private fun esc(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun sheetXml(rows: List<List<Any?>>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
        rows.forEachIndexed { r, row ->
            sb.append("<row r=\"${r + 1}\">")
            row.forEachIndexed { c, v ->
                val ref = colName(c) + (r + 1)
                when (v) {
                    null -> {}
                    is Int -> sb.append("<c r=\"$ref\" t=\"n\"><v>$v</v></c>")
                    is Double -> sb.append("<c r=\"$ref\" t=\"n\"><v>$v</v></c>")
                    else -> sb.append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v.toString())}</t></is></c>")
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun colName(i: Int): String {
        var n = i; var s = ""
        while (n >= 0) { s = ('A' + n % 26) + s; n = n / 26 - 1 }
        return s
    }

    /** Builds an .xlsx as bytes from named sheets of rows. */
    fun buildXlsx(sheets: List<Pair<String, List<List<Any?>>>>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            val n = sheets.size
            entry("[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                (1..n).joinToString("") { "<Override PartName=\"/xl/worksheets/sheet$it.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" } +
                "</Types>")
            entry("_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                "</Relationships>")
            entry("xl/workbook.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
                "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>" +
                sheets.mapIndexed { i, (name, _) ->
                    "<sheet name=\"${esc(name)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>"
                }.joinToString("") +
                "</sheets></workbook>")
            entry("xl/_rels/workbook.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                (1..n).joinToString("") { "<Relationship Id=\"rId$it\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$it.xml\"/>" } +
                "</Relationships>")
            sheets.forEachIndexed { i, (_, rows) ->
                entry("xl/worksheets/sheet${i + 1}.xml", sheetXml(rows))
            }
        }
        return bos.toByteArray()
    }

    /** Assembles the old-school spreadsheet: List + Summary (+ Now Playing + Backlog). */
    fun buildSpreadsheet(
        games: List<Game>, playing: List<Playing>, backlog: List<Backlog>
    ): ByteArray {
        val perYear = games.groupingBy { it.year }.eachCount()
        val listRows = mutableListOf<List<Any?>>(
            listOf("Year", "No", "Of", "Game", "Console", "Date", "Notes", "Replay",
                "IGDB Year", "IGDB Genres", "IGDB Rating")
        )
        games.sortedWith(compareBy({ it.year }, { it.seq })).forEach { g ->
            listRows.add(listOf(
                g.year, g.seq, perYear[g.year] ?: 0, g.name, g.platform,
                g.date?.take(10), g.notes, if (g.replay) "Yes" else null,
                g.igdbYear, g.igdbGenres?.replace("|", ", "), g.igdbRating?.let { Math.round(it).toInt() }
            ))
        }
        val summaryRows = mutableListOf<List<Any?>>(listOf("Year", "Games"))
        perYear.toSortedMap().forEach { (y, n) -> summaryRows.add(listOf(y, n)) }
        summaryRows.add(listOf("Total", games.size))
        summaryRows.add(emptyList())
        summaryRows.add(listOf("Platform", "Games"))
        games.groupingBy { it.platform }.eachCount().toList()
            .sortedByDescending { it.second }
            .forEach { (p, n) -> summaryRows.add(listOf(p, n)) }
        val playingRows = mutableListOf<List<Any?>>(listOf("Game", "Console", "Since", "Notes"))
        playing.forEach { playingRows.add(listOf(it.name, it.platform, it.started, it.notes)) }
        val backlogRows = mutableListOf<List<Any?>>(listOf("Game", "Console", "Added", "Notes"))
        backlog.forEach { backlogRows.add(listOf(it.name, it.platform, it.added, it.notes)) }
        return buildXlsx(listOf(
            "List" to listRows, "Summary" to summaryRows,
            "Now Playing" to playingRows, "Backlog" to backlogRows
        ))
    }
}
