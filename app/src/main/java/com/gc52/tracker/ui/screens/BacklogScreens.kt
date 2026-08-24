package com.gc52.tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.gc52.tracker.*
import com.gc52.tracker.data.Backlog
import com.gc52.tracker.data.Igdb
import com.gc52.tracker.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BacklogScreen(vm: AppViewModel, nav: NavHostController) {
    val backlog by vm.backlog.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
                }
                H1("Backlog", Modifier.weight(1f))
                TextButton(onClick = { nav.navigate("disambig?mode=backlog") }) {
                    Text("+ Add", color = LogoBlueLight)
                }
            }
        }
        item { BeatenSearch(vm) }
        searchResults(vm, nav)
        if (backlog.isEmpty()) {
            item { Text("Backlog is empty — future you thanks present you for keeping it that way.",
                color = Muted, fontSize = 14.sp) }
        }
        items(backlog.chunked(2), key = { it.first().id }) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniCard(Modifier.weight(1f), pair[0].name, pair[0].platform, pair[0].coverUrl) {
                    nav.navigate("backlogdetail/" + pair[0].id)
                }
                if (pair.size > 1) MiniCard(Modifier.weight(1f), pair[1].name, pair[1].platform, pair[1].coverUrl) {
                    nav.navigate("backlogdetail/" + pair[1].id)
                } else Spacer(Modifier.weight(1f))
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun BacklogDetailScreen(vm: AppViewModel, nav: NavHostController, id: Long) {
    var item by remember { mutableStateOf<Backlog?>(null) }
    var editingNotes by remember { mutableStateOf(false) }
    var notesDraft by remember { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf(false) }
    LaunchedEffect(id) { item = vm.backlogItem(id) }
    val b = item ?: return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
            }
            H1("Backlog")
        }
        b.coverUrl?.let { cover ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = cover.replace("t_cover_small", "t_cover_big"),
                    contentDescription = b.name,
                    modifier = Modifier.width(180.dp).clip(RoundedCornerShape(14.dp))
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformIcon(b.platform, 40)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(b.name, color = Cream, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text(b.platform + (b.added?.let { " · added $it" } ?: ""), color = Muted, fontSize = 14.sp)
            }
        }
        Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notes", color = LogoBlueLight, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    if (editingNotes) {
                        val updated = b.copy(notes = notesDraft.trim().ifBlank { null })
                        vm.updateBacklog(updated); item = updated
                    } else notesDraft = b.notes ?: ""
                    editingNotes = !editingNotes
                }) { Text(if (editingNotes) "Save" else "Edit", color = LogoBlueLight) }
            }
            if (editingNotes) Field("Why it's on the list", notesDraft) { notesDraft = it }
            else Text(b.notes ?: "No notes yet — tap Edit to add why it's here.",
                color = if (b.notes == null) Muted else Cream, fontSize = 14.sp)
        }

        BigLinkButtons(b.name)

        Button(
            onClick = { vm.startPlayingFromBacklog(b); nav.navigate("playing") { popUpTo("home"); launchSingleTop = true } },
            colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start playing", fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = { confirmRemove = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Remove from backlog", color = Warn)
        }
        IgdbAboutBlock(vm, b.name)
        Spacer(Modifier.height(20.dp))
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${b.name}?") },
            text = { Text("Takes it off the backlog.") },
            confirmButton = {
                TextButton(onClick = { vm.removeBacklog(b); nav.popBackStack() }) { Text("Remove", color = Warn) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomScreen(vm: AppViewModel, nav: NavHostController) {
    var genre by remember { mutableStateOf<String?>(null) }
    var era by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Igdb.Details?>(null) }
    var beaten by remember { mutableStateOf<com.gc52.tracker.data.Game?>(null) }
    var spinning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var filtersOpen by remember { mutableStateOf(true) }
    val enabled by vm.igdbEnabled.collectAsState()
    val scope = rememberCoroutineScope()

    fun spin() {
        spinning = true; error = null
        scope.launch {
            val d = vm.randomGame(genre?.let { Igdb.GENRES[it] }, era?.let { Igdb.ERAS[it] })
            if (d == null) error = "Nothing came back — check IGDB credentials or loosen the filters"
            else { result = d; beaten = vm.beatenMatch(d.name); filtersOpen = false }
            spinning = false
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
            }
            H1("Random picker 🎲")
        }
        if (!enabled) {
            Text("Add IGDB credentials in Settings to use the picker.", color = Warn, fontSize = 14.sp)
        }
        Row(
            Modifier.fillMaxWidth().clickable { filtersOpen = !filtersOpen },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                H2("Filters")
                if (!filtersOpen) {
                    Text(
                        listOfNotNull(genre, era).ifEmpty { listOf("Anything goes") }.joinToString(" · "),
                        color = Muted, fontSize = 13.sp
                    )
                }
            }
            Text(if (filtersOpen) "▲" else "▼", color = Muted, fontSize = 14.sp)
        }
        if (filtersOpen) {
            Text("Genre", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Igdb.GENRES.keys.forEach { g ->
                    FilterChip(selected = genre == g, onClick = { genre = if (genre == g) null else g },
                        label = { Text(g, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LogoBlue, selectedLabelColor = Cream,
                            containerColor = Surface1, labelColor = Muted))
                }
            }
            Text("Era", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Igdb.ERAS.keys.forEach { e ->
                    FilterChip(selected = era == e, onClick = { era = if (era == e) null else e },
                        label = { Text(e, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LogoBlue, selectedLabelColor = Cream,
                            containerColor = Surface1, labelColor = Muted))
                }
            }
        }
        Button(
            enabled = enabled && !spinning, onClick = { spin() },
            colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (spinning) "Spinning…" else if (result == null) "Spin 🎲" else "Roll again 🎲",
                fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        }
        error?.let { Text(it, color = Warn, fontSize = 14.sp) }

        result?.let { d ->
            Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // lead with a gameplay shot (skip the first, which is usually the title screen)
                (d.screenshots.getOrNull(1) ?: d.screenshots.firstOrNull())?.let { hero ->
                    AsyncImage(model = hero, contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.33f).clip(RoundedCornerShape(10.dp)))
                }
                Row {
                    d.coverBig?.let {
                        AsyncImage(model = it, contentDescription = d.name,
                            modifier = Modifier.width(110.dp).clip(RoundedCornerShape(10.dp)))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(d.name + (d.year?.let { " ($it)" } ?: ""), color = Cream,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (d.genres.isNotEmpty())
                            Text(d.genres.joinToString(" · "), color = Muted, fontSize = 13.sp)
                        if (d.platforms.isNotEmpty())
                            Text(d.platforms.joinToString(" · ") { Igdb.mapPlatform(it) },
                                color = Muted, fontSize = 13.sp, maxLines = 3)
                        beaten?.let {
                            Text("✓ You beat this in ${it.year} (${it.platform})", color = Good,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                d.summary?.let { Text(it, color = Cream, fontSize = 14.sp, maxLines = 6) }
                BigLinkButtons(d.name)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            vm.pendingNp = AppViewModel.PendingNp(d.name,
                                d.platforms.map { Igdb.mapPlatform(it) }.distinct(),
                                d.coverBig, target = "playing")
                            nav.navigate("npadd")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
                        modifier = Modifier.weight(1f)
                    ) { Text("+ Now playing", fontSize = 14.sp) }
                    Button(
                        onClick = {
                            vm.pendingNp = AppViewModel.PendingNp(d.name,
                                d.platforms.map { Igdb.mapPlatform(it) }.distinct(),
                                d.coverBig, target = "backlog")
                            nav.navigate("npadd")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
                        modifier = Modifier.weight(1f)
                    ) { Text("+ Backlog", fontSize = 14.sp) }
                }
            }
            Button(
                enabled = enabled && !spinning, onClick = { spin() },
                colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (spinning) "Spinning…" else "Roll again 🎲", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        Spacer(Modifier.height(20.dp))
    }
}
