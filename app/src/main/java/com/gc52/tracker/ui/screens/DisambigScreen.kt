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

@Composable
fun DisambigScreen(vm: AppViewModel, nav: NavHostController) {
    val query = vm.lastIgdbQuery.ifBlank { vm.query.value.trim() }
    val hits = vm.lastIgdbHits
    var similar by remember { mutableStateOf<List<Game>>(emptyList()) }
    // (name, suggested platforms) for the prefilled Now Playing dialog
    var picking by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    LaunchedEffect(query) { similar = vm.duplicatesOf(query) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
                }
                Column {
                    H1("Which game is it?")
                    Text("\u201C$query\u201D", color = Muted, fontSize = 14.sp)
                }
            }
        }

        // Always-first manual option
        item {
            Row(
                Modifier.fillMaxWidth().gradientCard()
                    .clickable { picking = query to emptyList() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("➕", fontSize = 19.sp, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text("Add new game manually", color = Cream, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Type the details yourself", color = Muted, fontSize = 13.sp)
                }
            }
        }

        if (hits.isEmpty()) {
            item { Text("No IGDB matches for this search.", color = Muted, fontSize = 14.sp) }
        }
        items(hits, key = { it.id }) { h ->
            Row(
                Modifier.fillMaxWidth().gradientCard()
                    .clickable { picking = h.name to h.platforms.map { Igdb.mapPlatform(it) }.distinct() }
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

    picking?.let { (name, plats) ->
        AddPlayingDialog(
            initialName = name,
            platformSuggestions = plats,
            onAdd = { n, pf, no ->
                vm.addPlaying(n, pf, no)
                picking = null
                vm.query.value = ""
                nav.popBackStack()
            },
            onDismiss = { picking = null }
        )
    }
}
