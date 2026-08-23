package com.gc52.tracker.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

object Prefs {
    private const val P = "gc52"
    fun treeUri(ctx: Context): Uri? =
        ctx.getSharedPreferences(P, 0).getString("tree", null)?.let(Uri::parse)
    fun setTreeUri(ctx: Context, uri: Uri) =
        ctx.getSharedPreferences(P, 0).edit().putString("tree", uri.toString()).apply()
}

object Storage {

    /** Direct child-document URI for <treeFolder>/<year>/<file>, no slow directory walk. */
    fun imageUri(ctx: Context, year: Int, file: String?): Uri? {
        if (file.isNullOrBlank()) return null
        val tree = Prefs.treeUri(ctx) ?: return null
        return try {
            val treeDoc = DocumentsContract.getTreeDocumentId(tree)
            DocumentsContract.buildDocumentUriUsingTree(tree, "$treeDoc/$year/$file")
        } catch (e: Exception) { null }
    }

    /** User-supplied replacement icon at <treeFolder>/platform-icons/<slug>.png, if present. */
    fun customIconUri(ctx: Context, slug: String): Uri? {
        val tree = Prefs.treeUri(ctx) ?: return null
        return try {
            val treeDoc = DocumentsContract.getTreeDocumentId(tree)
            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, "$treeDoc/platform-icons/$slug.png")
            val df = DocumentFile.fromSingleUri(ctx, uri)
            if (df != null && df.exists()) uri else null
        } catch (e: Exception) { null }
    }

    fun findCsv(ctx: Context, name: String = "52gc-import.csv"): Uri? {
        val tree = Prefs.treeUri(ctx) ?: return null
        val root = DocumentFile.fromTreeUri(ctx, tree) ?: return null
        return root.findFile(name)?.uri
    }

    fun sanitizeFileName(s: String): String =
        s.replace(Regex("[\\\\/:*?\"<>|]"), "").replace(Regex("\\s+"), " ").trim().trimEnd('.')

    fun collageFileName(year: Int, seq: Int, name: String, platform: String, ext: String): String =
        "%d-%03d - %s (%s).%s".format(year, seq, sanitizeFileName(name), sanitizeFileName(platform), ext)

    fun extFor(ctx: Context, uri: Uri): String {
        val mime = ctx.contentResolver.getType(uri) ?: ""
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
    }

    private fun mimeFor(ext: String) = when (ext) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    private fun dir(ctx: Context, name: String): DocumentFile? {
        val tree = Prefs.treeUri(ctx) ?: return null
        val root = DocumentFile.fromTreeUri(ctx, tree) ?: return null
        return root.findFile(name)?.takeIf { it.isDirectory } ?: root.createDirectory(name)
    }

    /** Copies a picked gallery image into <folder>/<year>/<fileName>. Returns true on success. */
    fun copyIntoYear(ctx: Context, src: Uri, year: Int, fileName: String): Boolean {
        return try {
            val yearDir = dir(ctx, year.toString()) ?: return false
            yearDir.findFile(fileName)?.delete()
            val ext = fileName.substringAfterLast('.', "jpg")
            val dest = yearDir.createFile(mimeFor(ext), fileName) ?: return false
            ctx.contentResolver.openInputStream(src)?.use { inp ->
                ctx.contentResolver.openOutputStream(dest.uri)?.use { out -> inp.copyTo(out) }
            } ?: return false
            true
        } catch (e: Exception) { false }
    }

    /** Writes a rendered bitmap as JPEG into <folder>/<year>/<fileName>. */
    fun saveBitmapIntoYear(ctx: Context, bmp: android.graphics.Bitmap, year: Int, fileName: String): Boolean {
        return try {
            val yearDir = dir(ctx, year.toString()) ?: return false
            yearDir.findFile(fileName)?.delete()
            val dest = yearDir.createFile("image/jpeg", fileName) ?: return false
            ctx.contentResolver.openOutputStream(dest.uri)?.use { out ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
            } ?: return false
            true
        } catch (e: Exception) { false }
    }

    /** Writes a text file into <folder>/exports/. Returns the file name on success. */
    fun writeExport(ctx: Context, fileName: String, mime: String, content: String): Boolean {
        return try {
            val exp = dir(ctx, "exports") ?: return false
            exp.findFile(fileName)?.delete()
            val dest = exp.createFile(mime, fileName) ?: return false
            ctx.contentResolver.openOutputStream(dest.uri)?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
            } ?: return false
            true
        } catch (e: Exception) { false }
    }

    /** Moves <folder>/<year>/<fileName> into <folder>/archive/ (copy + delete). */
    fun archiveImage(ctx: Context, year: Int, fileName: String): Boolean {
        return try {
            val tree = Prefs.treeUri(ctx) ?: return false
            val root = DocumentFile.fromTreeUri(ctx, tree) ?: return false
            val src = root.findFile(year.toString())?.findFile(fileName) ?: return true // nothing to archive
            val archive = dir(ctx, "archive") ?: return false
            var destName = fileName
            if (archive.findFile(destName) != null)
                destName = fileName.substringBeforeLast('.') + "-" + System.currentTimeMillis() +
                        "." + fileName.substringAfterLast('.', "jpg")
            val ext = destName.substringAfterLast('.', "jpg")
            val dest = archive.createFile(mimeFor(ext), destName) ?: return false
            ctx.contentResolver.openInputStream(src.uri)?.use { inp ->
                ctx.contentResolver.openOutputStream(dest.uri)?.use { out -> inp.copyTo(out) }
            } ?: return false
            src.delete()
            true
        } catch (e: Exception) { false }
    }
}

