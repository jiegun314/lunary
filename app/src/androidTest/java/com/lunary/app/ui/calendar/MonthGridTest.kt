package com.lunary.app.ui.calendar

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lunary.app.ui.theme.LunaryTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MonthGridTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun selectedDayExposesEventCountToAccessibility() {
        val selected = LocalDate.of(2026, 11, 13)
        compose.setContent {
            LunaryTheme {
                MonthGrid(
                    month = YearMonth.of(2026, 11),
                    selectedDate = selected,
                    today = selected,
                    eventColorsByDate = mapOf(selected to listOf(0xFFE91E63.toInt(), 0xFF2196F3.toInt())),
                    firstDayOfWeek = DayOfWeek.MONDAY,
                    cellSize = 52.dp,
                    onDateClick = {}
                )
            }
        }
        compose.onNodeWithContentDescription("2026年11月13日，2 个纪念日").assertExists()
    }

    @Test
    fun selectedDayExposesLetterToAccessibility() {
        val selected = LocalDate.of(2026, 11, 13)
        compose.setContent {
            LunaryTheme {
                MonthGrid(
                    month = YearMonth.of(2026, 11),
                    selectedDate = selected,
                    today = selected,
                    eventColorsByDate = emptyMap(),
                    firstDayOfWeek = DayOfWeek.MONDAY,
                    cellSize = 52.dp,
                    onDateClick = {},
                    letterDates = setOf(selected)
                )
            }
        }
        compose.onNodeWithContentDescription("2026年11月13日，有芳札").assertExists()
    }
}
