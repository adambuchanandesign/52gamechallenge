package com.gc52.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gc52.tracker.AppViewModel
import com.gc52.tracker.Filters
import com.gc52.tracker.data.Game
import com.gc52.tracker.ui.theme.*

private val eraMap = mapOf(
    "Nintendo NES" to "8-bit", "Sega Master System" to "8-bit", "Nintendo Game Boy" to "8-bit",
    "Nintendo Game Boy Color" to "8-bit", "Nintendo Famicom Disk System" to "8-bit",
    "Sega Game Gear" to "8-bit", "Atari 2600" to "8-bit", "Atari 5200" to "8-bit",
    "Atari 7800" to "8-bit", "ColecoVision" to "8-bit", "Intellivision" to "8-bit", "Vectrex" to "8-bit",
    "Sega Mega Drive" to "16-bit", "Nintendo SNES" to "16-bit", "Sega Mega CD" to "16-bit",
    "Sega Mega Drive 32X" to "16-bit", "PC Engine" to "16-bit", "PC Engine CD" to "16-bit",
    "SNK Neo Geo" to "16-bit", "WonderSwan" to "16-bit",
    "Sony PlayStation" to "32/64-bit", "Sega Saturn" to "32/64-bit", "Nintendo 64" to "32/64-bit",
    "Panasonic 3DO" to "32/64-bit", "Atari Jaguar" to "32/64-bit", "Nintendo Virtual Boy" to "32/64-bit",
    "Nintendo Game Boy Advance" to "32/64-bit", "Commodore Amiga CD32" to "32/64-bit",
    "Sony PlayStation 2" to "Sixth gen", "Nintendo GameCube" to "Sixth gen",
    "Sega Dreamcast" to "Sixth gen", "Microsoft Xbox" to "Sixth gen", "Nokia N-Gage" to "Sixth gen",
    "Xbox 360" to "Seventh gen", "Nintendo Wii" to "Seventh gen", "Nintendo WiiWare" to "Seventh gen",
    "Nintendo DS" to "Seventh gen", "Sony PSP" to "Seventh gen",
    "Nintendo Switch" to "Modern", "Nintendo WiiU" to "Modern", "Nintendo 3DS" to "Modern",
    "Sony PlayStation Vita" to "Modern", "Sony PlayStation 4" to "Modern",
    "Sony PlayStation 5" to "Modern", "Android" to "Modern",
    "Commodore Amiga" to "Home computers", "Amstrad CPC" to "Home computers",
    "ZX Spectrum" to "Home computers", "BBC Micro" to "Home computers", "MSX" to "Home computers",
    "X68000" to "Home computers", "DOS" to "Home computers", "ScummVM" to "Home computers",
    "PC" to "PC", "Arcade" to "Arcade"
)

