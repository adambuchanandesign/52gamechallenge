package com.gc52.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.BitmapFactory
import coil.compose.AsyncImage
import com.gc52.tracker.data.Game
import com.gc52.tracker.data.PlatformIcons
import com.gc52.tracker.data.Storage
import com.gc52.tracker.ui.screens.*
import com.gc52.tracker.ui.theme.*

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
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(vm, nav) }
        composable("games") { GamesScreen(vm, nav) }
        composable("detail/{id}") { back ->
            val id = back.arguments?.getString("id")?.toLongOrNull() ?: return@composable
            DetailScreen(vm, nav, id)
        }
        composable("add") { AddScreen(vm, nav) }
        composable("settings") { SettingsScreen(vm, nav) }
    }
}

/* ---------- shared components ---------- */

@Composable
fun PlatformIcon(platform: String, size: Int = 34) {
    val ctx = LocalContext.current
    val custom = remember(platform) { Storage.customIconUri(ctx, PlatformIcons.slug(platform)) }
    val asset = remember(platform) { PlatformIcons.assetFor(platform) }
    when {
        custom != null -> AsyncImage(
            model = custom, contentDescription = platform,
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape(6.dp))
        )
        asset != null -> {
            val bmp = remember(asset) {
                try { ctx.assets.open(asset).use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }
            }
            if (bmp != null) {
                Image(bmp.asImageBitmap(), platform, modifier = Modifier.size(size.dp))
            } else FallbackIcon(platform, size)
        }
        else -> FallbackIcon(platform, size)
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
                "${g.platform} · ${g.year} · ${g.seq}/52" + if (g.replay) " · replay" else "",
                color = Muted, fontSize = 12.sp, maxLines = 1
            )
        }
        g.date?.let { Text(it.take(10), color = Muted, fontSize = 11.sp) }
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
        Text(g.name, color = Cream, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Medium)
        Text("${g.year} · ${g.platform}", color = Muted, fontSize = 10.sp, maxLines = 1)
    }
}
