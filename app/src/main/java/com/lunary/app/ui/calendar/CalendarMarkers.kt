package com.lunary.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lunary.app.ui.components.LetterAccent

/**
 * 日期两侧的标记：左侧为芳札细竖线，右侧为纪念日圆点。
 * 两侧占位固定，保证中间日期文字始终水平居中。
 */
@Composable
internal fun DateMarkers(
    selected: Boolean,
    muted: Boolean,
    hasLetter: Boolean,
    eventColors: List<Int>,
    modifier: Modifier = Modifier,
    maxDots: Int = 7,
    sideSlotWidth: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(sideSlotWidth),
            contentAlignment = Alignment.Center
        ) {
            if (hasLetter) {
                LetterDateLine(selected = selected, muted = muted)
            }
        }
        content()
        Box(
            modifier = Modifier.width(sideSlotWidth),
            contentAlignment = Alignment.Center
        ) {
            if (eventColors.isNotEmpty()) {
                EventDots(colors = eventColors, maxDots = maxDots)
            }
        }
    }
}

@Composable
private fun LetterDateLine(selected: Boolean, muted: Boolean) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(12.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    muted -> LetterAccent.copy(alpha = 0.45f)
                    else -> LetterAccent
                }
            )
    )
}

@Composable
private fun EventDots(colors: List<Int>, maxDots: Int) {
    val visibleColors = colors.take(maxDots)
    val dotSize = if (visibleColors.size > 5) 3.dp else 4.dp
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        visibleColors.forEach { argb ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(
                        if (argb != 0) Color(argb) else MaterialTheme.colorScheme.primary
                    )
            )
        }
    }
}
