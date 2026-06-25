package com.tvhanan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tvhanan.di.ServiceLocator
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.ui.manual.ManualConnectScreen
import com.tvhanan.ui.remote.RemoteScreen
import com.tvhanan.ui.remote.RemoteViewModel
import com.tvhanan.ui.scan.ScanScreen
import com.tvhanan.ui.scan.ScanViewModel
import com.tvhanan.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

object Routes {
    const val SCAN = "scan"
    const val MANUAL = "manual"
    const val REMOTE = "remote/{ip}/{port}"
    const val SETTINGS = "settings"

    fun remoteRoute(device: TvDevice) = "remote/${device.ipAddress}/${device.port}"
    fun remoteRoute(ip: String, port: Int = 8001) = "remote/$ip/$port"
}

/**
 * @param onExitApp dipanggil saat user menekan "Keluar dari aplikasi" di
 *   Settings dan mengonfirmasi. Diteruskan dari MainActivity (biasanya
 *   memanggil Activity.finish()) supaya NavGraph/SettingsScreen tidak
 *   perlu memegang referensi ke Activity context secara langsung.
 */
@Composable
fun TvRemoteNavGraph(
    navController: NavHostController,
    serviceLocator: ServiceLocator,
    onExitApp: () -> Unit
) {
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = Routes.SCAN) {
        composable(Routes.SCAN) {
            val viewModel = remember {
                ScanViewModel(
                    discoveryService = serviceLocator.discoveryService,
                    preferences = serviceLocator.preferences
                )
            }
            ScanScreen(
                viewModel = viewModel,
                onDeviceSelected = { device ->
                    scope.launch {
                        serviceLocator.preferences.saveLastIp(device.ipAddress)
                        serviceLocator.preferences.saveLastPort(device.port.toString())
                    }
                    navController.navigate(Routes.remoteRoute(device))
                },
                onManualConnect = {
                    navController.navigate(Routes.MANUAL)
                }
            )
        }

        composable(Routes.MANUAL) {
            ManualConnectScreen(
                onConnect = { device ->
                    scope.launch {
                        serviceLocator.preferences.saveLastIp(device.ipAddress)
                        serviceLocator.preferences.saveLastPort(device.port.toString())
                        device.macAddress?.let { serviceLocator.preferences.saveMacAddress(it) }
                    }
                    navController.navigate(Routes.remoteRoute(device)) {
                        popUpTo(Routes.SCAN)
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.REMOTE,
            arguments = listOf(
                navArgument("ip") { type = NavType.StringType },
                navArgument("port") { type = NavType.IntType; defaultValue = 8001 }
            )
        ) { backStackEntry ->
            val ip = backStackEntry.arguments?.getString("ip") ?: return@composable
            val port = backStackEntry.arguments?.getInt("port") ?: 8001

            val viewModel = remember(ip, port) {
                RemoteViewModel(
                    ipAddress = ip,
                    port = port,
                    macAddress = null,
                    preferences = serviceLocator.preferences
                )
            }

            // Sinkronkan device aktif ke SettingsViewModel supaya TvInfoCard
            // langsung akurat, tidak menunggu race condition DataStore.
            androidx.compose.runtime.LaunchedEffect(ip, port) {
                serviceLocator.settingsViewModel.setActiveDevice(ip, port, null)
            }

            // remoteSize dibaca dari SettingsViewModel singleton supaya preferensi
            // ukuran tampilan tetap sinkron walau RemoteScreen di-recreate
            // (mis. setelah kembali dari Settings, atau setelah rotasi layar).
            val uiPrefs by serviceLocator.settingsViewModel.uiPreferences.collectAsState()

            RemoteScreen(
                viewModel = viewModel,
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                scaleFactor = uiPrefs.remoteSize.scaleFactor
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = serviceLocator.settingsViewModel,
                onBack = {
                    navController.popBackStack()
                },
                onManualConnect = { typedIp ->
                    navController.navigate(Routes.remoteRoute(typedIp)) {
                        popUpTo(Routes.SCAN)
                    }
                },
                onForgetAndExitToScan = {
                    navController.navigate(Routes.SCAN) {
                        popUpTo(0)
                    }
                },
                onExitApp = onExitApp
            )
        }
    }
}
