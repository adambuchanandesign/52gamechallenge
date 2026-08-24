package com.gc52.tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gc52.tracker.*
import com.gc52.tracker.data.Playing
import com.gc52.tracker.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val total by vm.total.collectAsState()
    val pace by vm.pace.collectAsState()
    val platformCounts by vm.platformCounts.collectAsState()
    val yearCounts by vm.yearCounts.collectAsState()
    val playing by vm.playing.collectAsState()
    val backlogList by vm.backlog.collectAsState()
    val onThisWeek by vm.onThisWeek.collectAsState()
    var showAddPlaying by remember { mutableStateOf(false) }

    fun openGames(year: Int? = null, platform: String? = null) {
        vm.filters.value = Filters(year = year, platform = platform)
        nav.navigate("games")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Clickable stat cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val range = if (yearCounts.isNotEmpty())
                    "${yearCounts.minOf { it.year }} – ${yearCounts.maxOf { it.year }}" else ""
                StatCard(Modifier.weight(1f), "$total", "games beaten",
                    if (range.isNotEmpty()) range to true else null, "🏆") { openGames() }
                val year = LocalDate.now().year
                val paceLine =
                    if (pace.diff < 0) "(${-pace.diff} behind pace)" else "(${pace.diff} ahead of pace)"
                StatCard(
                    Modifier.weight(1f), "${pace.count}/52", "$year · week ${pace.week}",
                    paceLine to (pace.diff < 0), "📅"
                ) { openGames(year = year) }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NavButton(Modifier.weight(1f), "Add beaten", Icons.Filled.Add) { nav.navigate("add") }
                NavButton(Modifier.weight(1f), "Browse games", Icons.AutoMirrored.Filled.List) { nav.navigate("games") }
            }
        }
        item {
            NavButton(Modifier.fillMaxWidth(), "Random picker 🎲", Icons.Filled.Casino) { nav.navigate("random") }
        }

        item { Spacer(Modifier.height(6.dp)) }
        item { SectionBreak() }

        // ---- Have I beaten this? ----
        item { H1("Have I beaten this?") }
        item { BeatenSearch(vm) }
        searchResults(vm, nav)

        // ---- Now playing ----
        item { SectionBreak() }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                H1("Now playing", Modifier.weight(1f))
                TextButton(onClick = { nav.navigate("disambig") }) { Text("+ Add", color = LogoBlueLight) }
            }
        }
        if (playing.isEmpty()) {
            item {
                Text("Nothing on the go — add what you're partway through so you don't forget.",
                    color = Muted, fontSize = 14.sp)
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniPlayingCard(Modifier.weight(1f), playing[0], vm, nav)
                    if (playing.size > 1) MiniPlayingCard(Modifier.weight(1f), playing[1], vm, nav)
                    else Spacer(Modifier.weight(1f))
                }
            }
            item {
                NavButton(Modifier.fillMaxWidth(), "View all now playing (${playing.size})",
                    Icons.AutoMirrored.Filled.List) { nav.navigate("playing") }
            }
        }

        // ---- Backlog ----
        item { SectionBreak() }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                H1("Backlog", Modifier.weight(1f))
                TextButton(onClick = { nav.navigate("disambig?mode=backlog") }) { Text("+ Add", color = LogoBlueLight) }
            }
        }
        if (backlogList.isEmpty()) {
            item { Text("Nothing queued up — the Random picker can fix that.", color = Muted, fontSize = 12.sp) }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniCard(Modifier.weight(1f), backlogList[0].name, backlogList[0].platform, backlogList[0].coverUrl) {
                        nav.navigate("backlogdetail/" + backlogList[0].id)
                    }
                    if (backlogList.size > 1) MiniCard(Modifier.weight(1f), backlogList[1].name, backlogList[1].platform, backlogList[1].coverUrl) {
                        nav.navigate("backlogdetail/" + backlogList[1].id)
                    } else Spacer(Modifier.weight(1f))
                }
            }
            item {
                NavButton(Modifier.fillMaxWidth(), "View all backlog (${backlogList.size})",
                    Icons.AutoMirrored.Filled.List) { nav.navigate("backlog") }
            }
        }

        // ---- History ----
        item { SectionBreak() }
        item { H1("History") }
        if (onThisWeek.isNotEmpty()) {
            item { H2("On this week") }
            items(onThisWeek.take(6).chunked(2), key = { "w" + it.first().id }) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { GameGridCell(pair[0]) { nav.navigate("detail/${pair[0].id}") } }
                    if (pair.size > 1) Box(Modifier.weight(1f)) { GameGridCell(pair[1]) { nav.navigate("detail/${pair[1].id}") } }
                    else Spacer(Modifier.weight(1f))
                }
            }
        }
        item { H2("Browse by year") }
        items(yearCounts.sortedByDescending { it.year }.chunked(2)) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { yc ->
                    Column(
                        Modifier.weight(1f).gradientCard()
                            .clickable { openGames(year = yc.year) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${yc.year}", color = Cream, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("${yc.n} games", color = Muted, fontSize = 14.sp)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // ---- Stats ----
        item { SectionBreak() }
        item { H1("Stats") }
        item { H2("Most beaten platforms") }
        items(platformCounts.take(5)) { pc ->
            Row(
                Modifier.fillMaxWidth().gradientCard()
                    .clickable { openGames(platform = pc.platform) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlatformIcon(pc.platform, 28)
                Spacer(Modifier.width(10.dp))
                Text(pc.platform, color = Cream, modifier = Modifier.weight(1f), fontSize = 16.sp)
                Text("${pc.n}", color = LogoBlueLight, fontWeight = FontWeight.Bold)
            }
        }

        item {
            NavButton(Modifier.fillMaxWidth(), "Full stats", Icons.Filled.BarChart) { nav.navigate("stats") }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    if (showAddPlaying) {
        AddPlayingDialog(
            onAdd = { n, pf, no -> vm.addPlaying(n, pf, no); showAddPlaying = false },
            onDismiss = { showAddPlaying = false }
        )
    }
}

/* ---------- shared pieces (also used by the Now Playing page) ---------- */

@Composable
fun SectionBreak() {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        HorizontalDivider(thickness = 1.dp, color = Muted.copy(alpha = 0.2f))
    }
}

@Composable
fun H1(text: String, modifier: Modifier = Modifier) =
    Text(text, color = Cream, fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = modifier)

@Composable
fun H2(text: String, modifier: Modifier = Modifier) =
    Text(text, color = Cream, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = modifier)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeatenSearch(vm: AppViewModel) {
    val query by vm.query.collectAsState()
    OutlinedTextField(
        value = query,
        onValueChange = { vm.query.value = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Have I beaten this…?", color = Muted) },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = LogoBlueLight) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LogoBlueLight,
            unfocusedBorderColor = Muted.copy(alpha = 0.35f),
            focusedTextColor = Cream, unfocusedTextColor = Cream
        )
    )
}

/** Search results block for the current query (call inside a LazyListScope). */
fun androidx.compose.foundation.lazy.LazyListScope.searchResults(vm: AppViewModel, nav: NavHostController) {
    item {
        val query by vm.query.collectAsState()
        val igdb by vm.igdbUi.collectAsState()
        if (query.trim().length >= 2) {
            when (val st = igdb) {
                is AppViewModel.IgdbUi.Loaded -> {
                    Row(
                        Modifier.fillMaxWidth().gradientCard()
                            .clickable { nav.navigate("disambig") }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${st.hits.size} result" + (if (st.hits.size == 1) "" else "s") +
                                    " on IGDB", color = Cream, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("for \u201C${st.query}\u201D — tap to pick one", color = Muted, fontSize = 13.sp)
                        }
                        Text("→", color = LogoBlueLight, fontSize = 19.sp)
                    }
                }
                AppViewModel.IgdbUi.Loading -> Text("Searching IGDB…", color = Muted, fontSize = 13.sp)
                AppViewModel.IgdbUi.Error -> Text("IGDB error — check credentials in Settings",
                    color = Warn, fontSize = 13.sp)
                AppViewModel.IgdbUi.Off -> Text("Tip: add IGDB credentials in Settings for live game search",
                    color = Muted, fontSize = 12.sp)
                else -> {}
            }
        }
    }
    item {
        val query by vm.query.collectAsState()
        val results by vm.searchResults.collectAsState()
        if (query.trim().length >= 2) {
            if (results.isEmpty()) {
                Box(Modifier.fillMaxWidth().gradientCard().padding(14.dp)) {
                    Text("Not beaten yet — go for it!", color = Good, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Beaten ${results.size}×:", color = Muted, fontSize = 15.sp)
                    results.take(8).forEach { g ->
                        GameRow(g) { nav.navigate("detail/${g.id}") }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayingCard(modifier: Modifier, p: Playing, vm: AppViewModel, nav: NavHostController) {
    Column(
        modifier.gradientCard()
            .clickable { nav.navigate("playingdetail/" + p.id) }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.75f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                .background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            if (p.coverUrl != null) {
                coil.compose.AsyncImage(
                    model = p.coverUrl!!.replace("t_cover_small", "t_cover_big"),
                    contentDescription = p.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else PlatformIcon(p.platform, 48)
        }
        Spacer(Modifier.height(6.dp))
        Text(p.name, color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            maxLines = 2, modifier = Modifier.fillMaxWidth())
        Text(p.platform, color = Muted, fontSize = 14.sp, maxLines = 1, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun StatCard(modifier: Modifier, big: String, small: String,
             sub: Pair<String, Boolean>? = null, emoji: String? = null, onClick: () -> Unit = {}) {
    Row(
        modifier.gradientCard().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        emoji?.let { Text(it, fontSize = 26.sp, modifier = Modifier.padding(start = 8.dp, end = 10.dp)) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(big, color = Cream, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Text(small, color = Muted, fontSize = 14.sp)
            sub?.let { (line, behind) ->
                Text(line, color = if (behind) Warn else Good, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun NavButton(modifier: Modifier, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier.gradientButton().clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = androidx.compose.ui.graphics.Color.White)
        Spacer(Modifier.width(8.dp))
        Text(label, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlayingDialog(
    initialName: String = "",
    platformSuggestions: List<String> = emptyList(),
    onAdd: (String, String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var n by remember { mutableStateOf(initialName) }
    var pf by remember { mutableStateOf(if (platformSuggestions.size == 1) platformSuggestions[0] else "") }
    var no by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Now playing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Game") }, singleLine = true)
                OutlinedTextField(value = pf, onValueChange = { pf = it }, label = { Text("Platform") }, singleLine = true)
                if (platformSuggestions.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        platformSuggestions.forEach { sug ->
                            SuggestionChip(onClick = { pf = sug }, label = { Text(sug, fontSize = 12.sp) })
                        }
                    }
                }
                OutlinedTextField(value = no, onValueChange = { no = it }, label = { Text("Notes (where you're up to)") })
            }
        },
        confirmButton = {
            TextButton(enabled = n.isNotBlank() && pf.isNotBlank(),
                onClick = { onAdd(n, pf, no.ifBlank { null }) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
