package com.lunary.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunary.app.R
import com.lunary.app.domain.model.Letter
import com.lunary.app.ui.formatDate
import com.lunary.app.ui.formatTime

/** Lavender card tint, kept distinct from the neutral event cards in both themes. */
@Composable
fun letterCardColor(): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val base = if (dark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    return lerp(
        base,
        MaterialTheme.colorScheme.primaryContainer,
        if (dark) 0.45f else 0.35f
    )
}

@Composable
fun LetterRow(
    letter: Letter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDate: Boolean = true
) {
    val timestamp = if (showDate) {
        "${formatDate(letter.date)} · ${formatTime(letter.timeMinutes)}"
    } else {
        formatTime(letter.timeMinutes)
    }
    val fromTo = stringResource(
        R.string.letter_from_to,
        letter.sender,
        letter.recipient
    )
    val iconSize = with(LocalDensity.current) { 18.sp.toDp() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = letterCardColor(),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(LetterAccent)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = letter.subject,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(color = MaterialTheme.colorScheme.primary)
                        ) {
                            append(timestamp)
                        }
                        withStyle(
                            SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            append(" · $fromTo")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
