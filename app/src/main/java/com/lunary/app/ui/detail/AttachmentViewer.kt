package com.lunary.app.ui.detail

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lunary.app.R
import com.lunary.app.domain.model.Attachment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Full-screen, in-app attachment viewer.
 *
 * The list itself is a horizontal pager so a swipe moves between every attachment of the event.
 * Images are decoded at a bounded resolution and fit to the window; video and audio stay inside
 * the app instead of launching an external player.
 */
@Composable
internal fun AttachmentViewerDialog(
    attachments: List<Attachment>,
    initialIndex: Int,
    fileFor: (Attachment) -> File?,
    contentUri: (Attachment) -> Uri?,
    onDismiss: () -> Unit
) {
    if (attachments.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, attachments.lastIndex),
        pageCount = { attachments.size }
    )
    val currentPage = pagerState.currentPage.coerceIn(0, attachments.lastIndex)
    val current = attachments[currentPage]
    val view = LocalView.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindowProvider = view.parent as? DialogWindowProvider
        DisposableEffect(dialogWindowProvider) {
            dialogWindowProvider?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                WindowInsetsControllerCompat(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
            onDispose { }
        }

        BackHandler(onBack = onDismiss)
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0
            ) { page ->
                val attachment = attachments[page]
                AttachmentViewerPage(
                    attachment = attachment,
                    file = remember(attachment.id) { fileFor(attachment) },
                    uri = remember(attachment.id) { contentUri(attachment) }
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.82f), Color.Black.copy(alpha = 0.42f))
                        )
                    )
                    .statusBarsPadding()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.attachment_viewer_close),
                        tint = Color.White
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = current.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatBytes(current.sizeBytes),
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
                Text(
                    text = stringResource(
                        R.string.attachment_viewer_position,
                        currentPage + 1,
                        attachments.size
                    ),
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun AttachmentViewerPage(
    attachment: Attachment,
    file: File?,
    uri: Uri?
) {
    when {
        attachment.mimeType.startsWith("image/") -> ImageViewerPage(attachment, file)
        attachment.mimeType.startsWith("video/") -> VideoViewerPage(attachment, uri)
        attachment.mimeType.startsWith("audio/") -> AudioViewerPage(attachment, uri)
        else -> UnsupportedViewerPage(attachment, uri)
    }
}

@Composable
private fun ImageViewerPage(attachment: Attachment, file: File?) {
    val imageState by produceState<ImageLoadState>(ImageLoadState.Loading, file?.path) {
        value = if (file == null) {
            ImageLoadState.Failed
        } else {
            withContext(Dispatchers.IO) {
                decodeBoundedImage(file)?.let(ImageLoadState::Ready) ?: ImageLoadState.Failed
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val state = imageState) {
            ImageLoadState.Loading -> CircularProgressIndicator(color = Color.White)
            ImageLoadState.Failed -> ViewerMessage(
                icon = Icons.Default.Warning,
                text = stringResource(R.string.attachment_image_error)
            )
            is ImageLoadState.Ready -> Image(
                bitmap = state.bitmap.asImageBitmap(),
                contentDescription = attachment.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun VideoViewerPage(attachment: Attachment, uri: Uri?) {
    if (uri == null) {
        ViewerMessage(
            icon = Icons.Default.Warning,
            text = stringResource(R.string.attachment_media_error)
        )
        return
    }

    val context = LocalContext.current
    var prepared by remember(uri) { mutableStateOf(false) }
    var playing by remember(uri) { mutableStateOf(false) }
    var duration by remember(uri) { mutableIntStateOf(0) }
    var position by remember(uri) { mutableIntStateOf(0) }
    var failed by remember(uri) { mutableStateOf(false) }

    val videoView = remember(uri) {
        VideoView(context).apply {
            setOnPreparedListener { player ->
                duration = player.duration.coerceAtLeast(0)
                prepared = true
            }
            setOnCompletionListener {
                playing = false
                position = duration
            }
            setOnErrorListener { _, _, _ ->
                failed = true
                prepared = false
                playing = false
                true
            }
            setVideoURI(uri)
        }
    }

    DisposableEffect(videoView) {
        onDispose { videoView.stopPlayback() }
    }
    LaunchedEffect(playing) {
        while (playing) {
            position = videoView.currentPosition
            delay(300)
        }
    }

    val playPauseLabel = stringResource(
        if (playing) R.string.attachment_pause else R.string.attachment_play
    )

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { videoView },
            modifier = Modifier.fillMaxSize()
        )

        when {
            failed -> ViewerMessage(
                icon = Icons.Default.Warning,
                text = stringResource(R.string.attachment_media_error)
            )
            !prepared -> CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> FilledIconButton(
                onClick = {
                    if (playing) {
                        videoView.pause()
                        playing = false
                    } else {
                        videoView.start()
                        playing = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .semantics { contentDescription = playPauseLabel },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.58f),
                    contentColor = Color.White
                )
            ) {
                if (playing) {
                    PauseGlyph(
                        color = Color.White,
                        modifier = Modifier.size(width = 20.dp, height = 26.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        if (prepared && !failed) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Slider(
                    value = position.toFloat().coerceIn(
                        0f,
                        duration.toFloat().coerceAtLeast(1f)
                    ),
                    onValueChange = { value ->
                        position = value.toInt()
                        videoView.seekTo(position)
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatPlaybackTime(position),
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatPlaybackTime(duration),
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioViewerPage(attachment: Attachment, uri: Uri?) {
    if (uri == null) {
        ViewerMessage(
            icon = Icons.Default.Warning,
            text = stringResource(R.string.attachment_media_error)
        )
        return
    }

    val context = LocalContext.current
    val player = remember(uri) { MediaPlayer() }
    var prepared by remember(uri) { mutableStateOf(false) }
    var playing by remember(uri) { mutableStateOf(false) }
    var duration by remember(uri) { mutableIntStateOf(0) }
    var position by remember(uri) { mutableIntStateOf(0) }
    var failed by remember(uri) { mutableStateOf(false) }

    DisposableEffect(player) {
        onDispose {
            runCatching { player.release() }
        }
    }
    LaunchedEffect(uri) {
        runCatching {
            player.setDataSource(context, uri)
            player.setOnPreparedListener { mediaPlayer ->
                duration = mediaPlayer.duration.coerceAtLeast(0)
                prepared = true
            }
            player.setOnCompletionListener {
                playing = false
                position = duration
            }
            player.setOnErrorListener { _, _, _ ->
                failed = true
                prepared = false
                playing = false
                true
            }
            player.prepareAsync()
        }.onFailure {
            failed = true
        }
    }
    LaunchedEffect(playing) {
        while (playing) {
            runCatching { position = player.currentPosition }
            delay(300)
        }
    }

    val playPauseLabel = stringResource(
        if (playing) R.string.attachment_pause else R.string.attachment_play
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 96.dp),
        contentAlignment = Alignment.Center
    ) {
        if (failed) {
            ViewerMessage(
                icon = Icons.Default.Warning,
                text = stringResource(R.string.attachment_media_error)
            )
            return@Box
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.size(156.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "♪",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 64.sp,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = attachment.displayName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.attachment_audio_label),
                color = Color.White.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(28.dp))
            FilledIconButton(
                onClick = {
                    if (!prepared) return@FilledIconButton
                    if (playing) {
                        player.pause()
                        playing = false
                    } else {
                        player.start()
                        playing = true
                    }
                },
                enabled = prepared,
                modifier = Modifier
                    .size(64.dp)
                    .semantics { contentDescription = playPauseLabel },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                if (!prepared) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    if (playing) {
                        PauseGlyph(
                            color = Color.Black,
                            modifier = Modifier.size(width = 18.dp, height = 24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Slider(
                value = position.toFloat().coerceIn(
                    0f,
                    duration.toFloat().coerceAtLeast(1f)
                ),
                onValueChange = { value ->
                    position = value.toInt()
                    player.seekTo(position)
                },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                enabled = prepared,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPlaybackTime(position),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatPlaybackTime(duration),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun UnsupportedViewerPage(attachment: Attachment, uri: Uri?) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = attachment.displayName,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.attachment_unsupported),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium
        )
        if (uri != null) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, attachment.mimeType)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.attachment_open_external))
            }
        }
    }
}

@Composable
private fun PauseGlyph(color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}

@Composable
private fun ViewerMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data class Ready(val bitmap: Bitmap) : ImageLoadState
    data object Failed : ImageLoadState
}

private fun decodeBoundedImage(file: File, maxDimension: Int = 2048): Bitmap? =
    decodeOrientedBoundedImage(file, maxDimension)

private fun formatPlaybackTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0) / 1000)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