@Composable
fun StatsScreen(vm: AppViewModel, nav: NavHostController) {
    val yearCounts by vm.yearCounts.collectAsState()
    val platformCounts by vm.platformCounts.collectAsState()
    val seriesList by vm.series.collectAsState()
    var seriesCounts by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var all by remember { mutableStateOf<List<Game>>(emptyList()) }
    var newSeries by remember { mutableStateOf("") }

    LaunchedEffect(seriesList) { seriesCounts = vm.seriesCounts() }
    LaunchedEffect(Unit) { all = vm.allGamesOnce() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
                }
                Text("Stats", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Games per year bar chart
        item {
            Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp)) {
                Text("Games per year", color = LogoBlueLight, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                val max = (yearCounts.maxOfOrNull { it.n } ?: 1).coerceAtLeast(1)
                Row(
                    Modifier.fillMaxWidth().height(158.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    yearCounts.forEach { yc ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${yc.n}", color = Muted, fontSize = 12.sp)
                            Box(
                                Modifier.fillMaxWidth()
                                    .height((110 * yc.n / max).coerceAtLeast(4).dp)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(AccentGradient)
                            )
                            Text("'${yc.year % 100}", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Eras
        item {
            val eras = all.groupingBy { eraMap[it.platform] ?: "Other" }.eachCount()
                .toList().sortedByDescending { it.second }
            Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp)) {
                Text("Eras you play most", color = LogoBlueLight, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val eraMax = (eras.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                eras.forEach { (era, n) ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(era, color = Cream, fontSize = 15.sp, modifier = Modifier.width(120.dp))
                        Box(
                            Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Surface2)
                        ) {
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth(n.toFloat() / eraMax)
                                    .clip(RoundedCornerShape(5.dp)).background(AccentGradient)
                            )
                        }
                        Text(" $n", color = Muted, fontSize = 14.sp)
                    }
                }
            }
        }

        // Series
        item {
            Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp)) {
                Text("Series counts", color = LogoBlueLight, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                seriesCounts.forEach { (name, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(name, color = Cream, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text("$n", color = LogoBlueLight, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { vm.removeSeries(name) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, "remove", tint = Muted, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSeries, onValueChange = { newSeries = it },
                        label = { Text("Add series", color = Muted) }, singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LogoBlueLight,
                            unfocusedBorderColor = Muted.copy(alpha = 0.35f),
                            focusedTextColor = Cream, unfocusedTextColor = Cream
                        )
                    )
                    TextButton(onClick = { if (newSeries.isNotBlank()) { vm.addSeries(newSeries); newSeries = "" } }) {
                        Text("Add")
                    }
                }
            }
        }

        // Fun facts — its own section, one mini block per fact
        item { Text("Fun facts", color = Cream, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        item {
            val facts = remember(all) { buildFacts(all, platformCounts.size) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                facts.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (label, value) ->
                            Column(Modifier.weight(1f).gradientCard().padding(12.dp)) {
                                Text(label, color = LogoBlueLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(value, color = Cream, fontSize = 15.sp,
                                    modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Full platform table
        item { Text("All platforms", color = Cream, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(platformCounts) { pc ->
            Row(
                Modifier.fillMaxWidth().gradientCard()
                    .clickable {
                        vm.filters.value = Filters(platform = pc.platform)
                        nav.navigate("games")
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.gc52.tracker.PlatformIcon(pc.platform, 26)
                Spacer(Modifier.width(10.dp))
                Text(pc.platform, color = Cream, modifier = Modifier.weight(1f), fontSize = 15.sp)
                Text("${pc.n}", color = LogoBlueLight, fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun buildFacts(all: List<Game>, platformCount: Int): List<Pair<String, String>> {
    val facts = ArrayList<Pair<String, String>>()
    val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val dated = all.mapNotNull { g ->
        g.date?.take(10)?.let { d ->
            runCatching { java.time.LocalDate.parse(d) }.getOrNull()?.let { g to it }
        }
    }.sortedBy { it.second }

    dated.firstOrNull()?.let { (g, d) -> facts.add("First logged" to "${g.name} — $d") }
    dated.lastOrNull()?.let { (g, d) -> facts.add("Most recent" to "${g.name} — $d") }

    val byYear = all.groupingBy { it.year }.eachCount()
    byYear.maxByOrNull { it.value }?.let { facts.add("Best year" to "${it.key} (${it.value} games)") }

    if (dated.isNotEmpty()) {
        dated.groupingBy { it.second }.eachCount().maxByOrNull { it.value }?.let { (day, n) ->
            if (n > 1) facts.add("Busiest day" to "$day ($n games!)")
        }
        dated.groupingBy { it.second.dayOfWeek }.eachCount().maxByOrNull { it.value }?.let { (dow, n) ->
            facts.add("Favourite day" to dow.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.UK) + " ($n games)")
        }
        dated.groupingBy { it.second.monthValue }.eachCount().maxByOrNull { it.value }?.let { (m, n) ->
            facts.add("Busiest month" to "${monthNames[m - 1]} ($n games)")
        }
        val gap = dated.zipWithNext().maxByOrNull { (a, b) ->
            java.time.temporal.ChronoUnit.DAYS.between(a.second, b.second)
        }
        gap?.let { (a, b) ->
            val days = java.time.temporal.ChronoUnit.DAYS.between(a.second, b.second)
            if (days > 1) facts.add("Longest drought" to "$days days (${a.first.name} → ${b.first.name})")
        }
        val first = dated.first().second
        val last = dated.last().second
        val weeks = (java.time.temporal.ChronoUnit.DAYS.between(first, last) / 7.0).coerceAtLeast(1.0)
        facts.add("Lifetime pace" to String.format(java.util.Locale.UK, "%.1f games/week", all.size / weeks))
    }

    val ordered = all.sortedWith(compareBy({ it.year }, { it.seq }))
    listOf(100, 250, 500, 750, 1000).forEach { m ->
        ordered.getOrNull(m - 1)?.let { g -> facts.add("Game #$m" to "${g.name} (${g.platform}, ${g.year})") }
    }
    val replays = all.count { it.replay }
    if (replays > 0) facts.add("Replays logged" to "$replays")
    facts.add("Distinct platforms" to "$platformCount")
    facts.add("Total beaten" to "${all.size}")
    return facts
}