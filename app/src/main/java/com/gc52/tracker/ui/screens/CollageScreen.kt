package com.gc52.tracker.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gc52.tracker.AppViewModel
import com.gc52.tracker.data.Game
import com.gc52.tracker.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TileState {
    var uri by mutableStateOf<Uri?>(null)
    var scale by mutableStateOf(1f)
    var offX by mutableStateOf(0f)   // fraction of tile size
    var offY by mutableStateOf(0f)
    var rotation by mutableStateOf(0f)
    fun reset() { scale = 1f; offX = 0f; offY = 0f; rotation = 0f }
    /** Rotate about the tile centre: the pan offset swings with the image. */
    fun rotateBy(deg: Float) {
        val rad = Math.toRadians(deg.toDouble())
        val cos = kotlin.math.cos(rad); val sin = kotlin.math.sin(rad)
        val nx = (offX * cos - offY * sin).toFloat()
        val ny = (offX * sin + offY * cos).toFloat()
        offX = nx; offY = ny
        rotation = (((rotation + deg) % 360f) + 360f) % 360f
    }
    fun straighten() { rotateBy(-rotation) }
}

@Composable
fun CollageScreen(vm: AppViewModel, nav: NavHostController, gameId: Long) {
    var game by remember { mutableStateOf<Game?>(null) }
    LaunchedEffect(gameId) { game = vm.game(gameId) }
    val g = game ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val tiles = remember { List(3) { TileState() } }
    var active by remember { mutableStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { tiles[active].uri = uri; tiles[active].reset() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Muted)
            }
            Column {
                Text("Collage builder", color = Cream, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("${g.name} (${g.platform})", color = Muted, fontSize = 14.sp, maxLines = 1)
            }
        }
        Text("Tap an empty tile to pick its photo. Pinch to zoom, drag to pan, twist to rotate.",
            color = Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 6.dp))

        // 2x2 preview: tiles 0,1 / 2,logo — white gutters like the originals
        val gutter = 3.dp
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White).padding(gutter),
            verticalArrangement = Arrangement.spacedBy(gutter)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(gutter)) {
                TileBox(Modifier.weight(1f), tiles[0], active == 0,
                    onPickRequest = { active = 0; pick.launch("image/*") }) { active = 0 }
                TileBox(Modifier.weight(1f), tiles[1], active == 1,
                    onPickRequest = { active = 1; pick.launch("image/*") }) { active = 1 }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gutter)) {
                TileBox(Modifier.weight(1f), tiles[2], active == 2,
                    onPickRequest = { active = 2; pick.launch("image/*") }) { active = 2 }
                // fixed logo tile
                Box(Modifier.weight(1f).aspectRatio(1f).clipToBounds()) {
                    val logo = remember {
                        ctx.assets.open("collage_logo.jpg").use { BitmapFactory.decodeStream(it) }
                    }
                    androidx.compose.foundation.Image(
                        logo.asImageBitmap(), "logo",
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        val t = tiles[active]
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Tile ${active + 1}:", color = LogoBlueLight, fontWeight = FontWeight.Bold)
            if (t.uri != null) {
                OutlinedButton(onClick = { pick.launch("image/*") }) { Text("Change photo") }
                TextButton(onClick = { t.rotateBy(90f) }) {
                    Text("Rotate 90°", color = LogoBlueLight)
                }
                TextButton(onClick = { t.straighten() }, enabled = t.rotation != 0f) {
                    Text("Straighten", color = if (t.rotation != 0f) LogoBlueLight else Muted)
                }
                TextButton(onClick = { t.reset() }) { Text("Reset", color = Muted) }
            } else {
                Text("Tap the tile to pick its photo", color = Muted, fontSize = 14.sp)
            }
        }
        Text(
            "Order guide: 1 = title screen, 2 = final area, 3 = ending.",
            color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(12.dp))
        Button(
            enabled = tiles.all { it.uri != null } && !saving,
            onClick = {
                saving = true; msg = "Rendering…"
                scope.launch {
                    val bmp = withContext(Dispatchers.IO) { renderCollage(ctx, tiles) }
                    if (bmp == null) { msg = "Couldn't load one of the photos"; saving = false; return@launch }
                    vm.saveCollage(g, bmp) { updated ->
                        saving = false
                        msg = if (updated != null) "Saved as ${updated.imageFile}" else "Save failed — check data folder"
                        if (updated != null) scope.launch { nav.popBackStack() }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = LogoBlue),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (saving) "Saving…" else "Save collage (2048×2048)", fontWeight = FontWeight.Bold) }
        msg?.let { Text(it, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun TileBox(modifier: Modifier, t: TileState, selected: Boolean,
            onPickRequest: () -> Unit = {}, onSelect: () -> Unit) {
    Box(
        modifier
            .aspectRatio(1f)
            .clipToBounds()
            .background(Color.Black)
            .pointerInput(t) {
                detectTransformGestures { _, pan, zoom, rot ->
                    onSelect()
                    if (t.uri != null) {
                        t.scale = (t.scale * zoom).coerceIn(0.3f, 6f)
                        t.rotateBy(rot)
                        t.offX += pan.x / size.width
                        t.offY += pan.y / size.height
                    }
                }
            }
            .pointerInput(t) {
                detectTapGestures(onTap = {
                    onSelect()
                    if (t.uri == null) onPickRequest()
                })
            }
    ) {
        if (t.uri != null) {
            // The image node keeps the photo's REAL aspect ratio (height = tile height,
            // width whatever that implies) so nothing is pre-clipped to the square -
            // the tile's own clipToBounds is the only crop, and panning reaches everything.
            // Giving the request an explicit size makes Coil load immediately;
            // a bare painter waits to be drawn first, and we wait for its size -> deadlock.
            val painter = coil.compose.rememberAsyncImagePainter(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(t.uri).size(2048).build()
            )
            val intrinsic = painter.intrinsicSize
            if (intrinsic == androidx.compose.ui.geometry.Size.Unspecified || intrinsic.height <= 0f) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LogoBlueLight, modifier = Modifier.size(28.dp))
                }
            } else {
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                    val ratio = intrinsic.width / intrinsic.height
                    val tilePx = constraints.maxWidth.toFloat()
                    androidx.compose.foundation.Image(
                        painter, contentDescription = null,
                        modifier = Modifier.align(Alignment.Center)
                            .graphicsLayer {
                                scaleX = t.scale; scaleY = t.scale
                                rotationZ = t.rotation
                                translationX = t.offX * tilePx
                                translationY = t.offY * tilePx
                            }
                            .requiredSize(width = maxHeight * ratio, height = maxHeight)
                    )
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(LogoBlue)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Pick photo", color = Cream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (selected) {
            Box(Modifier.fillMaxSize().border(2.dp, LogoBlueLight))
        }
    }
}


/** Renders the 2048x2048 collage from the tile states. */
fun renderCollage(ctx: android.content.Context, tiles: List<TileState>): Bitmap? {
    val OUT = 2048
    val B = 14                 // white gutter
    val T = (OUT - 3 * B) / 2  // tile size
    fun load(uri: Uri): Bitmap? = try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        val maxDim = maxOf(opts.outWidth, opts.outHeight)
        while (maxDim / sample > 2200) sample *= 2
        val o2 = BitmapFactory.Options().apply { inSampleSize = sample }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o2) }
    } catch (e: Exception) { null }

    val bitmaps = tiles.map { load(it.uri ?: return null) ?: return null }
    val logo = ctx.assets.open("collage_logo.jpg").use { BitmapFactory.decodeStream(it) }

    val out = Bitmap.createBitmap(OUT, OUT, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG)

    fun drawTile(ix: Int, iy: Int, bmp: Bitmap, t: TileState?) {
        val left = B + ix * (T + B)
        val top = B + iy * (T + B)
        canvas.save()
        canvas.clipRect(left.toFloat(), top.toFloat(), (left + T).toFloat(), (top + T).toFloat())
        canvas.drawColor(android.graphics.Color.BLACK)
        val cx = left + T / 2f
        val cy = top + T / 2f
        val base = T.toFloat() / bmp.height   // fill the tile height; width pans
        val s = base * (t?.scale ?: 1f)
        canvas.translate(cx + (t?.offX ?: 0f) * T, cy + (t?.offY ?: 0f) * T)
        canvas.rotate(t?.rotation ?: 0f)
        canvas.scale(s, s)
        canvas.drawBitmap(bmp, -bmp.width / 2f, -bmp.height / 2f, paint)
        canvas.restore()
    }
    drawTile(0, 0, bitmaps[0], tiles[0])
    drawTile(1, 0, bitmaps[1], tiles[1])
    drawTile(0, 1, bitmaps[2], tiles[2])
    drawTile(1, 1, logo, null)
    return out
}
