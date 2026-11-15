package com.lunary.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lunary.app.R

private val NotoSansScRegular = FontFamily(Font(R.font.noto_sans_sc_regular, FontWeight.Normal))
private val NunitoMedium = FontFamily(Font(R.font.nunito_medium, FontWeight.Medium))

@Composable
fun BrandLockup(
    iconSize: Dp = 36.dp,
    titleSize: TextUnit = 19.sp,
    subtitleSize: TextUnit = 11.sp,
    iconTextGap: Dp = 7.dp
) {
    val dark = isSystemInDarkTheme()
    val chineseColor = if (dark) Color(0xFFEDE7F6) else Color(0xFF302B3B)
    val latinColor = if (dark) Color(0xFFD2CADF) else Color(0xFF4F485C)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_brand_logo),
            contentDescription = null,
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(iconTextGap))
        Column(
            modifier = Modifier.heightIn(min = iconSize),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontFamily = NotoSansScRegular,
                fontWeight = FontWeight.Normal,
                fontSize = titleSize,
                lineHeight = 20.sp,
                letterSpacing = 0.2.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                color = chineseColor
            )
            Text(
                text = stringResource(R.string.app_name_en),
                fontFamily = NunitoMedium,
                fontWeight = FontWeight.Medium,
                fontSize = subtitleSize,
                lineHeight = 12.sp,
                letterSpacing = 0.1.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                color = latinColor
            )
        }
    }
}
