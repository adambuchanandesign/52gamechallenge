package com.gc52.tracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gc52.tracker.AppViewModel
import com.gc52.tracker.data.Game
import com.gc52.tracker.data.Prefs
import com.gc52.tracker.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(vm: AppViewModel, nav: NavHostController, playingId: Long = -1L) {
    var name by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(LocalDate.now().year.toString()) }
    var date by remember { mutableStateOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))) }
    var notes by remember { mutableStateOf("") }
    var replay by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<Uri?>(null) }
    var dups by remember { mutableStateOf<List<Game>>(emptyList()) }
    val platforms by vm.platforms.collectAsState()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) picked = uri
    }

    LaunchedEffect(playingId) {
        if (playingId > 0) vm.playingItem(playingId)?.let { p ->
            name = p.name; platform = p.platform; if (!p.notes.isNullOrBlank()) notes = p.notes!!
        }
    }
    LaunchedEffect(name) { dups = if (name.trim().length >= 3) vm.duplicatesOf(name) else emptyList() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
            }
            Text("Add beaten game", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Field("Game name", name) { name = it }
        if (dups.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().gradientCard().padding(12.dp)) {
                Text("⚠ Possible duplicate:", color = Warn, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                dups.take(5).forEach { d ->
                    Text("• ${d.name} (${d.platform}, ${d.year})", color = Cream, fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp).clickable { nav.navigate("detail/${d.id}") })
                }
                Text("Tick 'Replay' below if this is intentional.", color = Muted, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }

        // platform with suggestions
        var platOpen by remember { mutableStateOf(false) }
        Box {
            Field("Platform", platform) { platform = it; platOpen = true }
            DropdownMenu(expanded = platOpen && platform.isNotBlank(), onDismissRequest = { platOpen = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = false)) {
                platforms.filter { it.contains(platform, ignoreCase = true) && !it.equals(platform, true) }
                    .take(6).forEach { p ->
                        DropdownMenuItem(text = { Text(p) }, onClick = { platform = p; platOpen = false })
                    }
            }
        }

        Field("Year", year) { year = it.filter(Char::isDigit).take(4) }
        Field("Date beaten", date) { date = it }
        Field("Notes", notes) { notes = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = replay, onCheckedChange = { replay = it })
            Text("Replay (beaten before)", color = Cream)
        }

        Column(Modifier.fillMaxWidth().gradientCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Collage", color = LogoBlueLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                if (picked == null) "Made one outside the app? Pick it and it'll be copied into the year folder and renamed to match the collection."
                else "✓ Image selected — will be saved as ${year}-XXX - ${name.ifBlank { "…" }} (${platform.ifBlank { "…" }})",
                color = if (picked == null) Muted else Good, fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickImage.launch("image/*") }) {
                    Text(if (picked == null) "Pick from gallery" else "Pick a different one")
                }
                if (picked != null) TextButton(onClick = { picked = null }) { Text("Remove", color = Warn) }
            }
        }

        Button(
            enabled = name.isNotBlank() && platform.isNotBlank() && year.length == 4,
            onClick = {
                vm.addGame(name, platform, year.toInt(), date.trim().ifBlank { null }, notes, replay, picked) { id ->
                }
                if (playingId > 0) vm.consumePlaying(playingId)
                nav.popBackStack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save", fontWeight = FontWeight.Bold) }
        Text("The in-app collage builder arrives in the next phase.",
            color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val ctx = LocalContext.current
    var folder by remember { mutableStateOf(Prefs.treeUri(ctx)?.lastPathSegment ?: "not set") }
    val status by vm.importStatus.collectAsState()

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            Prefs.setTreeUri(ctx, uri)
            folder = uri.lastPathSegment ?: "chosen"
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
            }
            Text("Settings", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Data folder", color = LogoBlueLight, fontWeight = FontWeight.Bold)
            Text("Your 52GameChallenge folder with the year folders, 52gc-import.csv, and optional platform-icons/.",
                color = Muted, fontSize = 12.sp)
            Text(folder, color = Cream, fontSize = 13.sp)
            Button(onClick = { pickFolder.launch(null) },
                colors = ButtonDefaults.buttonColors(containerColor = LogoBlue)) { Text("Choose folder") }
        }

        Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Import", color = LogoBlueLight, fontWeight = FontWeight.Bold)
            Text("Loads 52gc-import.csv from the data folder. Replaces the current database.",
                color = Muted, fontSize = 12.sp)
            Button(onClick = { vm.runImport(null) },
                colors = ButtonDefaults.buttonColors(containerColor = LogoBlue)) { Text("Import now") }
            status?.let { Text(it, color = if (it.startsWith("Imported")) Good else Warn, fontSize = 13.sp) }
        }

        Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Export", color = LogoBlueLight, fontWeight = FontWeight.Bold)
            Text("Writes a timestamped CSV of everything (opens in Excel, and re-imports here) into exports/ in the data folder.",
                color = Muted, fontSize = 12.sp)
            val exportStatus by vm.exportStatus.collectAsState()
            Button(onClick = { vm.runExport() },
                colors = ButtonDefaults.buttonColors(containerColor = LogoBlue)) { Text("Export now") }
            exportStatus?.let { Text(it, color = if (it.startsWith("Exported")) Good else Warn, fontSize = 13.sp) }
        }

        Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp)) {
            Text("Roadmap", color = LogoBlueLight, fontWeight = FontWeight.Bold)
            Text("Coming next: box art, platform icon manager, ideas backlog.",
                color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
