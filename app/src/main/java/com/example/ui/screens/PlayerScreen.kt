package com.example.ui.screens

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.MediaItem
import com.example.ui.MediaViewModel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CustomVectorIcon(
    type: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        when (type) {
            "back" -> {
                drawLine(color = tint, start = Offset(w * 0.75f, cy), end = Offset(w * 0.25f, cy), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.25f, cy), end = Offset(w * 0.5f, cy - h * 0.2f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.25f, cy), end = Offset(w * 0.5f, cy + h * 0.2f), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
            "more" -> {
                val r = 3.dp.toPx()
                drawCircle(color = tint, radius = r, center = Offset(cx, cy - h * 0.25f))
                drawCircle(color = tint, radius = r, center = Offset(cx, cy))
                drawCircle(color = tint, radius = r, center = Offset(cx, cy + h * 0.25f))
            }
            "aspect" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.22f),
                    size = Size(w * 0.7f, h * 0.56f),
                    style = Stroke(width = 2.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
                // draw top left inside corner
                drawLine(color = tint, start = Offset(w * 0.25f, h * 0.32f), end = Offset(w * 0.35f, h * 0.32f), strokeWidth = 1.5.dp.toPx())
                drawLine(color = tint, start = Offset(w * 0.25f, h * 0.32f), end = Offset(w * 0.25f, h * 0.42f), strokeWidth = 1.5.dp.toPx())
                // draw bottom right inside corner
                drawLine(color = tint, start = Offset(w * 0.75f, h * 0.68f), end = Offset(w * 0.65f, h * 0.68f), strokeWidth = 1.5.dp.toPx())
                drawLine(color = tint, start = Offset(w * 0.75f, h * 0.68f), end = Offset(w * 0.75f, h * 0.58f), strokeWidth = 1.5.dp.toPx())
            }
            "lock" -> {
                // padlock closed
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.32f, h * 0.2f),
                    size = Size(w * 0.36f, h * 0.35f),
                    style = Stroke(width = 2.5.dp.toPx()),
                    cornerRadius = CornerRadius(w * 0.18f)
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.22f, h * 0.45f),
                    size = Size(w * 0.56f, h * 0.38f),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                drawCircle(color = Color.Black, radius = 2.dp.toPx(), center = Offset(cx, h * 0.6f))
            }
            "unlock" -> {
                // padlock open (shackle up and offset)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.32f, h * 0.08f),
                    size = Size(w * 0.36f, h * 0.35f),
                    style = Stroke(width = 2.5.dp.toPx()),
                    cornerRadius = CornerRadius(w * 0.18f)
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.22f, h * 0.45f),
                    size = Size(w * 0.56f, h * 0.38f),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                drawCircle(color = Color.Black, radius = 2.dp.toPx(), center = Offset(cx, h * 0.6f))
            }
            "play" -> {
                val path = Path().apply {
                    moveTo(w * 0.32f, h * 0.22f)
                    lineTo(w * 0.78f, cy)
                    lineTo(w * 0.32f, h * 0.78f)
                    close()
                }
                drawPath(path = path, color = tint)
            }
            "pause" -> {
                val barW = w * 0.15f
                val barH = h * 0.56f
                val gap = w * 0.14f
                val topY = h * 0.22f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx - barW - gap / 2f, topY),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(cx + gap / 2f, topY),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
            "rewind" -> {
                val path1 = Path().apply {
                    moveTo(w * 0.48f, h * 0.25f)
                    lineTo(w * 0.15f, cy)
                    lineTo(w * 0.48f, h * 0.75f)
                    close()
                }
                val path2 = Path().apply {
                    moveTo(w * 0.8f, h * 0.25f)
                    lineTo(w * 0.48f, cy)
                    lineTo(w * 0.8f, h * 0.75f)
                    close()
                }
                drawPath(path1, tint)
                drawPath(path2, tint)
            }
            "forward" -> {
                val path1 = Path().apply {
                    moveTo(w * 0.2f, h * 0.25f)
                    lineTo(w * 0.52f, cy)
                    lineTo(w * 0.2f, h * 0.75f)
                    close()
                }
                val path2 = Path().apply {
                    moveTo(w * 0.52f, h * 0.25f)
                    lineTo(w * 0.85f, cy)
                    lineTo(w * 0.52f, h * 0.75f)
                    close()
                }
                drawPath(path1, tint)
                drawPath(path2, tint)
            }
            "previous" -> {
                val path = Path().apply {
                    moveTo(w * 0.75f, h * 0.25f)
                    lineTo(w * 0.35f, cy)
                    lineTo(w * 0.75f, h * 0.75f)
                    close()
                }
                drawPath(path, tint)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.22f, h * 0.25f),
                    size = Size(w * 0.08f, h * 0.5f),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
            }
            "next" -> {
                val path = Path().apply {
                    moveTo(w * 0.25f, h * 0.25f)
                    lineTo(w * 0.65f, cy)
                    lineTo(w * 0.25f, h * 0.75f)
                    close()
                }
                drawPath(path, tint)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.7f, h * 0.25f),
                    size = Size(w * 0.08f, h * 0.5f),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
            }
            "brightness" -> {
                val r = w * 0.18f
                drawCircle(color = tint, radius = r, center = Offset(cx, cy))
                val rayLen = w * 0.08f
                val rayDist = w * 0.26f
                for (i in 0 until 8) {
                    val angle = i * (Math.PI / 4)
                    val start = Offset(
                        (cx + rayDist * Math.cos(angle)).toFloat(),
                        (cy + rayDist * Math.sin(angle)).toFloat()
                    )
                    val end = Offset(
                        (cx + (rayDist + rayLen) * Math.cos(angle)).toFloat(),
                        (cy + (rayDist + rayLen) * Math.sin(angle)).toFloat()
                    )
                    drawLine(color = tint, start = start, end = end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            "volume" -> {
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.35f)
                    lineTo(w * 0.38f, h * 0.35f)
                    lineTo(w * 0.62f, h * 0.18f)
                    lineTo(w * 0.62f, h * 0.82f)
                    lineTo(w * 0.38f, h * 0.65f)
                    lineTo(w * 0.2f, h * 0.65f)
                    close()
                }
                drawPath(path, tint)
                drawArc(
                    color = tint,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.42f, h * 0.25f),
                    size = Size(w * 0.35f, h * 0.5f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            "seek" -> {
                val r = w * 0.22f
                drawCircle(color = tint, radius = r, center = Offset(cx, cy - h * 0.05f), style = Stroke(width = 2.dp.toPx()))
                drawLine(
                    color = tint,
                    start = Offset(cx + r * 0.707f, cy - h * 0.05f + r * 0.707f),
                    end = Offset(w * 0.82f, h * 0.78f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            "speed" -> {
                drawArc(
                    color = tint,
                    startAngle = -180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.18f),
                    size = Size(w * 0.68f, h * 0.68f),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawLine(
                    color = tint,
                    start = Offset(cx, cy),
                    end = Offset(cx + w * 0.2f, cy - h * 0.12f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            "palette" -> {
                // Paint palette with beautiful pigments
                drawCircle(color = tint, radius = w * 0.35f, center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
                // small hand finger thumb hole
                drawCircle(color = Color.Black, radius = w * 0.08f, center = Offset(cx - w * 0.12f, cy + h * 0.1f))
                // Palette pigments
                drawCircle(color = Color(0xFFFF007F), radius = w * 0.04f, center = Offset(cx + w * 0.14f, cy - h * 0.16f))
                drawCircle(color = Color(0xFF00F0FF), radius = w * 0.04f, center = Offset(cx - w * 0.14f, cy - h * 0.1f))
                drawCircle(color = Color(0xFFFFB300), radius = w * 0.04f, center = Offset(cx + w * 0.16f, cy + h * 0.12f))
            }
            "like_filled" -> {
                val path = Path().apply {
                    moveTo(cx, h * 0.82f)
                    // left side of heart
                    cubicTo(w * 0.12f, h * 0.48f, w * 0.02f, h * 0.18f, cx, h * 0.35f)
                    // right side of heart
                    cubicTo(w * 0.98f, h * 0.18f, w * 0.88f, h * 0.48f, cx, h * 0.82f)
                }
                drawPath(path, tint)
            }
            "like_outline" -> {
                val path = Path().apply {
                    moveTo(cx, h * 0.82f)
                    cubicTo(w * 0.12f, h * 0.48f, w * 0.02f, h * 0.18f, cx, h * 0.35f)
                    cubicTo(w * 0.98f, h * 0.18f, w * 0.88f, h * 0.48f, cx, h * 0.82f)
                }
                drawPath(path, tint, style = Stroke(width = 2.dp.toPx()))
            }
            else -> {
                // generic play/pause symbol if fallback
                drawCircle(color = tint, radius = w * 0.1f)
            }
        }
    }
}

@OptIn(UnstableApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    mediaId: Long,
    viewModel: MediaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val mediaList by viewModel.mediaList.collectAsState()
    
    // Maintain active playing media ID so we can swap back-and-forth dynamically
    var activeMediaId by remember(mediaId) { mutableStateOf(mediaId) }
    
    val mediaItem = remember(mediaList, activeMediaId) {
        mediaList.find { it.id == activeMediaId }
    }

    val playerThemeColorIndex by viewModel.playerThemeColor.collectAsState()
    val playerControlsLayoutIndex by viewModel.playerControlsLayout.collectAsState()
    val isSeekBarBelowButtons by viewModel.isSeekBarBelowButtons.collectAsState()

    val primaryColor = when (playerThemeColorIndex) {
        0 -> NeonGreen
        1 -> Color(0xFFFF007F) // Hot Pink
        2 -> ElectricCyan
        3 -> Color(0xFFFFB300) // Sun Amber
        4 -> Color(0xFFFE3B30) // Crimson Red
        5 -> Color(0xFF2979FF) // Royal Blue
        else -> NeonGreen
    }

    if (mediaItem == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = primaryColor)
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()

    // HUD gesture indicator overlays
    var hudText by remember { mutableStateOf("") }
    var hudIconType by remember { mutableStateOf("play") }
    var isHudVisible by remember { mutableStateOf(false) }
    var hudDisplayJob by remember { mutableStateOf<Job?>(null) }

    fun showOnScreenIndicator(text: String, iconType: String) {
        hudText = text
        hudIconType = iconType
        isHudVisible = true
        hudDisplayJob?.cancel()
        hudDisplayJob = coroutineScope.launch {
            delay(1500)
            isHudVisible = false
        }
    }

    // Audio & Volume controls
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Sync physical hardware volume changes with our on-screen custom indicator
    DisposableEffect(context) {
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if ("android.media.VOLUME_CHANGED_ACTION" == intent.action) {
                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val percent = (currentVol.toFloat() / maxVol.toFloat() * 100).toInt()
                    showOnScreenIndicator("Volume: $percent%", "volume")
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    // Playback state variables
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var isBottomSheetOpen by remember { mutableStateOf(false) }

    // Controls display timer (disappears after 3 seconds of inactivities)
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastInteractionTime, isControlsVisible, isLocked) {
        if (isControlsVisible && !isLocked) {
            delay(3000)
            isControlsVisible = false
        }
    }

    fun resetControlsTimer() {
        if (!isLocked) {
            isControlsVisible = true
            lastInteractionTime = System.currentTimeMillis()
        }
    }

    // ExoPlayer controller instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(audioAttributes, true)
        }
    }

    var playerTracks by remember { mutableStateOf(exoPlayer.currentTracks) }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                playerTracks = tracks
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Dynamic track indices & media loading logic
    var previousMediaId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(activeMediaId, mediaList) {
        // First save previous media playback pos if appropriate
        if (previousMediaId != null && previousMediaId != activeMediaId) {
            val lastPos = exoPlayer.currentPosition
            viewModel.savePlaybackPosition(previousMediaId!!, lastPos)
        }
        previousMediaId = activeMediaId

        val activeItem = mediaList.find { it.id == activeMediaId }
        if (activeItem != null) {
            // Get Scoped Storage resilient URI
            val safePlayUri = getUriForPath(context, activeItem.path)
            exoPlayer.setMediaItem(Media3Item.fromUri(safePlayUri))
            exoPlayer.prepare()
            if (activeItem.lastPlaybackPosition > 0) {
                exoPlayer.seekTo(activeItem.lastPlaybackPosition)
            }
            duration = activeItem.duration
            exoPlayer.play()
            isPlaying = true
            playbackSpeed = exoPlayer.playbackParameters.speed
        }
    }

    // Progress updates loop
    LaunchedEffect(exoPlayer) {
        try {
            while (true) {
                if (exoPlayer.isPlaying) {
                    currentPosition = exoPlayer.currentPosition
                    duration = exoPlayer.duration.coerceAtLeast(0)
                }
                delay(250)
            }
        } catch (e: Exception) {
            // Ignore (e.g., player released or channel closed)
        }
    }

    // Audio Focus Interruption & Headphones handler
    val audioFocusChangeListener = remember {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    mainHandler.post {
                        try {
                            exoPlayer.pause()
                            isPlaying = false
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(exoPlayer) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(context, exoPlayer) {
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                    try {
                        exoPlayer.pause()
                        isPlaying = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Permanent release on exit
    DisposableEffect(Unit) {
        onDispose {
            try {
                viewModel.savePlaybackPosition(activeMediaId, exoPlayer.currentPosition)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                exoPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Resolve next/prev buttons indexes
    val currentIdx = remember(mediaList, activeMediaId) {
        mediaList.indexOfFirst { it.id == activeMediaId }
    }
    val hasPrevious = currentIdx > 0
    val hasNext = currentIdx != -1 && currentIdx < mediaList.lastIndex

    val playNext = {
        if (hasNext) {
            val nextItem = mediaList[currentIdx + 1]
            activeMediaId = nextItem.id
            resetControlsTimer()
        }
    }

    val playPrevious = {
        if (hasPrevious) {
            val prevItem = mediaList[currentIdx - 1]
            activeMediaId = prevItem.id
            resetControlsTimer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Consolidated Gestures engine: Tap, Double tap, Slider seek, Volume, Brightness, 2-Finger speed
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                awaitPointerEventScope {
                    var isDragging = false
                    var dragType = 0 // 1: Brightness, 2: Volume, 3: Seek, 4: Speed
                    var startY = 0f
                    var startX = 0f
                    var startBrightness = 0f
                    var startVolume = 0
                    var totalDragDistanceY = 0f
                    var totalDragDistanceX = 0f
                    var volumeAccumulator = 0f

                    var downTime = 0L
                    var downX = 0f
                    var downY = 0f
                    var lastClickTime = 0L

                    while (true) {
                        val event = awaitPointerEvent()
                        val pointers = event.changes
                        val pointerCount = pointers.size

                        if (pointerCount == 2) {
                            // Two-finger speed scale modifier
                            val p1 = pointers[0]
                            val p2 = pointers[1]
                            if (p1.pressed && p2.pressed) {
                                if (dragType != 4) {
                                    dragType = 4
                                    startY = (p1.position.y + p2.position.y) / 2f
                                }
                                val currentY = (p1.position.y + p2.position.y) / 2f
                                val deltaY = startY - currentY
                                startY = currentY

                                val speedDelta = (deltaY / 300f)
                                val newSpeed = (exoPlayer.playbackParameters.speed + speedDelta).coerceIn(0.5f, 2.0f)
                                val roundedSpeed = (Math.round(newSpeed * 20f) / 20f)
                                exoPlayer.setPlaybackSpeed(roundedSpeed)
                                playbackSpeed = roundedSpeed
                                showOnScreenIndicator("Speed: ${String.format("%.2f", roundedSpeed)}x", "speed")
                                p1.consume()
                                p2.consume()
                            }
                        } else if (pointerCount == 1) {
                            val change = pointers[0]
                            if (change.pressed) {
                                val currentX = change.position.x
                                val currentY = change.position.y

                                if (!isDragging && change.previousPressed == false) {
                                    downTime = System.currentTimeMillis()
                                    downX = currentX
                                    downY = currentY
                                    startX = currentX
                                    startY = currentY
                                    startBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                                    if (startBrightness < 0f) startBrightness = 0.5f
                                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    totalDragDistanceX = 0f
                                    totalDragDistanceY = 0f
                                    dragType = 0
                                    volumeAccumulator = 0f
                                } else if (isDragging) {
                                    val deltaX = currentX - startX
                                    val deltaY = startY - currentY

                                    startX = currentX
                                    startY = currentY
                                    totalDragDistanceX += deltaX
                                    totalDragDistanceY += deltaY

                                    val screenWidth = size.width
                                    if (dragType == 0) {
                                        if (Math.abs(totalDragDistanceX) > 24f || Math.abs(totalDragDistanceY) > 24f) {
                                            resetControlsTimer()
                                            if (Math.abs(totalDragDistanceX) > Math.abs(totalDragDistanceY)) {
                                                dragType = 3 // Seek
                                            } else {
                                                if (change.position.x < (screenWidth / 2f)) {
                                                    dragType = 1 // Brightness
                                                } else {
                                                    dragType = 2 // Volume
                                                }
                                            }
                                        }
                                    }

                                    if (dragType == 1) {
                                        val valDelta = (deltaY / 400f)
                                        val newBright = (activity?.window?.attributes?.screenBrightness?.coerceIn(0f, 1f) ?: 0.5f) + valDelta
                                        val finalBright = newBright.coerceIn(0.01f, 1.0f)
                                        activity?.runOnUiThread {
                                            val lp = activity.window?.attributes
                                            lp?.screenBrightness = finalBright
                                            activity.window?.attributes = lp
                                        }
                                        val percent = (finalBright * 100).toInt()
                                        showOnScreenIndicator("Brightness: $percent%", "brightness")
                                        change.consume()
                                    } else if (dragType == 2) {
                                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        val indexDelta = (deltaY / 50f)
                                        volumeAccumulator += indexDelta
                                        val steps = volumeAccumulator.toInt()
                                        if (steps != 0) {
                                            volumeAccumulator -= steps
                                            val newVol = (currentVol + steps).coerceIn(0, maxVol)
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                            val percent = (newVol.toFloat() / maxVol.toFloat() * 100).toInt()
                                            showOnScreenIndicator("Volume: $percent%", "volume")
                                        } else {
                                            val percent = (currentVol.toFloat() / maxVol.toFloat() * 100).toInt()
                                            showOnScreenIndicator("Volume: $percent%", "volume")
                                        }
                                        change.consume()
                                    } else if (dragType == 3) {
                                        val seekSeconds = (deltaX * 100).toLong()
                                        val targetProgress = (exoPlayer.currentPosition + seekSeconds).coerceIn(0, duration)
                                        exoPlayer.seekTo(targetProgress)
                                        currentPosition = targetProgress
                                        showOnScreenIndicator("Seek: ${formatPosition(targetProgress)}", "seek")
                                        change.consume()
                                    }
                                }

                                if (!isDragging && (Math.abs(currentX - downX) > 12f || Math.abs(currentY - downY) > 12f)) {
                                    isDragging = true
                                }
                            } else {
                                val currentX = change.position.x
                                val currentY = change.position.y
                                if (!isDragging && change.previousPressed == true) {
                                    val clickDuration = System.currentTimeMillis() - downTime
                                    val dist = Math.sqrt(((currentX - downX) * (currentX - downX) + (currentY - downY) * (currentY - downY)).toDouble())
                                    if (clickDuration < 320 && dist < 24) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastClickTime < 320) {
                                            val screenWidth = size.width
                                            if (currentX < screenWidth / 3f) {
                                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                                exoPlayer.seekTo(target)
                                                currentPosition = target
                                                showOnScreenIndicator("Rewind 10s", "rewind")
                                            } else if (currentX > 2f * screenWidth / 3f) {
                                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                                                exoPlayer.seekTo(target)
                                                currentPosition = target
                                                showOnScreenIndicator("Forward 10s", "forward")
                                            } else {
                                                if (isPlaying) {
                                                    exoPlayer.pause()
                                                    isPlaying = false
                                                } else {
                                                    exoPlayer.play()
                                                    isPlaying = true
                                                }
                                                showOnScreenIndicator(if (isPlaying) "Play" else "Pause", if (isPlaying) "play" else "pause")
                                            }
                                        } else {
                                            resetControlsTimer()
                                            isControlsVisible = !isControlsVisible
                                        }
                                        lastClickTime = now
                                    }
                                }
                                isDragging = false
                                dragType = 0
                                volumeAccumulator = 0f
                            }
                        } else {
                            isDragging = false
                            dragType = 0
                            volumeAccumulator = 0f
                        }
                    }
                }
            }
    ) {
        // Underlay video stream view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Background dim overlay
        AnimatedVisibility(
            visible = isControlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            )
        }

        // Consolidated Controls Overlay
        AnimatedVisibility(
            visible = isControlsVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Header (Title/Back/Actions)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .testTag("player_back_button")
                    ) {
                        CustomVectorIcon(type = "back", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaItem.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = mediaItem.folder,
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Audio & Subtitle track & action menu settings Sheet trigger (First button in top-right)
                    IconButton(
                        onClick = {
                            resetControlsTimer()
                            isBottomSheetOpen = true
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        CustomVectorIcon(type = "more", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Aspect ratio (Second button in top-right)
                    IconButton(
                        onClick = {
                            resetControlsTimer()
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                            val scaleStr = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit Screen"
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom Fit"
                                else -> "Fill Screen"
                            }
                            showOnScreenIndicator(scaleStr, "aspect")
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        CustomVectorIcon(type = "aspect", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // 2. Center: Controls Panel
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Previous item
                    IconButton(
                        onClick = { playPrevious() },
                        enabled = hasPrevious,
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (hasPrevious) primaryColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                CircleShape
                            )
                            .border(1.dp, if (hasPrevious) primaryColor.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.1f), CircleShape)
                    ) {
                        CustomVectorIcon(
                            type = "previous",
                            tint = if (hasPrevious) primaryColor else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Rewind 10 Sec
                    IconButton(
                        onClick = {
                            resetControlsTimer()
                            val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(target)
                            currentPosition = target
                            showOnScreenIndicator("Rewind 10s", "rewind")
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(primaryColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, primaryColor.copy(alpha = 0.3f), CircleShape)
                    ) {
                        CustomVectorIcon(type = "rewind", tint = primaryColor, modifier = Modifier.size(20.dp))
                    }

                    // Large glowing circle Play/Pause
                    IconButton(
                        onClick = {
                            resetControlsTimer()
                            if (isPlaying) {
                                exoPlayer.pause()
                                isPlaying = false
                            } else {
                                exoPlayer.play()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .background(primaryColor.copy(alpha = 0.2f), CircleShape)
                            .border(3.dp, primaryColor, CircleShape)
                            .testTag("player_play_pause_button")
                    ) {
                        CustomVectorIcon(
                            type = if (isPlaying) "pause" else "play",
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Forward 10 Sec
                    IconButton(
                        onClick = {
                            resetControlsTimer()
                            val target = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                            exoPlayer.seekTo(target)
                            currentPosition = target
                            showOnScreenIndicator("Forward 10s", "forward")
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(primaryColor.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, primaryColor.copy(alpha = 0.3f), CircleShape)
                    ) {
                        CustomVectorIcon(type = "forward", tint = primaryColor, modifier = Modifier.size(20.dp))
                    }

                    // Next item
                    IconButton(
                        onClick = { playNext() },
                        enabled = hasNext,
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (hasNext) primaryColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                CircleShape
                            )
                            .border(1.dp, if (hasNext) primaryColor.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.1f), CircleShape)
                    ) {
                        CustomVectorIcon(
                            type = "next",
                            tint = if (hasNext) primaryColor else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 3. Footer (Seekbar + Speed/Favorite Actions)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    val seekBarView = @Composable {
                        // Seekbar row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatPosition(currentPosition),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )

                            Slider(
                                value = currentPosition.toFloat(),
                                onValueChange = {
                                    resetControlsTimer()
                                    currentPosition = it.toLong()
                                    exoPlayer.seekTo(it.toLong())
                                },
                                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = primaryColor,
                                    activeTrackColor = primaryColor,
                                    inactiveTrackColor = Color.LightGray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .testTag("player_progress_slider")
                            )

                            Text(
                                text = formatPosition(duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }

                    val buttonsView = @Composable {
                        // Favorites + Accent customizer + Playback Speed controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Favorite toggle
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        resetControlsTimer()
                                        viewModel.toggleFavorite(mediaItem)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CustomVectorIcon(
                                    type = if (mediaItem.isFavorite) "like_filled" else "like_outline",
                                    tint = if (mediaItem.isFavorite) primaryColor else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (mediaItem.isFavorite) "Liked" else "Like",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }

                            // Theme customizer
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        resetControlsTimer()
                                        val nextColorIdx = (playerThemeColorIndex + 1) % 6
                                        viewModel.setPlayerThemeColor(nextColorIdx)
                                        val names = listOf("Neon Green", "Hot Pink", "Electric Cyan", "Sun Amber", "Crimson Red", "Royal Blue")
                                        showOnScreenIndicator(names[nextColorIdx], "palette")
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CustomVectorIcon(
                                    type = "palette",
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Accent",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            // Speed Quick selector
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        resetControlsTimer()
                                        isBottomSheetOpen = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CustomVectorIcon(
                                    type = "speed",
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${String.format("%.2f", playbackSpeed)}x",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (isSeekBarBelowButtons) {
                        buttonsView()
                        Spacer(modifier = Modifier.height(10.dp))
                        seekBarView()
                    } else {
                        seekBarView()
                        Spacer(modifier = Modifier.height(10.dp))
                        buttonsView()
                    }
                }
            }
        }

        // Child Lock icon overlay trigger
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = if (isLocked) 16.dp else 84.dp)
                .zIndex(100f)
        ) {
            IconButton(
                onClick = {
                    if (isLocked) {
                        isLocked = false
                        showOnScreenIndicator("Controls Unlocked", "unlock")
                        resetControlsTimer()
                    } else {
                        isLocked = true
                        isControlsVisible = false
                        showOnScreenIndicator("Controls Locked", "lock")
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isLocked) primaryColor.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .border(1.5.dp, if (isLocked) primaryColor else Color.White.copy(alpha = 0.3f), CircleShape)
            ) {
                CustomVectorIcon(
                    type = if (isLocked) "lock" else "unlock",
                    tint = if (isLocked) primaryColor else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Custom HUD gesturable popup feedback overlay (sleek capsule pill)
        AnimatedVisibility(
            visible = isHudVisible,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                    .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CustomVectorIcon(
                        type = hudIconType,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = hudText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        // Custom Material 3 Bottom Sheet panel
        if (isBottomSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isBottomSheetOpen = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = Color(0xFF141414),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Playback Audio & Subtitles",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    )

                    // Audio track listings
                    Text(
                        text = "Audio Languages",
                        style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan)
                    )
                    val audioTracks = remember(playerTracks) { getAvailableAudioTracks(playerTracks) }
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = "Default Audio Pack (Stereo)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            audioTracks.forEach { aud ->
                                TrackChip(
                                    selected = aud.isSelected,
                                    label = aud.label,
                                    activeColor = primaryColor,
                                    onClick = { selectTrack(exoPlayer, aud.group, aud.trackIndex) }
                                )
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Subtitles container togglers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Display Closed Captions (CC)",
                            style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan)
                        )
                        var ccState by remember(playerTracks) {
                            mutableStateOf(!exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT))
                        }
                        Switch(
                            checked = ccState,
                            onCheckedChange = {
                                ccState = it
                                if (it) enableSubtitles(exoPlayer) else disableSubtitles(exoPlayer)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryColor,
                                checkedTrackColor = primaryColor.copy(alpha = 0.3f)
                            )
                        )
                    }

                    val scriptTracks = remember(playerTracks) { getAvailableSubtitleTracks(playerTracks) }
                    if (scriptTracks.isEmpty()) {
                        Text(
                            text = "No Embedded Subtitles Track Scanned",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            scriptTracks.forEach { sub ->
                                TrackChip(
                                    selected = sub.isSelected,
                                    label = sub.label,
                                    activeColor = primaryColor,
                                    onClick = { selectTrack(exoPlayer, sub.group, sub.trackIndex) }
                                )
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Player Theme Selectors
                    Text(
                        text = "Player Accent Theme Customizer",
                        style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val names = listOf("Neon Green", "Hot Pink", "Electric Cyan", "Sun Amber", "Crimson Red", "Royal Blue")
                        names.forEachIndexed { idx, name ->
                            TrackChip(
                                selected = playerThemeColorIndex == idx,
                                label = name,
                                activeColor = primaryColor,
                                onClick = {
                                    viewModel.setPlayerThemeColor(idx)
                                }
                            )
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Player layout positions
                    Text(
                        text = "Customize Controls Layout Position",
                        style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrackChip(
                            selected = playerControlsLayoutIndex == 0,
                            label = "Centered Classic",
                            activeColor = primaryColor,
                            onClick = { viewModel.setPlayerControlsLayout(0) }
                        )
                        TrackChip(
                            selected = playerControlsLayoutIndex == 1,
                            label = "Compact Bottom (MX Style)",
                            activeColor = primaryColor,
                            onClick = { viewModel.setPlayerControlsLayout(1) }
                        )
                    }

                    // Seekbar below buttons dynamic reordering switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSeekBarBelowButtons(!isSeekBarBelowButtons) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Seekbar Below Buttons Position",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Toggles seekbar row to sit below control triggers",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isSeekBarBelowButtons,
                            onCheckedChange = { viewModel.setSeekBarBelowButtons(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = primaryColor,
                                checkedTrackColor = primaryColor.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Playback speeds selection row
                    Text(
                        text = "Manually Change Playback Speed",
                        style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { s ->
                            TrackChip(
                                selected = playbackSpeed == s,
                                label = "${s}x",
                                activeColor = primaryColor,
                                onClick = {
                                    playbackSpeed = s
                                    exoPlayer.setPlaybackSpeed(s)
                                    isBottomSheetOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Track properties helpers classes for Media3 selectors
data class AudioTrackInfo(
    val format: Format,
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

data class SubtitleTrackInfo(
    val format: Format,
    val group: Tracks.Group,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

fun getAvailableAudioTracks(tracks: Tracks): List<AudioTrackInfo> {
    val audioTracks = mutableListOf<AudioTrackInfo>()
    for (group in tracks.groups) {
        if (group.type == C.TRACK_TYPE_AUDIO) {
            val trackGroup = group.mediaTrackGroup
            for (i in 0 until trackGroup.length) {
                if (group.isTrackSupported(i)) {
                    val format = trackGroup.getFormat(i)
                    val lang = format.language ?: "unknown"
                    val label = format.label ?: lang
                    audioTracks.add(
                        AudioTrackInfo(
                            format = format,
                            group = group,
                            trackIndex = i,
                            label = label,
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
            }
        }
    }
    return audioTracks
}

fun getAvailableSubtitleTracks(tracks: Tracks): List<SubtitleTrackInfo> {
    val subTracks = mutableListOf<SubtitleTrackInfo>()
    for (group in tracks.groups) {
        if (group.type == C.TRACK_TYPE_TEXT) {
            val trackGroup = group.mediaTrackGroup
            for (i in 0 until trackGroup.length) {
                if (group.isTrackSupported(i)) {
                    val format = trackGroup.getFormat(i)
                    val lang = format.language ?: "unknown"
                    val label = format.label ?: lang
                    subTracks.add(
                        SubtitleTrackInfo(
                            format = format,
                            group = group,
                            trackIndex = i,
                            label = label,
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
            }
        }
    }
    return subTracks
}

fun selectTrack(exoPlayer: ExoPlayer, group: Tracks.Group, trackIndex: Int) {
    try {
        val override = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun disableSubtitles(exoPlayer: ExoPlayer) {
    try {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun enableSubtitles(exoPlayer: ExoPlayer) {
    try {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun TrackChip(
    selected: Boolean,
    label: String,
    activeColor: Color = NeonGreen,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) activeColor.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
            .border(
                1.5.dp,
                if (selected) activeColor else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) activeColor else Color.White
        )
    }
}

fun formatPosition(positionMs: Long): String {
    val totalSeconds = positionMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun getUriForPath(context: Context, path: String): android.net.Uri {
    if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("content://")) {
        return android.net.Uri.parse(path)
    }
    val file = java.io.File(path)
    if (file.exists()) {
        return android.net.Uri.fromFile(file)
    }
    return android.net.Uri.parse(path)
}
