package com.gc52.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gc52.tracker.AppViewModel
import com.gc52.tracker.PlatformIcon
import com.gc52.tracker.data.Playing
import com.gc52.tracker.ui.theme.*

@Composable
fun PlayingDetailScreen(vm: AppViewModel, nav: NavHostController, id: Long) {
    var item by remember { mutableStateOf<Playing?>(null) }
    var editingNotes by remember { mutableStateOf(false) }
    var notesDraft by remember { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf(false) }
    LaunchedEffect(id) { item = vm.playingItem(id) }
    val p = item ?: return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
            }
            H1("Now playing")
        }

        p.coverUrl?.let { cover ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = cover.replace("t_cover_small", "t_cover_big"),
                    contentDescription = p.name,
                    modifier = Modifier.width(180.dp).clip(RoundedCornerShape(14.dp))
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformIcon(p.platform, 40)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(p.name, color = Cream, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(p.platform + (p.started?.let { " · playing since $it" } ?: ""),
                    color = Muted, fontSize = 14.sp)
            }
        }

        Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notes", color = LogoBlueLight, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    if (editingNotes) {
                        val updated = p.copy(notes = notesDraft.trim().ifBlank { null })
                        vm.updatePlaying(updated); item = updated
                    } else notesDraft = p.notes ?: ""
                    editingNotes = !editingNotes
                }) { Text(if (editingNotes) "Save" else "Edit", color = LogoBlueLight) }
            }
            if (editingNotes) {
                Field("Where you're up to", notesDraft) { notesDraft = it }
            } else {
                Text(
                    p.notes ?: "No notes yet — tap Edit to add where you're up to.",
                    color = if (p.notes == null) Muted else Cream, fontSize = 14.sp
                )
            }
        }

        com.gc52.tracker.BigLinkButtons(p.name)

        Button(
            onClick = { nav.navigate("add?playing=" + p.id) },
            colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Beaten! Log it", fontWeight = FontWeight.Bold) }

        OutlinedButton(
            onClick = { confirmRemove = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Remove from Now playing", color = Warn) }
        Spacer(Modifier.height(20.dp))
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${p.name}?") },
            text = { Text("Takes it off the Now playing list. Nothing is logged as beaten.") },
            confirmButton = {
                TextButton(onClick = { vm.removePlaying(p); nav.popBackStack() }) { Text("Remove", color = Warn) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } }
        )
    }
}
