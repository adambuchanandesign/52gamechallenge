package com.gc52.tracker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gc52.tracker.data.Game
import com.gc52.tracker.data.PlatformIcons
import com.gc52.tracker.data.Storage
import com.gc52.tracker.ui.screens.*
import com.gc52.tracker.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GC52Theme {
                val nav = rememberNavController()
                AppNav(nav)
            }
        }
    }
}

@Composable
fun AppNav(nav: NavHostController) {
    val vm: AppViewModel = viewModel()
    var barVisible by remember { mutableStateOf(true) }
    val conn = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) barVisible = false
                else if (available.y > 10f) barVisible = true
                return Offset.Zero
            }
        }
    }
    val topPad by animateDpAsState(if (barVisible) 64.dp else 0.dp, label = "topPad")

    Box(Modifier.fillMaxSize().nestedScroll(conn)) {
        Box(Modifier.fillMaxSize().padding(top = topPad)) {
            NavHost(navController = nav, startDestination = "home") {
                composable("home") { HomeScreen(vm, nav) }
                composable("games") { GamesScreen(vm, nav) }
                composable("detail/{id}") { back ->
                    val id = back.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                    DetailScreen(vm, nav, id)
                }
                composable("add?playing={pid}",
                    arguments = listOf(androidx.navigation.navArgument("pid") { defaultValue = "-1" })
                ) { back ->
                    val pid = back.arguments?.getString("pid")?.toLongOrNull() ?: -1L
                    AddScreen(vm, nav, pid)
                }
                composable("stats") { StatsScreen(vm, nav) }
                composable("playing") { PlayingScreen(vm, nav) }
                composable("collage/{id}") { back ->
                    val id = back.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                    CollageScreen(vm, nav, id)
                }
                composable("settings") { SettingsScreen(vm, nav) }
            }
        }
        AnimatedVisibility(
            visible = barVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) { TopBar(nav) }
    }
}

@Composable
fun TopBar(nav: NavHostController) {
    var menuOpen by remember { mutableStateOf(false) }
    Column {
        Box(Modifier.fillMaxWidth().height(63.dp).background(Surface2)) {
            Box(Modifier.align(Alignment.CenterStart)) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.Menu, "Menu", tint = Cream)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    listOf(
                        "Homepage" to "home",
                        "Completed" to "games",
                        "Now Playing" to "playing",
                        "Stats" to "stats",
                        "Settings" to "settings"
                    ).forEach { (label, route) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                menuOpen = false
                                nav.navigate(route) { launchSingleTop = true }
                            }
                        )
                    }
                }
            }
            Image(
                painter = painterResource(R.drawable.logo_52gc),
                contentDescription = "#52GameChallenge",
                modifier = Modifier.align(Alignment.Center).height(48.dp)
                    .clickable { nav.navigate("home") { launchSingleTop = true } },
                contentScale = ContentScale.Fit
            )
            IconButton(onClick = { nav.navigate("add") }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Add, "Add beaten game", tint = LogoBlueLight)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = LogoBlue.copy(alpha = 0.35f))
    }
}

/* ---------- platform icon with global cache (fixes list scroll jank) ---------- */

sealed class IconResult {
    data class Custom(val uri: Uri) : IconResult()
    data class Asset(val bmp: Bitmap) : IconResult()
    object Fallback : IconResult()
}

object IconCache {
    private val cache = ConcurrentHashMap<String, IconResult>()
    fun get(platform: String): IconResult? = cache[platform]
    fun load(ctx: android.content.Context, platform: String): IconResult = cache.getOrPut(platform) {
        val custom = Storage.customIconUri(ctx, PlatformIcons.slug(platform))
        if (custom != null) IconResult.Custom(custom)
        else PlatformIcons.assetFor(platform)?.let { a ->
            try {
                ctx.assets.open(a).use { IconResult.Asset(BitmapFactory.decodeStream(it)) }
            } catch (e: Exception) { IconResult.Fallback }
        } ?: IconResult.Fallback
    }
    fun invalidate() = cache.clear()
}

@Composable
fun PlatformIcon(platform: String, size: Int = 34) {
    val ctx = LocalContext.current.applicationContext
    var result by remember(platform) { mutableStateOf(IconCache.get(platform)) }
    LaunchedEffect(platform) {
        if (result == null) {
            result = withContext(Dispatchers.IO) { IconCache.load(ctx, platform) }
        }
    }
    when (val r = result) {
        is IconResult.Custom -> AsyncImage(
            model = r.uri, contentDescription = platform,
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape(6.dp))
        )
        is IconResult.Asset -> Image(r.bmp.asImageBitmap(), platform, modifier = Modifier.size(size.dp))
        IconResult.Fallback -> FallbackIcon(platform, size)
        null -> Box(Modifier.size(size.dp))
    }
}

@Composable
fun FallbackIcon(platform: String, size: Int) {
    Box(
        modifier = Modifier.size(size.dp).clip(RoundedCornerShape(6.dp)).background(AccentGradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            PlatformIcons.initials(platform),
            color = Cream, fontSize = (size * 0.34).sp, fontWeight = FontWeight.Bold
        )
    }
}

/* ---------- list item components ---------- */

@Composable
fun GameRow(g: Game, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .gradientCard()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlatformIcon(g.platform)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(g.name, color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
            Text(
                g.platform + if (g.replay) " · replay" else "",
                color = Muted, fontSize = 13.sp, maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${g.seq}/52", color = Cream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            g.date?.let { Text(it.take(10), color = Muted, fontSize = 13.sp) }
        }
    }
}

@Composable
fun YearDivider(year: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Muted.copy(alpha = 0.25f))
        Text("$year", color = Cream, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = Muted.copy(alpha = 0.25f))
    }
}

@Composable
fun GameLargeCell(g: Game, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val uri = remember(g.id) { Storage.imageUri(ctx, g.year, g.imageFile) }
    Column(
        modifier = Modifier.fillMaxWidth().gradientCard().clickable(onClick = onClick).padding(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                AsyncImage(
                    model = uri, contentDescription = g.name,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            } else PlatformIcon(g.platform, 64)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformIcon(g.platform, 28)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(g.name, color = Cream, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(g.platform, color = Muted, fontSize = 13.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${g.seq}/52", color = Cream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                g.date?.let { Text(it.take(10), color = Muted, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
fun GameGridCell(g: Game, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val uri = remember(g.id) { Storage.imageUri(ctx, g.year, g.imageFile) }
    Column(
        modifier = Modifier.gradientCard().clickable(onClick = onClick).padding(6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                AsyncImage(
                    model = uri, contentDescription = g.name,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            } else PlatformIcon(g.platform, 48)
        }
        Spacer(Modifier.height(6.dp))
        Text(g.name, color = Cream, fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.Medium)
        Text(g.platform, color = Muted, fontSize = 12.sp, maxLines = 1)
        Text("${g.seq}/52", color = Cream, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        g.date?.let { Text(it.take(10), color = Muted, fontSize = 11.sp) }
    }
}
