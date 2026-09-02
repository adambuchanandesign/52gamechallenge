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
import com.gc52.tracker.data.normalizeTitle
import com.gc52.tracker.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NpAddScreen(vm: AppViewModel, nav: NavHostController) {
    val pending = vm.pendingNp
    var name by remember { mutableStateOf(pending?.name ?: "") }
    var platform by remember {
        mutableStateOf(if (pending != null && pending.platforms.size == 1) pending.platforms[0] else "")
    }
    var notes by remember { mutableStateOf("") }
    val existingPlatforms by vm.platforms.collectAsState()

    val exactMatch = existingPlatforms.any { it.equals(platform.trim(), ignoreCase = true) }
    val closeMatches = remember(platform, existingPlatforms) {
        val n = normalizeTitle(platform)
        if (n.isBlank()) emptyList()
        else existingPlatforms.filter {
            val e = normalizeTitle(it)
            !it.equals(platform.trim(), true) && (e.contains(n) || n.contains(e))
        }.take(4)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
            }
            H1("Add game")
        }

        pending?.coverUrl?.let { cover ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = cover.replace("t_cover_small", "t_cover_big"),
                    contentDescription = name,
                    modifier = Modifier.width(150.dp).clip(RoundedCornerShape(12.dp))
                )
            }
        }

        Field("Game", name) { name = it }
        Field("Platform", platform) { platform = it }

        val suggestions = pending?.platforms ?: emptyList()
        if (suggestions.isNotEmpty()) {
            Text("Platforms for this game:", color = Muted, fontSize = 13.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                suggestions.forEach { sug ->
                    FilterChip(
                        selected = platform.equals(sug, true),
                        onClick = { platform = sug },
                        label = { Text(sug, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LogoBlue, selectedLabelColor = Cream,
                            containerColor = Surface1, labelColor = Muted
                        )
                    )
                }
            }
        }

        if (platform.isNotBlank() && !exactMatch) {
            Column(Modifier.fillMaxWidth().gradientCard().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (closeMatches.isNotEmpty()) {
                    Text("⚠ Similar platform already in your list — reuse it to keep stats tidy:",
                        color = Warn, fontSize = 14.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        closeMatches.forEach { m ->
                            SuggestionChip(onClick = { platform = m }, label = { Text(m, fontSize = 13.sp) })
                        }
                    }
                } else {
                    Text("New platform — it will appear in filters and stats as typed.",
                        color = Muted, fontSize = 13.sp)
                }
            }
        }

        Field("Notes (where you're up to)", notes) { notes = it }

        Text("Where does it go?", color = Muted, fontSize = 14.sp)
        val ready = name.isNotBlank() && platform.isNotBlank()
        fun finish(dest: String) {
            vm.pendingNp = null
            vm.query.value = ""
            nav.navigate(dest) { popUpTo("home"); launchSingleTop = true }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                enabled = ready,
                onClick = {
                    // hand the details to the full beaten form (date, collage, replay...)
                    vm.pendingNp = AppViewModel.PendingNp(name, listOf(platform), pending?.coverUrl,
                        igdbId = pending?.igdbId)
                    vm.query.value = ""
                    // clear the search/choice pages off the stack: back = Home
                    nav.navigate("add") { popUpTo("home"); launchSingleTop = true }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
                modifier = Modifier.weight(1f)
            ) { Text("Beaten it", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            Button(
                enabled = ready,
                onClick = {
                    vm.addPlaying(name, platform, notes, pending?.coverUrl, pending?.igdbId)
                    finish("playing")
                },
                colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
                modifier = Modifier.weight(1f)
            ) { Text("Now playing", fontSize = 14.sp) }
            Button(
                enabled = ready,
                onClick = {
                    vm.addBacklog(name, platform, notes, pending?.coverUrl, pending?.igdbId)
                    finish("backlog")
                },
                colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
                modifier = Modifier.weight(1f)
            ) { Text("Backlog", fontSize = 14.sp) }
        }
        Spacer(Modifier.height(20.dp))
    }
}
