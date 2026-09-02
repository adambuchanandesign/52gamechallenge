package com.gc52.tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import com.gc52.tracker.data.Game
import com.gc52.tracker.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ReorderScreen(vm: AppViewModel, nav: NavHostController, year: Int) {
    var order by remember { mutableStateOf<List<Game>>(emptyList()) }
    var dirty by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var jumpFor by remember { mutableStateOf<Int?>(null) }   // index whose position is being edited
    val scope = rememberCoroutineScope()
    LaunchedEffect(year) { order = vm.gamesOfYear(year) }

    fun move(from: Int, to: Int) {
        if (to < 0 || to >= order.size || from == to) return
        val l = order.toMutableList()
        val item = l.removeAt(from)
        l.add(to, item)
        order = l
        dirty = true
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
            }
            Column(Modifier.weight(1f)) {
                H1("Reorder $year")
                Text("Arrows nudge; tap a number to jump it to a position.",
                    color = Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(order, key = { _, g -> g.id }) { i, g ->
                Row(
                    Modifier.fillMaxWidth().gradientCard().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "%02d".format(i + 1), color = LogoBlueLight, fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { jumpFor = i }.padding(6.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    PlatformIcon(g.platform, 28)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.name, color = Cream, fontSize = 14.sp, maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(g.date?.take(10) ?: "no date", color = Muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { move(i, i - 1) }, enabled = i > 0, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Up",
                            tint = if (i > 0) Cream else Muted.copy(alpha = 0.3f))
                    }
                    IconButton(onClick = { move(i, i + 1) }, enabled = i < order.size - 1, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Down",
                            tint = if (i < order.size - 1) Cream else Muted.copy(alpha = 0.3f))
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        status?.let { Text(it, color = if (it.startsWith("Saved")) Good else Warn, fontSize = 14.sp) }
        Button(
            enabled = dirty && !saving,
            onClick = {
                saving = true; status = "Renumbering and renaming images…"
                scope.launch {
                    val ok = vm.applyReorder(year, order.map { it.id })
                    saving = false
                    if (ok) { status = "Saved ✓ — numbers and image files updated"; dirty = false
                        order = vm.gamesOfYear(year) }
                    else status = "Something failed — nothing was lost, try again"
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (saving) "Saving…" else "Save new order", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
    }

    jumpFor?.let { idx ->
        var pos by remember { mutableStateOf((idx + 1).toString()) }
        AlertDialog(
            onDismissRequest = { jumpFor = null },
            title = { Text("Move \u201C${order[idx].name}\u201D") },
            text = {
                OutlinedTextField(value = pos, onValueChange = { pos = it.filter(Char::isDigit).take(3) },
                    label = { Text("New position (1–${order.size})") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    pos.toIntOrNull()?.let { p -> move(idx, (p - 1).coerceIn(0, order.size - 1)) }
                    jumpFor = null
                }) { Text("Move") }
            },
            dismissButton = { TextButton(onClick = { jumpFor = null }) { Text("Cancel") } }
        )
    }
}
