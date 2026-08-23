package com.gc52.tracker.ui.screens

import androidx.compose.foundation.background
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
                            Text("${yc.n}", color = Muted, fontSize = 9.sp)
                            Box(
                                Modifier.fillMaxWidth()
                                    .height((110 * yc.n / max).coerceAtLeast(4).dp)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(AccentGradient)
                            )
                            Text("'${yc.year % 100}", color = Muted, fontSize = 9.sp)
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
                val total = all.size.coerceAtLeast(1)
                eras.forEach { (era, n) ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(era, color = Cream, fontSize = 13.sp, modifier = Modifier.width(120.dp))
                        Box(
                            Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Surface2)
                        ) {
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth(n.toFloat() / total)
                                    .clip(RoundedCornerShape(5.dp)).background(AccentGradient)
                            )
                        }
                        Text(" $n", color = Muted, fontSize = 12.sp)
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
                        Text(name, color = Cream, fontSize = 14.sp, modifier = Modifier.weight(1f))
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

        // Fun facts
        item {
            val byDate = all.filter { it.date != null }.sortedBy { it.date }
            val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            val busiestMonth = byDate.groupingBy { it.date!!.substring(5, 7) }.eachCount()
                .maxByOrNull { it.value }
            val milestones = listOf(100, 250, 500, 750, 1000).mapNotNull { m ->
                all.sortedWith(compareBy({ it.year }, { it.seq })).getOrNull(m - 1)?.let { g -> m to g }
            }
            Column(Modifier.fillMaxWidth().gradientCard().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Fun facts", color = LogoBlueLight, fontWeight = FontWeight.Bold)
                byDate.firstOrNull()?.let { Text("First logged: ${it.name} — ${it.date!!.take(10)}", color = Cream, fontSize = 13.sp) }
                byDate.lastOrNull()?.let { Text("Most recent: ${it.name} — ${it.date!!.take(10)}", color = Cream, fontSize = 13.sp) }
                busiestMonth?.let {
                    Text("Busiest month overall: ${monthNames[it.key.toInt() - 1]} (${it.value} games)", color = Cream, fontSize = 13.sp)
                }
                milestones.forEach { (m, g) ->
                    Text("Game #$m: ${g.name} (${g.platform}, ${g.year})", color = Cream, fontSize = 13.sp)
                }
                Text("Distinct platforms: ${platformCounts.size}", color = Cream, fontSize = 13.sp)
                Text("Total beaten: ${all.size}", color = Cream, fontSize = 13.sp)
            }
        }

        // Full platform table
        item { Text("All platforms", color = Cream, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(platformCounts) { pc ->
            Row(
                Modifier.fillMaxWidth().gradientCard().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.gc52.tracker.PlatformIcon(pc.platform, 26)
                Spacer(Modifier.width(10.dp))
                Text(pc.platform, color = Cream, modifier = Modifier.weight(1f), fontSize = 13.sp)
                Text("${pc.n}", color = LogoBlueLight, fontWeight = FontWeight.Bold)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
