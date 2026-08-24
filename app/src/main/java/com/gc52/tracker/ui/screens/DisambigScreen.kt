package com.gc52.tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.gc52.tracker.GameRow
import com.gc52.tracker.data.Game
import com.gc52.tracker.data.Igdb
import com.gc52.tracker.ui.theme.*
import com.gc52.tracker.AppViewModel.IgdbUi

@Composable
fun DisambigScreen(vm: AppViewModel, nav: NavHostController, mode: String = "playing") {
    val queryState by vm.query.collectAsState()
    val query = queryState.trim()
    val igdb by vm.igdbUi.collectAsState()
    val hits = (igdb as? AppViewModel.IgdbUi.Loaded)?.hits ?: emptyList()
    var similar by remember { mutableStateOf<List<Game>>(emptyList()) }
    LaunchedEffect(query) { similar = if (query.length >= 2) vm.duplicatesOf(query) else emptyList() }
    fun pick(name: String, platforms: List<String>, cover: String?) {
        vm.pendingNp = AppViewModel.PendingNp(name, platforms, cover, target = mode)
        nav.navigate("npadd")
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
                }
                H1(if (mode == "backlog") "Add to Backlog" else "Add to Now playing")
            }
        }
        item { BeatenSearch(vm) }
        item {
            when (igdb) {
                AppViewModel.IgdbUi.Loading -> Text("Searching IGDB…", color = Muted, fontSize = 13.sp)
                AppViewModel.IgdbUi.Error -> Text("IGDB error — check credentials in Settings",
                    color = Warn, fontSize = 13.sp)
                AppViewModel.IgdbUi.Off -> Text("Tip: add IGDB credentials in Settings for live results",
                    color = Muted, fontSize = 13.sp)
                else -> {}
            }
        }

        // Manual entry - expanded by default when nothing is being searched
        item {
            var manualOpen by remember { mutableStateOf(query.isEmpty()) }
            Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable { manualOpen = !manualOpen },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("➕", fontSize = 19.sp, modifier = Modifier.padding(end = 12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Add game manually", color = Cream, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Type the details yourself", color = Muted, fontSize = 13.sp)
                    }
                    Text(if (manualOpen) "▲" else "▼", color = Muted, fontSize = 14.sp)
                }
                if (manualOpen) ManualNpForm(vm, nav, prefill = query, target = mode)
            }
        }

        if (hits.isEmpty() && query.length >= 2 && igdb is AppViewModel.IgdbUi.Loaded) {
            item { Text("No IGDB matches for this search.", color = Muted, fontSize = 14.sp) }
        }
        items(hits, key = { it.id }) { h ->
            Row(
                Modifier.fillMaxWidth().gradientCard()
                    .clickable { pick(h.name, h.platforms.map { Igdb.mapPlatform(it) }.distinct(), h.coverUrl) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (h.coverUrl != null) {
                    AsyncImage(
                        model = h.coverUrl, contentDescription = h.name,
                        modifier = Modifier.size(width = 45.dp, height = 60.dp).clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Box(Modifier.size(width = 45.dp, height = 60.dp).clip(RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center) { Text("🎮", fontSize = 19.sp) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.name + (h.year?.let { " ($it)" } ?: ""), color = Cream,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2)
                    if (h.platforms.isNotEmpty()) {
                        Text(h.platforms.joinToString(" · ") { Igdb.mapPlatform(it) },
                            color = Muted, fontSize = 12.sp, maxLines = 2)
                    }
                    com.gc52.tracker.SmallLinkRow(h.name)
                }
            }
        }

        if (similar.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)) }
            item { H2("Already beaten with a similar name") }
            items(similar, key = { "b" + it.id }) { g ->
                GameRow(g) { nav.navigate("detail/${g.id}") }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

}

@Composable
fun ManualNpForm(vm: AppViewModel, nav: NavHostController, prefill: String, target: String = "playing") {
    var name by remember(prefill) { mutableStateOf(prefill) }
    var platform by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val existing by vm.platforms.collectAsState()
    val exact = existing.any { it.equals(platform.trim(), ignoreCase = true) }
    val close = remember(platform, existing) {
        val n = com.gc52.tracker.data.normalizeTitle(platform)
        if (n.isBlank()) emptyList()
        else existing.filter {
            val e = com.gc52.tracker.data.normalizeTitle(it)
            !it.equals(platform.trim(), true) && (e.contains(n) || n.contains(e))
        }.take(4)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Field("Game", name) { name = it }
        Field("Platform", platform) { platform = it }
        if (platform.isNotBlank() && !exact && close.isNotEmpty()) {
            Text("⚠ Similar platform exists — reuse it to keep stats tidy:", color = Warn, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                close.forEach { m -> SuggestionChip(onClick = { platform = m }, label = { Text(m, fontSize = 13.sp) }) }
            }
        }
        Field("Notes (where you're up to)", notes) { notes = it }
        Button(
            enabled = name.isNotBlank() && platform.isNotBlank(),
            onClick = {
                if (target == "backlog") vm.addBacklog(name, platform, notes)
                else vm.addPlaying(name, platform, notes)
                vm.query.value = ""
                nav.navigate(if (target == "backlog") "backlog" else "playing") {
                    popUpTo("home"); launchSingleTop = true
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = LogoBlue)
        ) { Text("Start playing", fontWeight = FontWeight.Bold) }
    }
}