/** Minimal RFC-4180-ish CSV reader (handles quoted fields with commas). */
object Csv {
    fun parseLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQ = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQ && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> { sb.append('"'); i++ }
                c == '"' -> inQ = !inQ
                c == ',' && !inQ -> { out.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}

object Importer {
    /** Returns number of games imported. Replaces existing data. */
    suspend fun importCsv(ctx: Context, dao: GameDao, csvUri: Uri): Int {
        val games = ArrayList<Game>()
        ctx.contentResolver.openInputStream(csvUri)?.bufferedReader(Charsets.UTF_8)?.useLines { lines ->
            var header = true
            for (raw in lines) {
                val line = raw.trimEnd('\r')
                if (line.isBlank()) continue
                if (header) { header = false; continue }
                val f = Csv.parseLine(line)
                if (f.size < 4) continue
                val year = f[0].trim().toIntOrNull() ?: continue
                val seq = f[1].trim().substringBefore('/').toIntOrNull() ?: continue
                val name = f[2].trim()
                val plat = f[3].trim()
                val date = f.getOrNull(4)?.trim()?.ifBlank { null }
                val img = f.getOrNull(5)?.trim()?.ifBlank { null }
                val notes = f.getOrNull(6)?.trim()?.ifBlank { null }
                games.add(
                    Game(year = year, seq = seq, name = name, platform = plat,
                        date = date, imageFile = img, notes = notes,
                        normName = normalizeTitle(name))
                )
            }
        }
        if (games.isNotEmpty()) {
            dao.clearGames()
            dao.insertAll(games)
        }
        return games.size
    }
}

/** Platform name -> bundled asset slug (assets/platform-icons/<slug>.png). Null = fallback tile. */
object PlatformIcons {
    private val map = mapOf(
        "Sega Mega Drive" to "sega-mega-drive",
        "Sega Master System" to "sega-master-system",
        "Sega Game Gear" to "sega-game-gear",
        "Sega Mega CD" to "sega-mega-cd",
        "Sega Mega Drive 32X" to "sega-32x",
        "Sega 32X" to "sega-32x",
        "Sega Saturn" to "sega-saturn",
        "Sega Dreamcast" to "sega-dreamcast",
        "Nintendo NES" to "nes",
        "Nintendo SNES" to "super-nintendo",
        "Nintendo 64" to "nintendo-64",
        "Nintendo GameCube" to "nintendo-gamecube",
        "Nintendo Game Boy" to "nintendo-game-boy",
        "Nintendo Game Boy Color" to "nintendo-game-boy-color",
        "Nintendo Game Boy Advance" to "nintendo-game-boy-advance",
        "Nintendo Virtual Boy" to "nintendo-virtual-boy",
        "Nintendo Pokemon Mini" to "nintendo-pokemon-mini",
        "Sony PlayStation" to "sony-playstation",
        "Sony PlayStation 2" to "sony-playstation-2",
        "Microsoft Xbox" to "microsoft-xbox",
        "Commodore Amiga CD32" to "commodore-amiga-cd32",
        "PC Engine" to "nec-pc-engine",
        "NEC PC Engine" to "nec-pc-engine",
        "Atari Lynx" to "atari-lynx",
        "Atari Jaguar" to "atari-jaguar",
        "Atari 5200" to "atari-5200",
        "Atari 7800" to "atari-7800",
        "SNK Neo Geo" to "snk-neo-geo",
        "SNK Neo Geo Pocket" to "snk-neo-geo-pocket",
        "SNK Neo Geo Pocket Color" to "snk-neo-geo-pocket-color",
        "ColecoVision" to "cbs-colecovision",
        "Intellivision" to "mattel-intellivision",
        "Vectrex" to "vectrex",
        "Nokia N-Gage" to "nokia-n-gage",
        "Philips Videopac" to "philips-videopac"
    )

    fun slug(platform: String): String =
        platform.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

    fun assetFor(platform: String): String? = map[platform]?.let { "platform-icons/$it.png" }

    /** Short initials for the fallback tile, e.g. "Nintendo Switch" -> "NS". */
    fun initials(platform: String): String {
        val words = platform.split(' ', '-').filter { it.isNotBlank() }
        return when {
            words.isEmpty() -> "?"
            words.size == 1 -> words[0].take(3).uppercase()
            else -> words.take(3).joinToString("") { it.first().uppercase() }
        }
    }
}
