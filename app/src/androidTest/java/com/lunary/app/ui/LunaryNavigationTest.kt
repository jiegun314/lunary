package com.lunary.app.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.lunary.app.MainActivity
import org.junit.Rule
import org.junit.Test

class LunaryNavigationTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigation_opensEventsAndSettings() {
        compose.onNodeWithText("纪念日").performClick()
        compose.onNodeWithText("搜索标题、备注、地点").assertExists()
        compose.onNodeWithContentDescription("设置").performClick()
        compose.onNodeWithText("外观").assertExists()
    }

    @Test
    fun editor_requiresTitleBeforeSave() {
        compose.onNodeWithContentDescription("添加纪念日").performClick()
        compose.onNodeWithText("新建纪念日").assertExists()
        compose.onNodeWithContentDescription("保存").assertIsNotEnabled()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("UI test event")
        compose.onNodeWithContentDescription("保存").assertIsEnabled()
    }
}
