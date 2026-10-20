package com.xinyu.yuweng.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.xinyu.yuweng.R
import com.xinyu.yuweng.core.designsystem.theme.DaiGray
import com.xinyu.yuweng.core.designsystem.theme.FogGray
import com.xinyu.yuweng.core.designsystem.theme.WarmGold

/**
 * 底部导航 4 Tab。
 * 当前 Tab 暖阳金+文字，其他仅图标雾灰，无角标。
 */
data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    unselectedIcon: ImageVector
)

val navTabs = listOf(
    NavTab(Routes.CHAT, R.string.tab_chat, Icons.Filled.Chat, Icons.Outlined.Chat),
    NavTab(Routes.MEMORY, R.string.tab_memory, Icons.Filled.Person, Icons.Outlined.Person),
    NavTab(Routes.STATUS, R.string.tab_status, Icons.Filled.Person, Icons.Outlined.Person),
    NavTab(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun YuwengBottomBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(
        containerColor = Color.Transparent
    ) {
        navTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        color = if (selected) WarmGold else FogGray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WarmGold,
                    unselectedIconColor = FogGray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
