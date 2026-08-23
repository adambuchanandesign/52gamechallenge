package com.gc52.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gc52.tracker.*
import com.gc52.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(vm: AppViewModel, nav: NavHostController) {
    val games by vm.games.collectAsState()
    val filters by vm.filters.collectAsState()
    val years by vm.years.collectAsState()
    val platforms by vm.platforms.collectAsState()

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
            }
            Text("Games", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${games.size}", color = LogoBlueLight, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                vm.filters.value = filters.copy(view = when (filters.view) {
                    ViewMode.LIST -> ViewMode.GRID
                    ViewMode.GRID -> ViewMode.LARGE
                    ViewMode.LARGE -> ViewMode.LIST
                })
            }) {
                Icon(
                    when (filters.view) {
                        ViewMode.LIST -> Icons.Filled.GridView
                        ViewMode.GRID -> Icons.Filled.ViewAgenda
                        ViewMode.LARGE -> Icons.Filled.ViewList
                    },
                    "Toggle view", tint = LogoBlueLight
                )
            }
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val anyFilter = filters.year != null || filters.platform != null || filters.sort != SortMode.NEWEST
            if (anyFilter) {
                AssistChip(
                    onClick = { vm.filters.value = filters.copy(sort = SortMode.NEWEST, year = null, platform = null) },
                    label = { Text("✕ Clear") },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Warn.copy(alpha = 0.18f), labelColor = Warn)
                )
            }
            SortChip("Newest", filters.sort == SortMode.NEWEST) { vm.filters.value = filters.copy(sort = SortMode.NEWEST) }
            SortChip("Oldest", filters.sort == SortMode.OLDEST) { vm.filters.value = filters.copy(sort = SortMode.OLDEST) }
            SortChip("A–Z", filters.sort == SortMode.AZ) { vm.filters.value = filters.copy(sort = SortMode.AZ) }
            DropChip(
                label = filters.year?.toString() ?: "Year",
                selected = filters.year != null,
                options = years.map { it.toString() },
                onClear = { vm.filters.value = filters.copy(year = null) },
                onPick = { vm.filters.value = filters.copy(year = it.toInt()) }
            )
            DropChip(
                label = filters.platform ?: "Console",
                selected = filters.platform != null,
                options = platforms,
                onClear = { vm.filters.value = filters.copy(platform = null) },
                onPick = { vm.filters.value = filters.copy(platform = it) }
            )
        }

        val dividers = filters.sort != SortMode.AZ
        if (filters.view == ViewMode.LIST) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                var lastYear: Int? = null
                games.forEach { g ->
                    if (dividers && g.year != lastYear) {
                        lastYear = g.year
                        item(key = "hd${g.year}") { YearDivider(g.year) }
                    }
                    item(key = g.id) { GameRow(g) { nav.navigate("detail/${g.id}") } }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        } else if (filters.view == ViewMode.LARGE) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                var lastYear: Int? = null
                games.forEach { g ->
                    if (dividers && g.year != lastYear) {
                        lastYear = g.year
                        item(key = "hd${g.year}") { YearDivider(g.year) }
                    }
                    item(key = g.id) { GameLargeCell(g) { nav.navigate("detail/${g.id}") } }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                var lastYear: Int? = null
                games.forEach { g ->
                    if (dividers && g.year != lastYear) {
                        lastYear = g.year
                        item(key = "hd${g.year}", span = { GridItemSpan(maxLineSpan) }) { YearDivider(g.year) }
                    }
                    item(key = g.id) { GameGridCell(g) { nav.navigate("detail/${g.id}") } }
                }
            }
        }
    }
}

@Composable
fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = LogoBlue, selectedLabelColor = Cream,
            containerColor = Surface1, labelColor = Muted
        )
    )
}

@Composable
fun DropChip(label: String, selected: Boolean, options: List<String>, onClear: () -> Unit, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected, onClick = { open = true }, label = { Text(label) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = LogoBlue, selectedLabelColor = Cream,
                containerColor = Surface1, labelColor = Muted
            )
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (selected) DropdownMenuItem(text = { Text("All") }, onClick = { onClear(); open = false })
            options.forEach { o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { onPick(o); open = false })
            }
        }
    }
}
