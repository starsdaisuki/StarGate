package com.stardaisuki.stargate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stardaisuki.stargate.ui.screens.DebugScreen
import com.stardaisuki.stargate.ui.screens.EditProfileScreen
import com.stardaisuki.stargate.ui.screens.HomeScreen
import com.stardaisuki.stargate.ui.theme.StarGateTheme

class MainActivity : ComponentActivity() {

    // 注册权限请求回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 权限结果不需要特殊处理，有没有都能用 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 请求定位权限（获取 Wi-Fi SSID 需要）
        requestLocationPermission()

        setContent {
            StarGateTheme {
                StarGateApp()
            }
        }
    }

    private fun requestLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needRequest) {
            requestPermissionLauncher.launch(permissions)
        }
    }
}

@Composable
fun StarGateApp() {
    val viewModel: MainViewModel = viewModel()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToEdit = { profileId ->
                    if (profileId != null) {
                        navController.navigate("edit/$profileId")
                    } else {
                        navController.navigate("edit/new")
                    }
                },
                onNavigateToDebug = {
                    navController.navigate("debug")
                }
            )
        }

        composable("debug") {
            DebugScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "edit/{profileId}",
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId")
            EditProfileScreen(
                profileId = if (profileId == "new") null else profileId,
                viewModel = viewModel,
                onSave = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
