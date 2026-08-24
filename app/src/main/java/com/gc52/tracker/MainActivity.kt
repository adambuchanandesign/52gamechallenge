package com.gc52.tracker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.offset
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
    var menuOpen by remember { mutableStateOf(false) }
    val conn = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) barVisible = false
                else if (available.y > 10f) barVisible = true
                return Offset.Zero
            }
        }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val barHPx = with(density) { 64.dp.toPx() }
    val extraPx = with(density) { 64.dp.roundToPx() }
    val barOffset by androidx.compose.animation.core.animateFloatAsState(
        if (barVisible) 0f else -barHPx, label = "barOffset"
    )

    Box(Modifier.fillMaxSize().navigationBarsPadding().nestedScroll(conn).clipToBounds()) {
        Box(
            Modifier
                .layout { measurable, constraints ->
                    // Content is always viewport + bar height tall, so sliding it up
                    // reveals more content instead of a gap. Placement only - never re-measures.
                    val p = measurable.measure(
                        constraints.copy(
                            minHeight = constraints.maxHeight + extraPx,
                            maxHeight = constraints.maxHeight + extraPx
                        )
                    )
                    layout(p.width, constraints.maxHeight) { p.place(0, 0) }
                }
                .graphicsLayer { translationY = barHPx + barOffset }
        ) {
            Box(Modifier.fillMaxSize().padding(bottom = 64.dp)) {
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
                composable("disambig?mode={mode}",
                    arguments = listOf(androidx.navigation.navArgument("mode") { defaultValue = "playing" })
                ) { back ->
                    DisambigScreen(vm, nav, back.arguments?.getString("mode") ?: "playing")
                }
                composable("npadd") { NpAddScreen(vm, nav) }
                composable("backlog") { BacklogScreen(vm, nav) }
                composable("backlogdetail/{id}") { back ->
                    val id = back.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                    BacklogDetailScreen(vm, nav, id)
                }
                composable("random") { RandomScreen(vm, nav) }
                composable("playingdetail/{id}") { back ->
                    val id = back.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                    PlayingDetailScreen(vm, nav, id)
                }
                composable("collage/{id}") { back ->
                    val id = back.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                    CollageScreen(vm, nav, id)
                }
                composable("settings") { SettingsScreen(vm, nav) }
            }
            }
        }
        TopBar(
            nav,
            onMenu = { menuOpen = true },
            modifier = Modifier.align(Alignment.TopCenter)
                .graphicsLayer { translationY = barOffset }
        )
        if (menuOpen) FullScreenMenu(nav) { menuOpen = false }
    }
}

@Composable
fun FullScreenMenu(nav: NavHostController, onClose: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    Box(
        Modifier.fillMaxSize()
            .background(Bg.copy(alpha = 0.98f))
            .clickable(onClick = onClose)
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            Icon(
                Icons.Filled.Home, "Homepage", tint = Cream,
                modifier = Modifier.size(38.dp).clickable {
                    onClose(); nav.navigate("home") { launchSingleTop = true }
                }
            )
            listOf(
                "Completed" to "games",
                "Now Playing" to "playing",
                "Backlog" to "backlog",
                "Add Game" to "add",
                "Random" to "random",
                "Stats" to "stats",
                "Settings" to "settings"
            ).forEach { (label, route) ->
                Text(
                    label, color = Cream, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        onClose()
                        nav.navigate(route) { launchSingleTop = true }
                    }.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, "Close", tint = Muted, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
fun TopBar(nav: NavHostController, onMenu: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(64.dp).background(Surface2)) {
        IconButton(onClick = onMenu, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.Filled.Menu, "Menu", tint = Cream)
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
        HorizontalDivider(
            thickness = 1.dp, color = LogoBlue.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
            Text(g.name, color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
            Text(
                g.platform + if (g.replay) " · replay" else "",
                color = Muted, fontSize = 14.sp, maxLines = 1
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${g.seq}/52", color = Cream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            g.date?.let { Text(it.take(10), color = Muted, fontSize = 14.sp) }
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
        Text("$year", color = Cream, fontWeight = FontWeight.Bold, fontSize = 19.sp,
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
                Text(g.name, color = Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(g.platform, color = Muted, fontSize = 14.sp, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${g.seq}/52", color = Cream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                g.date?.let { Text(it.take(10), color = Muted, fontSize = 14.sp) }
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
        Text(g.name, color = Cream, fontSize = 14.sp, maxLines = 1, fontWeight = FontWeight.Medium)
        Text(g.platform, color = Muted, fontSize = 13.sp, maxLines = 1)
        Text("${g.seq}/52", color = Cream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        g.date?.let { Text(it.take(10), color = Muted, fontSize = 12.sp) }
    }
}

/* ---------- external link helpers ---------- */

fun launchUrl(ctx: android.content.Context, url: String) {
    try {
        ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) { }
}

fun hltbUrl(name: String) = "https://howlongtobeat.com/?q=" + java.net.URLEncoder.encode(name, "UTF-8")
fun longplayUrl(name: String) =
    "https://www.youtube.com/results?search_query=" + java.net.URLEncoder.encode("$name longplay", "UTF-8")

/** Two large blue browser-out buttons. */
@Composable
fun BigLinkButtons(name: String) {
    val ctx = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.weight(1f).gradientButton().clickable { launchUrl(ctx, hltbUrl(name)) }
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center
        ) { Text("HowLongToBeat", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        Row(
            Modifier.weight(1f).gradientButton().clickable { launchUrl(ctx, longplayUrl(name)) }
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center
        ) { Text("Longplays", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
    }
}

/** Small green inline text links. */
@Composable
fun SmallLinkRow(name: String) {
    val ctx = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("HowLongToBeat", color = Good, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { launchUrl(ctx, hltbUrl(name)) })
        Text("Longplays", color = Good, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { launchUrl(ctx, longplayUrl(name)) })
    }
}

/** Generic cover mini card used for Now playing + Backlog 50/50 grids. */
@Composable
fun MiniCard(modifier: Modifier, name: String, platform: String, coverUrl: String?, onClick: () -> Unit) {
    Column(
        modifier.gradientCard().clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.75f)
                .clip(RoundedCornerShape(10.dp)).background(Surface2),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl.replace("t_cover_small", "t_cover_big"),
                    contentDescription = name,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            } else PlatformIcon(platform, 48)
        }
        Spacer(Modifier.height(6.dp))
        Text(name, color = Cream, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            maxLines = 2, modifier = Modifier.fillMaxWidth())
        Text(platform, color = Muted, fontSize = 14.sp, maxLines = 1, modifier = Modifier.fillMaxWidth())
    }
}