package com.lunary.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lunary.app.R

/**
 * Shared top bar for the three top-level destinations.
 *
 * The destination name is always the title. On compact widths the brand logo
 * keeps app identity without taking the title slot; on expanded widths the
 * brand lives in the NavigationRail header instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunaryTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val heightDp = with(density) { windowInfo.containerSize.height.toDp() }
    val compact = widthDp < 600.dp || heightDp < 480.dp
    val appName = stringResource(R.string.app_name)
    val logoSize = 28.dp * density.fontScale.coerceIn(1f, 1.3f)
    val titleStyle = MaterialTheme.typography.titleMedium
    val wordmarkStyle = titleStyle.copy(
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    val pageTitleStyle = (if (compact) {
        titleStyle
    } else {
        MaterialTheme.typography.titleLarge
    }).copy(
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
    )
    val separatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    val titleContent: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = if (compact) "$appName，$title" else title
            }
        ) {
            if (compact) {
                Image(
                    painter = painterResource(R.drawable.ic_brand_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(logoSize)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = appName,
                    style = wordmarkStyle,
                    maxLines = 1
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(separatorColor)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = title,
                style = pageTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    TopAppBar(
        modifier = modifier,
        title = titleContent,
        actions = actions
    )
}
