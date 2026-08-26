package org.nqmgaming.aneko.presentation.detail.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.pet.CodexPetContract
import org.nqmgaming.aneko.presentation.detail.SkinPreviewCache
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import kotlin.math.min

data class SkinAction(
    val id: String,
    val name: String,
    val icon: String,
)

data class AnimationFrameData(
    val bitmap: Bitmap,
    val durationMs: Long,
)

@Composable
fun SkinAnimationPlayer(
    packageName: String,
    cardColor: Color,
    scaleFactor: Float = 1.0f,
    transparencyAlpha: Float = 1.0f,
    speedMultiplier: Float = 1.0f,
    onlineImageUrl: String? = null,
    downloadUrl: String? = null,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var availableActions by remember(packageName, downloadUrl, onlineImageUrl) {
        mutableStateOf<List<SkinAction>>(emptyList())
    }
    var selectedAction by remember(packageName, downloadUrl, onlineImageUrl) {
        mutableStateOf<SkinAction?>(null)
    }
    var frames by remember(packageName, selectedAction, downloadUrl, onlineImageUrl) {
        mutableStateOf<List<AnimationFrameData>>(emptyList())
    }
    var currentFrameIndex by remember(packageName, selectedAction, downloadUrl, onlineImageUrl) {
        mutableIntStateOf(0)
    }
    var isTapped by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Tap bounce animation
    val bounceScale by animateFloatAsState(
        targetValue = if (isTapped) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "bounceAnimation",
        finishedListener = { isTapped = false }
    )

    // Gentle breathing / bobbing animation for single frame or idle
    val infiniteTransition = rememberInfiniteTransition(label = "idleBobbing")
    val idleBobbingOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbingOffset"
    )

    // 1. Ensure directory is available (from local or fetched to preview cache) & load actions
    LaunchedEffect(packageName, downloadUrl, onlineImageUrl) {
        isLoading = true

        // If online skin with downloadUrl, fetch full package into cache in background
        if (isOnline && !downloadUrl.isNullOrBlank()) {
            SkinPreviewCache.getOrFetchDirectory(context, packageName, downloadUrl)
        }

        val actions = withContext(Dispatchers.IO) {
            loadAvailableActions(context, packageName, onlineImageUrl)
        }
        availableActions = actions
        selectedAction = actions.firstOrNull()
    }

    // 2. Load frames for the selected action
    LaunchedEffect(packageName, selectedAction, downloadUrl, onlineImageUrl) {
        val act = selectedAction
        val loadedFrames = withContext(Dispatchers.IO) {
            loadFramesForAction(context, packageName, act?.id ?: "idle", onlineImageUrl, downloadUrl)
        }
        frames = loadedFrames
        currentFrameIndex = 0
        isLoading = false
    }

    // 3. Frame animation loop
    LaunchedEffect(frames, speedMultiplier) {
        if (frames.size <= 1) return@LaunchedEffect
        while (isActive) {
            val safeIdx = currentFrameIndex.coerceIn(0, frames.lastIndex)
            val frame = frames[safeIdx]
            val duration = (frame.durationMs / speedMultiplier.coerceAtLeast(0.1f)).toLong()
            delay(duration.coerceIn(30L, 2000L))
            currentFrameIndex = (safeIdx + 1) % frames.size
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Hero Live Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(cardColor.copy(alpha = 0.15f))
                .border(
                    width = 2.dp,
                    color = cardColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { isTapped = true }
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Ambient circular backdrop
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(cardColor.copy(alpha = 0.25f))
            )

            // Animated Sprite Canvas
            val currentFrame = frames.getOrNull(currentFrameIndex)?.bitmap
            if (currentFrame != null && !currentFrame.isRecycled) {
                val shouldBob = frames.size == 1
                val yOffset = if (shouldBob) idleBobbingOffset.toInt() else 0

                Canvas(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(scaleFactor * bounceScale)
                ) {
                    val nativeImg = currentFrame.asImageBitmap()
                    val srcW = currentFrame.width.toFloat()
                    val srcH = currentFrame.height.toFloat()
                    val scale = min(size.width / srcW, size.height / srcH)
                    val dstW = (srcW * scale).toInt()
                    val dstH = (srcH * scale).toInt()
                    val posX = ((size.width - dstW) / 2).toInt()
                    val posY = (((size.height - dstH) / 2) + yOffset).toInt()

                    drawImage(
                        image = nativeImg,
                        dstOffset = IntOffset(posX, posY),
                        dstSize = IntSize(dstW, dstH),
                        alpha = transparencyAlpha,
                        filterQuality = FilterQuality.None,
                    )
                }
            } else if (isLoading) {
                CircularProgressIndicator(
                    color = cardColor,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Text(
                    text = "🐾",
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.scale(bounceScale)
                )
            }

            // Hint label at bottom-center
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            ) {
                Text(
                    text = if (frames.size > 1) "✨ ${frames.size} frames"
                    else stringResource(R.string.tap_pet_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Chips Carousel
        if (availableActions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(availableActions, key = { it.id }) { action ->
                    val isSelected = action.id == selectedAction?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAction = action },
                        label = {
                            Text(
                                text = "${action.icon} ${action.name}",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cardColor,
                            selectedLabelColor = Color.White,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
        }
    }
}

/**
 * Returns available actions by inspecting local/cached directories.
 */
private fun loadAvailableActions(
    context: Context,
    packageName: String,
    onlineImageUrl: String?,
): List<SkinAction> {
    val skinDir = resolveSkinDirectory(context, packageName)
    val isCodex = (skinDir != null && skinDir.walkTopDown().any { it.name.equals("pet.json", true) }) ||
            packageName.startsWith("codex.") ||
            (onlineImageUrl?.contains("petdex") == true)

    return if (isCodex) {
        listOf(
            SkinAction("idle", "Idle", "💤"),
            SkinAction("runningRight", "Run Right", "👉"),
            SkinAction("runningLeft", "Run Left", "👈"),
            SkinAction("waving", "Waving", "✨"),
            SkinAction("jumping", "Jumping", "🦘"),
            SkinAction("waiting", "Waiting", "⏳"),
            SkinAction("failed", "Failed", "💦"),
            SkinAction("running", "Run", "🏃"),
            SkinAction("review", "Review", "🔍"),
        )
    } else {
        listOf(
            SkinAction("idle", "Idle", "🐾"),
            SkinAction("right", "Run Right", "👉"),
            SkinAction("left", "Run Left", "👈"),
            SkinAction("up", "Climb Up", "👆"),
            SkinAction("down", "Climb Down", "👇"),
            SkinAction("sleep", "Sleeping", "💤"),
            SkinAction("kaki", "Scratching", "💅"),
            SkinAction("akubi", "Yawning", "🥱"),
            SkinAction("togi", "Wall Scratch", "🧱"),
        )
    }
}

/**
 * Finds the directory holding skin assets: either installed skins or cached preview directory.
 */
private fun resolveSkinDirectory(context: Context, packageName: String): File? {
    val installedDir = File(File(context.filesDir, "skins"), packageName)
    if (installedDir.exists() && installedDir.isDirectory && installedDir.listFiles()?.isNotEmpty() == true) {
        return installedDir
    }
    val previewDir = File(File(context.cacheDir, "skin_previews"), packageName)
    if (previewDir.exists() && previewDir.isDirectory && previewDir.listFiles()?.isNotEmpty() == true) {
        return previewDir
    }
    return null
}

/**
 * Loads animation frame bitmaps for the specified action.
 */
private suspend fun loadFramesForAction(
    context: Context,
    packageName: String,
    actionId: String,
    onlineImageUrl: String?,
    downloadUrl: String?
): List<AnimationFrameData> {
    var skinDir = resolveSkinDirectory(context, packageName)

    // If not resolved yet and downloadUrl provided, fetch now
    if (skinDir == null && !downloadUrl.isNullOrBlank()) {
        skinDir = SkinPreviewCache.getOrFetchDirectory(context, packageName, downloadUrl)
    }

    val hasLocalDir = skinDir != null && skinDir.exists()

    // ── CASE 1: CODEX PET (INSTALLED OR CACHED PREVIEW) ──
    val isCodex = (hasLocalDir && skinDir.walkTopDown().any { it.name.equals("pet.json", true) }) ||
            packageName.startsWith("codex.")

    if (isCodex && hasLocalDir) {
        val atlasFile = skinDir.walkTopDown().firstOrNull { file ->
            file.isFile && (file.name.equals("spritesheet.png", ignoreCase = true) ||
                    file.name.equals("spritesheet.webp", ignoreCase = true))
        }

        if (atlasFile != null) {
            val atlas = BitmapFactory.decodeFile(atlasFile.absolutePath)
            if (atlas != null) {
                val columns = 8
                val cellWidth = atlas.width / columns
                val rows = if (atlas.height >= cellWidth * 11) 11 else 9
                val cellHeight = atlas.height / rows

                val animRow = when (actionId) {
                    "idle" -> CodexPetContract.idle
                    "runningRight" -> CodexPetContract.runningRight
                    "runningLeft" -> CodexPetContract.runningLeft
                    "waving" -> CodexPetContract.waving
                    "jumping" -> CodexPetContract.jumping
                    "failed" -> CodexPetContract.failed
                    "waiting" -> CodexPetContract.waiting
                    "running" -> CodexPetContract.running
                    "review" -> CodexPetContract.review
                    else -> CodexPetContract.idle
                }

                val framesList = mutableListOf<AnimationFrameData>()
                val rowY = animRow.row * cellHeight

                for (col in 0 until animRow.frameCount) {
                    val colX = col * cellWidth
                    if (colX + cellWidth <= atlas.width && rowY + cellHeight <= atlas.height) {
                        val frameBmp = Bitmap.createBitmap(atlas, colX, rowY, cellWidth, cellHeight)
                        val duration = animRow.durationsMs.getOrElse(col) { 150 }.toLong()
                        framesList.add(AnimationFrameData(frameBmp, duration))
                    }
                }
                if (framesList.isNotEmpty()) return framesList
            }
        }
    }

    // ── CASE 2: CLASSIC ANEKO SKIN (INSTALLED, CACHED PREVIEW, OR BUILT-IN) ──
    val allFiles = if (hasLocalDir) {
        skinDir.walkTopDown().filter { it.isFile && it.extension in listOf("png", "webp", "jpg") }.toList()
    } else {
        emptyList()
    }

    val isBuiltIn = packageName == context.packageName || packageName == "org.nqmgaming.aneko"

    val frameCandidates = when (actionId) {
        "idle" -> listOf("mati1", "mati2", "kaki1", "kaki2", "awake")
        "right" -> listOf("right1", "right2")
        "left" -> listOf("left1", "left2")
        "up" -> listOf("up1", "up2")
        "down" -> listOf("down1", "down2")
        "sleep" -> listOf("sleep1", "sleep2")
        "kaki" -> listOf("kaki1", "kaki2")
        "akubi" -> listOf("akubi1", "akubi2", "akubi_l", "akubi_r")
        "togi" -> listOf("rtogi1", "rtogi2", "ltogi1", "ltogi2", "utogi1", "utogi2", "dtogi1", "dtogi2")
        else -> listOf("awake", "mati1", "mati2")
    }

    val loaded = mutableListOf<AnimationFrameData>()
    for (candidateName in frameCandidates) {
        val file = allFiles.firstOrNull { it.nameWithoutExtension.equals(candidateName, ignoreCase = true) }
        if (file != null) {
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            if (bmp != null) {
                loaded.add(AnimationFrameData(bmp, 200L))
            }
        } else if (isBuiltIn) {
            try {
                context.assets.open("aneko/$candidateName.png").use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        loaded.add(AnimationFrameData(bmp, 200L))
                    }
                }
            } catch (_: Exception) {}
        }
    }

    if (loaded.isNotEmpty()) return loaded

    // ── CASE 3: XML PARSING FROM CACHED/LOCAL SKIN ──
    val skinXmlFile = if (hasLocalDir) {
        skinDir.walkTopDown().firstOrNull { it.isFile && it.name.equals("skin.xml", ignoreCase = true) }
    } else null

    if (skinXmlFile != null) {
        try {
            val xmlFrames = parseXmlFramesForAction(skinXmlFile, actionId)
            for (frameName in xmlFrames) {
                val file = allFiles.firstOrNull { it.nameWithoutExtension.equals(frameName, ignoreCase = true) }
                if (file != null) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        loaded.add(AnimationFrameData(bmp, 200L))
                    }
                }
            }
            if (loaded.isNotEmpty()) return loaded
        } catch (_: Exception) {}
    }

    // ── CASE 4: ONLINE PREVIEW STRIP / IMAGE FALLBACK ──
    if (!onlineImageUrl.isNullOrBlank()) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(onlineImageUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val isPetdexStrip = onlineImageUrl.contains("preview.webp") ||
                            (bitmap.width >= bitmap.height * 3)

                    if (isPetdexStrip) {
                        val frameCount = 6
                        val frameWidth = bitmap.width / frameCount
                        val frameHeight = bitmap.height
                        val framesList = mutableListOf<AnimationFrameData>()

                        for (col in 0 until frameCount) {
                            val startX = col * frameWidth
                            if (startX + frameWidth <= bitmap.width) {
                                val frameBmp = Bitmap.createBitmap(bitmap, startX, 0, frameWidth, frameHeight)
                                framesList.add(AnimationFrameData(frameBmp, 160L))
                            }
                        }
                        if (framesList.isNotEmpty()) return framesList
                    } else {
                        return listOf(AnimationFrameData(bitmap, 500L))
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Local preview fallback
    if (allFiles.isNotEmpty()) {
        val preview = allFiles.firstOrNull { it.name.contains("preview", ignoreCase = true) || it.name.contains("icon", ignoreCase = true) }
            ?: allFiles.first()
        val bmp = BitmapFactory.decodeFile(preview.absolutePath)
        if (bmp != null) {
            return listOf(AnimationFrameData(bmp, 500L))
        }
    }

    if (isBuiltIn) {
        try {
            context.assets.open("aneko/awake.png").use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) return listOf(AnimationFrameData(bmp, 500L))
            }
        } catch (_: Exception) {}
    }

    return emptyList()
}

/**
 * Parses drawable names from skin.xml for a given action.
 */
private fun parseXmlFramesForAction(xmlFile: File, actionId: String): List<String> {
    val drawables = mutableListOf<String>()
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()

    FileInputStream(xmlFile).use { input ->
        parser.setInput(input, null)
        var event = parser.eventType
        var currentMotionState = ""

        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "motion" -> {
                        currentMotionState = parser.getAttributeValue(null, "state") ?: ""
                    }
                    "item" -> {
                        val isMatching = when (actionId) {
                            "right" -> currentMotionState.contains("right", ignoreCase = true)
                            "left" -> currentMotionState.contains("left", ignoreCase = true)
                            "up" -> currentMotionState.contains("up", ignoreCase = true)
                            "down" -> currentMotionState.contains("down", ignoreCase = true)
                            "sleep" -> currentMotionState.contains("sleep", ignoreCase = true) || currentMotionState.contains("wait", ignoreCase = true)
                            else -> currentMotionState.contains("stop", ignoreCase = true) || currentMotionState.contains("awake", ignoreCase = true)
                        }
                        if (isMatching) {
                            parser.getAttributeValue(null, "drawable")?.trim()?.let {
                                if (it.isNotBlank() && it !in drawables) drawables.add(it)
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
    }
    return drawables
}
