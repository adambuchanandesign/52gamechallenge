package com.gc52.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gc52.tracker.AppViewModel
import com.gc52.tracker.PlatformIcon
import com.gc52.tracker.ui.theme.*

@Composable
fun PlayingScreen(vm: AppViewModel, nav: NavHostController) {
    val playing by vm.playing.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
                }
                H1("Now playing", Modifier.weight(1f))
                TextButton(onClick = { nav.navigate("disambig") }) { Text("+ Add", color = LogoBlueLight) }
            }
        }
        item { BeatenSearch(vm) }
        searchResults(vm, nav)

        if (playing.isEmpty()) {
            item { Text("Nothing on the go right now.", color = Muted, fontSize = 15.sp) }
        }
        items(playing, key = { it.id }) { p ->
            Row(
                Modifier.fillMaxWidth().gradientCard().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlatformIcon(p.platform, 30)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
                    Text(p.platform + (p.started?.let { " · since $it" } ?: ""), color = Muted, fontSize = 14.sp)
                    p.notes?.let { Text(it, color = Muted, fontSize = 14.sp, maxLines = 2) }
                }
                TextButton(onClick = { nav.navigate("add?playing=" + p.id) }) {
                    Text("Beaten!", color = Good, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { vm.removePlaying(p) }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Close, "remove", tint = Muted, modifier = Modifier.size(15.dp))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showAdd) {
        AddPlayingDialog(
            onAdd = { n, pf, no -> vm.addPlaying(n, pf, no); showAdd = false },
            onDismiss = { showAdd = false }
        )
    }
}
