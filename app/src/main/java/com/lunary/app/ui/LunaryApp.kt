package com.lunary.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lunary.app.R
import com.lunary.app.ui.backup.BackupScreen
import com.lunary.app.ui.backup.AutoBackupScreen
import com.lunary.app.ui.calendar.CalendarScreen
import com.lunary.app.ui.detail.DetailScreen
import com.lunary.app.ui.editor.EditorScreen
import com.lunary.app.ui.events.EventsScreen
import com.lunary.app.ui.letters.LetterDetailScreen
import com.lunary.app.ui.letters.LetterEditorScreen
import com.lunary.app.ui.letters.LettersScreen
import com.lunary.app.ui.settings.SettingsScreen
import com.lunary.app.ui.taxonomy.CategoryScreen
import com.lunary.app.ui.trash.TrashScreen
import com.lunary.app.ui.taxonomy.TagScreen

private data class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int
)

private val topLevelDestinations = listOf(
    TopLevelDestination("calendar", Icons.Default.DateRange, R.string.nav_calendar),
    TopLevelDestination("events", Icons.Default.List, R.string.nav_events),
    TopLevelDestination("letters", Icons.Default.Email, R.string.nav_letters)
)

@Composable
fun LunaryApp(
    initialEventId: String? = null,
    onInitialEventConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    LaunchedEffect(initialEventId) {
        if (initialEventId != null) {
            navController.navigate("detail/$initialEventId")
            onInitialEventConsumed()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showNavigation = topLevelDestinations.any { it.route == currentRoute }
    val settingsTree = currentRoute == "settings" || currentRoute == "backup" || currentRoute == "auto_backup" ||
        currentRoute == "trash" || currentRoute == "categories" || currentRoute == "tags"

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 600.dp
        val railHasRoom = maxHeight >= 480.dp
        val showRail = expanded && (showNavigation || settingsTree)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showNavigation && !expanded) {
                    NavigationBar {
                        topLevelDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = { navigateTopLevel(destination.route) },
                                icon = {
                                    Icon(destination.icon, contentDescription = null)
                                },
                                label = { Text(stringResource(destination.labelRes)) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                if (showRail) {
                    NavigationRail(modifier = Modifier.width(88.dp)) {
                        if (railHasRoom) {
                            BrandRailHeader()
                        }
                        topLevelDestinations.forEach { destination ->
                            NavigationRailItem(
                                selected = currentRoute == destination.route,
                                onClick = { navigateTopLevel(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.labelRes)) }
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        NavigationRailItem(
                            selected = settingsTree,
                            onClick = {
                                navController.navigate("settings") { launchSingleTop = true }
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_settings)) }
                        )
                    }
                }
                NavHost(
                    navController = navController,
                    startDestination = "calendar",
                    modifier = Modifier.weight(1f)
                ) {
                    composable("calendar") {
                        CalendarScreen(
                            onAddEvent = { date ->
                                navController.navigate("editor?date=${date.toEpochDay()}")
                            },
                            onEventClick = { id -> navController.navigate("detail/$id") },
                            onAddLetter = { date ->
                                navController.navigate("letter_editor?date=${date.toEpochDay()}")
                            },
                            onLetterClick = { id -> navController.navigate("letter_detail/$id") },
                            onOpenSettings = {
                                navController.navigate("settings") { launchSingleTop = true }
                            }
                        )
                    }
                    composable("events") {
                        EventsScreen(
                            onAddEvent = { navController.navigate("editor") },
                            onEventClick = { id -> navController.navigate("detail/$id") },
                            onEditEvent = { id -> navController.navigate("editor?eventId=$id") },
                            onOpenSettings = {
                                navController.navigate("settings") { launchSingleTop = true }
                            }
                        )
                    }
                    composable("letters") {
                        LettersScreen(
                            onAddLetter = { navController.navigate("letter_editor") },
                            onLetterClick = { id -> navController.navigate("letter_detail/$id") },
                            onOpenSettings = {
                                navController.navigate("settings") { launchSingleTop = true }
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onOpenTrash = { navController.navigate("trash") },
                            onOpenBackup = { navController.navigate("backup") },
                            onOpenAutoBackup = { navController.navigate("auto_backup") },
                            onManageCategories = { navController.navigate("categories") },
                            onManageTags = { navController.navigate("tags") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "detail/{eventId}",
                        arguments = listOf(navArgument("eventId") { type = NavType.StringType })
                    ) {
                        DetailScreen(
                            onBack = { navController.popBackStack() },
                            onEdit = { id -> navController.navigate("editor?eventId=$id") }
                        )
                    }
                    composable(
                        route = "editor?eventId={eventId}&date={date}",
                        arguments = listOf(
                            navArgument("eventId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("date") {
                                type = NavType.LongType
                                defaultValue = Long.MIN_VALUE
                            }
                        )
                    ) {
                        EditorScreen(
                            onClose = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                            onDeleted = {
                                val previousRoute =
                                    navController.previousBackStackEntry?.destination?.route
                                navController.popBackStack()
                                if (previousRoute?.startsWith("detail/") == true) {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                    composable(
                        route = "letter_detail/{letterId}",
                        arguments = listOf(navArgument("letterId") { type = NavType.StringType })
                    ) {
                        LetterDetailScreen(
                            onBack = { navController.popBackStack() },
                            onEdit = { id ->
                                navController.navigate("letter_editor?letterId=$id")
                            }
                        )
                    }
                    composable(
                        route = "letter_editor?letterId={letterId}&date={date}",
                        arguments = listOf(
                            navArgument("letterId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("date") {
                                type = NavType.LongType
                                defaultValue = Long.MIN_VALUE
                            }
                        )
                    ) {
                        LetterEditorScreen(
                            onClose = { navController.popBackStack() },
                            onSaved = { navController.popBackStack() },
                            onDeleted = {
                                val previousRoute =
                                    navController.previousBackStackEntry?.destination?.route
                                navController.popBackStack()
                                if (previousRoute?.startsWith("letter_detail/") == true) {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                    composable("trash") {
                        TrashScreen(onBack = { navController.popBackStack() })
                    }
                    composable("backup") {
                        BackupScreen(onBack = { navController.popBackStack() })
                    }
                    composable("auto_backup") {
                        AutoBackupScreen(onBack = { navController.popBackStack() })
                    }
                    composable("categories") {
                        CategoryScreen(onBack = { navController.popBackStack() })
                    }
                    composable("tags") {
                        TagScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandRailHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_brand_logo),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
