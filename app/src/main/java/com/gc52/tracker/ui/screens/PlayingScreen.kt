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
        items(playing.chunked(2), key = { it.first().id }) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniPlayingCard(Modifier.weight(1f), pair[0], vm, nav)
                if (pair.size > 1) MiniPlayingCard(Modifier.weight(1f), pair[1], vm, nav)
                else Spacer(Modifier.weight(1f))
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
