package com.xinyu.yuweng

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.xinyu.yuweng.core.designsystem.theme.YuwengTheme
import com.xinyu.yuweng.navigation.YuwengBottomBar
import com.xinyu.yuweng.navigation.YuwengNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YuwengTheme {
                YuwengAppScreen()
            }
        }
    }
}

@Composable
fun YuwengAppScreen() {
    val navController = rememberNavController()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { YuwengBottomBar(navController) }
    ) { innerPadding ->
        YuwengNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
