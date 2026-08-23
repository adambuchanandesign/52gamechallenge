package com.gc52.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gc52.tracker.AppViewModel
import com.gc52.tracker.GameRow
import com.gc52.tracker.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val total by vm.total.collectAsState()
    val pace by vm.pace.collectAsState()
    val platformCounts by vm.platformCounts.collectAsState()
    val query by vm.query.collectAsState()
    val results by vm.searchResults.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.gc52.tracker.R.drawable.logo_52gc),
                    contentDescription = "#52GameChallenge",
                    modifier = Modifier.weight(1f).height(96.dp),
                    alignment = Alignment.CenterStart,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                IconButton(onClick = { nav.navigate("settings") }) {
                    Icon(Icons.Filled.Settings, "Settings", tint = Muted)
                }
            }
        }

        // Have I beaten this?
        item {
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
        if (query.trim().length >= 2) {
            if (results.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().gradientCard().padding(14.dp)) {
                        Text("Not beaten yet — go for it!", color = Good, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                item { Text("Beaten ${results.size}×:", color = Muted, fontSize = 13.sp) }
                items(results, key = { it.id }) { g ->
                    GameRow(g) { nav.navigate("detail/${g.id}") }
                }
            }
        }

        // Pace + totals
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "$total", "games beaten")
                val year = LocalDate.now().year
                StatCard(Modifier.weight(1f), "${pace.count}/52", "$year · week ${pace.week}")
            }
        }
        item {
            val behind = pace.diff < 0
            Box(Modifier.fillMaxWidth().gradientCard().padding(14.dp)) {
                Text(
                    if (behind) "You're ${-pace.diff} behind pace — one game closes the gap a little!"
                    else "You're ${pace.diff} ahead of pace. Nice.",
                    color = if (behind) Warn else Good, fontSize = 14.sp, fontWeight = FontWeight.Medium
                )
            }
        }

        // Nav buttons
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NavButton(Modifier.weight(1f), "Add beaten", Icons.Filled.Add) { nav.navigate("add") }
                NavButton(Modifier.weight(1f), "Browse games", Icons.AutoMirrored.Filled.List) { nav.navigate("games") }
            }
        }

        // Top platforms
        item { Text("Most beaten platforms", color = Cream, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        items(platformCounts.take(5)) { pc ->
            Row(
                Modifier.fillMaxWidth().gradientCard().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.gc52.tracker.PlatformIcon(pc.platform, 28)
                Spacer(Modifier.width(10.dp))
                Text(pc.platform, color = Cream, modifier = Modifier.weight(1f), fontSize = 14.sp)
                Text("${pc.n}", color = LogoBlueLight, fontWeight = FontWeight.Bold)
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun StatCard(modifier: Modifier, big: String, small: String) {
    Column(
        modifier.gradientCard().padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(big, color = LogoBlueLight, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text(small, color = Muted, fontSize = 12.sp)
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
