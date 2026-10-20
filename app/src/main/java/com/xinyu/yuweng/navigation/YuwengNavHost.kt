package com.xinyu.yuweng.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.xinyu.yuweng.feature.chat.ChatScreen
import com.xinyu.yuweng.feature.memory.MemoryScreen
import com.xinyu.yuweng.feature.settings.ForgettingCurveScreen
import com.xinyu.yuweng.feature.settings.SettingsScreen
import com.xinyu.yuweng.feature.status.StatusScreen

object Routes {
    const val CHAT = "chat"
    const val MEMORY = "memory"
    const val STATUS = "status"
    const val SETTINGS = "settings"
    const val FORGETTING_CURVE = "forgetting_curve"
}

@Composable
fun YuwengNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.CHAT,
        modifier = modifier
    ) {
        composable(Routes.CHAT) { ChatScreen() }
        composable(Routes.MEMORY) { MemoryScreen() }
        composable(Routes.STATUS) { StatusScreen() }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToForgettingCurve = {
                    navController.navigate(Routes.FORGETTING_CURVE)
                }
            )
        }
        composable(Routes.FORGETTING_CURVE) {
            ForgettingCurveScreen(onBack = { navController.popBackStack() })
        }
    }
}
