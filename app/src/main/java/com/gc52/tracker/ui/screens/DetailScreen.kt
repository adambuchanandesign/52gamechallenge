package com.gc52.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.gc52.tracker.AppViewModel
import com.gc52.tracker.PlatformIcon
import com.gc52.tracker.data.Game
import com.gc52.tracker.data.Storage
import com.gc52.tracker.ui.theme.*

@Composable
fun DetailScreen(vm: AppViewModel, nav: NavHostController, id: Long) {
    var game by remember { mutableStateOf<Game?>(null) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var replaceMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(id) { game = vm.game(id) }
    val g = game ?: return
    val ctx = LocalContext.current
    val img = remember(g.id, g.imageFile) { Storage.imageUri(ctx, g.year, g.imageFile) }

    val pickReplacement = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            replaceMsg = "Replacing…"
            vm.replaceImage(g, uri) { updated ->
                if (updated != null) { game = updated; replaceMsg = "Image replaced (old one moved to archive/)" }
                else replaceMsg = "Replace failed — check the data folder in Settings"
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { editing = !editing }) { Icon(Icons.Filled.Edit, "Edit", tint = LogoBlueLight) }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete", tint = Warn) }
        }

        if (img != null) {
            AsyncImage(
                model = img, contentDescription = g.name, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            )
        } else if (g.igdbCover != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = g.igdbCover, contentDescription = g.name,
                    modifier = Modifier.width(200.dp).clip(RoundedCornerShape(16.dp))
                )
            }
        } else {
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).gradientCard(),
                contentAlignment = Alignment.Center
            ) { Text("No collage yet", color = Muted) }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformIcon(g.platform, 40)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(g.name, color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(g.platform, color = Muted, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoLine("Challenge", "${g.seq}/52 of ${g.year}" + if (g.replay) "  ·  replay" else "")
            InfoLine("Beaten", g.date ?: "date unknown")
            g.imageFile?.let { InfoLine("Image", it) }
        }

        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp)) {
            Text("Notes", color = LogoBlueLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                g.notes ?: "No notes yet — tap the pencil to add some.",
                color = if (g.notes == null) Muted else Cream, fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { nav.navigate("collage/" + g.id) }) {
                Text(if (g.imageFile == null) "Build collage" else "Rebuild collage")
            }
            OutlinedButton(onClick = { pickReplacement.launch("image/*") }) {
                Text("From gallery")
            }
        }
        replaceMsg?.let {
            Text(it, color = if (it.startsWith("Image replaced")) Good else Muted,
                fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }

        if (editing) {
            Spacer(Modifier.height(14.dp))
            EditForm(g) { updated -> vm.update(updated) { }; game = updated; editing = false }
        }

        // IGDB info block (shared component - same layout as Now Playing / Backlog)
        Spacer(Modifier.height(12.dp))
        com.gc52.tracker.IgdbAboutBlock(vm, g.name, g.igdbId)
        var refreshMsg by remember { mutableStateOf<String?>(null) }
        val refreshScope = rememberCoroutineScope()
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                refreshMsg = "Refreshing…"
                refreshScope.launch {
                    val updated = vm.refreshIgdb(g)
                    if (updated != null) { game = updated; refreshMsg = "IGDB data refreshed ✓" }
                    else refreshMsg = "No IGDB match found for this name"
                }
            }) { Text("Refresh IGDB data", color = Muted, fontSize = 14.sp) }
            refreshMsg?.let { Text(it, color = if (it.startsWith("IGDB")) Good else Muted, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete entry?") },
            text = { Text("Removes ${g.name} from the list. The collage image file is not touched.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(g) { }; nav.popBackStack() }) { Text("Delete", color = Warn) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Row {
        Text(label, color = Muted, fontSize = 15.sp, modifier = Modifier.width(84.dp))
        Text(value, color = Cream, fontSize = 15.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditForm(g: Game, onSave: (Game) -> Unit) {
    var name by remember { mutableStateOf(g.name) }
    var platform by remember { mutableStateOf(g.platform) }
    var date by remember { mutableStateOf(g.date ?: "") }
    var notes by remember { mutableStateOf(g.notes ?: "") }
    var image by remember { mutableStateOf(g.imageFile ?: "") }
    var replay by remember { mutableStateOf(g.replay) }

    Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Edit", color = LogoBlueLight, fontWeight = FontWeight.Bold)
        Field("Name", name) { name = it }
        Field("Platform", platform) { platform = it }
        Field("Date (yyyy-MM-dd or yyyy-MM-dd HH:mm)", date) { date = it }
        Field("Image filename", image) { image = it }
        Field("Notes", notes) { notes = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = replay, onCheckedChange = { replay = it })
            Text("Replay (beaten before)", color = Cream, fontSize = 16.sp)
        }
        Button(
            onClick = {
                onSave(g.copy(
                    name = name.trim(), platform = platform.trim(),
                    date = date.trim().ifBlank { null },
                    imageFile = image.trim().ifBlank { null },
                    notes = notes.trim().ifBlank { null }, replay = replay
                ))
            },
            colors = ButtonDefaults.buttonColors(containerColor = LogoBlue)
        ) { Text("Save") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label, color = Muted) },
        modifier = Modifier.fillMaxWidth(), singleLine = label != "Notes",
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LogoBlueLight, unfocusedBorderColor = Muted.copy(alpha = 0.35f),
            focusedTextColor = Cream, unfocusedTextColor = Cream
        )
    )
}
