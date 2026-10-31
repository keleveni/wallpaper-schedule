package com.keleveni.wallpaperscheduler.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.keleveni.wallpaperscheduler.R
import com.keleveni.wallpaperscheduler.WallpaperSchedulerApp
import com.keleveni.wallpaperscheduler.ui.schedules.ScheduleEditScreen
import com.keleveni.wallpaperscheduler.ui.schedules.ScheduleEditViewModel
import com.keleveni.wallpaperscheduler.ui.schedules.ScheduleListScreen
import com.keleveni.wallpaperscheduler.ui.settings.SettingsScreen
import com.keleveni.wallpaperscheduler.ui.theme.WallpaperSchedulerTheme
import com.keleveni.wallpaperscheduler.ui.wallpapers.WallpaperListScreen
import com.keleveni.wallpaperscheduler.ui.wallpapers.WallpaperPreviewScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // アラームが何らかの理由で失われていても、アプリを開いた時点で設定し直す
        lifecycleScope.launch { (application as WallpaperSchedulerApp).container.scheduler.sync() }
        setContent {
            WallpaperSchedulerTheme { AppNavigation() }
        }
    }
}

private enum class Tab(val route: String, val icon: ImageVector, val label: Int) {
    Schedules("schedules", Icons.Default.Schedule, R.string.nav_schedules),
    Wallpapers("wallpapers", Icons.Default.Wallpaper, R.string.nav_wallpapers),
    Settings("settings", Icons.Default.Settings, R.string.nav_settings),
}

private const val EDIT_ROUTE = "schedule/{${ScheduleEditViewModel.ARG_SCHEDULE_ID}}"

private fun editRoute(id: Long) = "schedule/$id"

private const val ARG_WALLPAPER_ID = "wallpaperId"
private const val PREVIEW_ROUTE = "wallpaper/{$ARG_WALLPAPER_ID}"

private fun previewRoute(id: Long) = "wallpaper/$id"

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = Tab.entries.any { it.route == currentRoute }

    fun selectTab(tab: Tab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { selectTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Schedules.route,
            // 各画面の Scaffold が上部のインセットを扱うため、下部のみ反映する
            modifier = Modifier
                .padding(bottom = padding.calculateBottomPadding())
                .consumeWindowInsets(PaddingValues(bottom = padding.calculateBottomPadding())),
        ) {
            composable(Tab.Schedules.route) {
                ScheduleListScreen(
                    onAddSchedule = { navController.navigate(editRoute(ScheduleEditViewModel.NEW_SCHEDULE_ID)) },
                    onEditSchedule = { navController.navigate(editRoute(it)) },
                    onOpenSettings = { selectTab(Tab.Settings) },
                )
            }
            composable(Tab.Wallpapers.route) {
                WallpaperListScreen(onOpenWallpaper = { navController.navigate(previewRoute(it)) })
            }
            composable(Tab.Settings.route) { SettingsScreen() }
            composable(
                route = EDIT_ROUTE,
                arguments = listOf(navArgument(ScheduleEditViewModel.ARG_SCHEDULE_ID) { type = NavType.LongType }),
            ) {
                ScheduleEditScreen(onClose = { navController.popBackStack() })
            }
            composable(
                route = PREVIEW_ROUTE,
                arguments = listOf(navArgument(ARG_WALLPAPER_ID) { type = NavType.LongType }),
            ) { entry ->
                WallpaperPreviewScreen(
                    wallpaperId = entry.arguments!!.getLong(ARG_WALLPAPER_ID),
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}
