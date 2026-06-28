package com.tvhanan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvhanan.di.ServiceLocator
import com.tvhanan.ui.settings.SettingsViewModel
import com.tvhanan.domain.model.TvDevice
import com.tvhanan.ui.manual.ManualConnectScreen
import com.tvhanan.ui.remote.RemoteScreen
import com.tvhanan.ui.remote.RemoteViewModel
import com.tvhanan.ui.scan.ScanScreen
import com.tvhanan.ui.scan.ScanViewModel
import com.tvhanan.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object Routes {
    const val SCAN = "scan"
    const val MANUAL = "manual"
    const val REMOTE = "remote/{ip}/{port}"
    const val SETTINGS = "settings"

    fun remoteRoute(device: TvDevice) = "remote/${device.ipAddress}/${device.port}"
    // Selaraskan nilai bawaan port ke 8002 demi konsistensi koneksi terenkripsi
    fun remoteRoute(ip: String, port: Int = 8002) = "remote/$ip/$port"
}

/**
 * Navigasi grafis utama aplikasi remote TV.
 */
@Composable
fun TvRemoteNavGraph(
    navController: NavHostController,
    serviceLocator: ServiceLocator,
    onExitApp: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // Buat SettingsViewModel secara resmi diikat pada tingkat NavHost (Singleton Sejati)
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(serviceLocator.preferences, serviceLocator.discoveryService) as T
            }
        }
    )

    val startRoute by androidx.compose.runtime.produceState<String?>(initialValue = null) {
        val savedIp = serviceLocator.preferences.lastIp.first()
        value = if (savedIp != null) {
            // Ubah fallback port bawaan ke 8002 agar selaras dengan prioritas WSS
            val savedPort = serviceLocator.preferences.lastPort.first()?.toIntOrNull() ?: 8002
            Routes.remoteRoute(savedIp, savedPort)
        } else {
            Routes.SCAN
        }
    }

    if (startRoute == null) return

    NavHost(navController = navController, startDestination = startRoute!!) {
        composable(Routes.SCAN) {
            val viewModel: ScanViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return ScanViewModel(serviceLocator.discoveryService, serviceLocator.preferences) as T
                    }
                }
            )
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
                navArgument("port") { type = NavType.IntType; defaultValue = 8002 } // Selaraskan ke 8002
            )
        ) { backStackEntry ->
            val ip = backStackEntry.arguments?.getString("ip") ?: return@composable
            val port = backStackEntry.arguments?.getInt("port") ?: 8002 // Selaraskan ke 8002

            val viewModel: RemoteViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return RemoteViewModel(ip, port, null, com.tvhanan.data.network.TvWebSocketClient(), serviceLocator.preferences) as T
                    }
                }
            )

            val connectionStateForSync by viewModel.connectionState.collectAsStateWithLifecycle()

            // Sinkronisasi awal: ip, digabung dengan sinkronisasi token - single LaunchedEffect
            androidx.compose.runtime.LaunchedEffect(ip, port, connectionStateForSync) {
                val mac = serviceLocator.preferences.macAddress.first()
                val token = serviceLocator.preferences.getToken()
                settingsViewModel.setActiveDevice(
                    ipAddress = ip,
                    port = port,
                    macAddress = mac,
                    token = token,
                    isConnected = connectionStateForSync == com.tvhanan.domain.model.ConnectionState.CONNECTED
                )
                
                // Observe new token changes
                viewModel.observeNewToken { newToken ->
                    val currentMac = serviceLocator.preferences.macAddress.first()
                    settingsViewModel.setActiveDevice(
                        ipAddress = ip,
                        port = port,
                        macAddress = currentMac,
                        token = newToken,
                        isConnected = true
                    )
                }
            }

            val uiPrefs by settingsViewModel.uiPreferences.collectAsStateWithLifecycle()
            val deviceName = settingsViewModel.tvDevice.collectAsStateWithLifecycle().value?.name ?: "Samsung TV"

            RemoteScreen(
                viewModel = viewModel,
                onOpenSettings = {
                    navController.navigate(Routes.SETTINGS) {
                        launchSingleTop = true
                    }
                },
                scaleFactor = uiPrefs.remoteSize.scaleFactor,
                keepScreenOn = uiPrefs.keepScreenOn,
                hapticEnabled = uiPrefs.hapticEnabled,
                meshBackgroundEnabled = uiPrefs.meshBackgroundEnabled,
                deviceName = deviceName
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = settingsViewModel,
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