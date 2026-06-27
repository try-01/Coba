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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle // TAMBAHKAN INI
import com.tvhanan.di.ServiceLocator
import com.tvhanan.ui.settings.SettingsViewModel // TAMBAHKAN INI
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
    
    // Buat SettingsViewModel secara legal & resmi dari Compose, menjadikannya
    // Shared ViewModel yang bertahan selama NavGraph (App) hidup
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(serviceLocator.preferences, serviceLocator.discoveryService) as T
            }
        }
    )

    // Cek apakah ada TV tersimpan dari sesi sebelumnya — kalau ada, langsung
    // buka RemoteScreen-nya supaya user tidak perlu scan/manual ulang setiap
    // kali masuk app. Dibungkus produceState supaya pengecekan DataStore
    // (operasi suspend) tidak memblokir Composable pertama kali digambar.
    val startRoute by androidx.compose.runtime.produceState<String?>(initialValue = null) {
        val savedIp = serviceLocator.preferences.lastIp.first()
        value = if (savedIp != null) {
            val savedPort = serviceLocator.preferences.lastPort.first()?.toIntOrNull() ?: 8001
            Routes.remoteRoute(savedIp, savedPort)
        } else {
            Routes.SCAN
        }
    }

    // Selama startRoute belum ditentukan (masih null), tampilkan layar
    // kosong sebentar daripada flicker ke ScanScreen dulu baru pindah.
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
                navArgument("port") { type = NavType.IntType; defaultValue = 8001 }
            )
        ) { backStackEntry ->
            val ip = backStackEntry.arguments?.getString("ip") ?: return@composable
            val port = backStackEntry.arguments?.getInt("port") ?: 8001

            val viewModel: RemoteViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return RemoteViewModel(ip, port, null, com.tvhanan.data.network.TvWebSocketClient(), serviceLocator.preferences) as T
                    }
                }
            )

            // Sinkronkan device aktif ke SettingsViewModel begitu IP/port
            // diketahui, lalu sinkronkan ULANG begitu status koneksi berubah
            // jadi CONNECTED (saat itu token & MAC tersimpan sudah pasti final
            // di DataStore, karena RemoteViewModel.connect() baru menulis token
            // setelah pairing sukses).
            val connectionStateForSync by viewModel.connectionState.collectAsStateWithLifecycle()

            // Sinkronisasi awal: begitu ip/port/status koneksi berubah.
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
            }

            // Sinkronisasi TAMBAHAN: token bisa tersimpan ke DataStore BEBERAPA
            // SAAT SETELAH connectionState sudah CONNECTED (TV baru kirim event
            // token setelah user approve popup pairing). LaunchedEffect di atas
            // tidak tahu kapan ini terjadi karena connectionState tidak berubah
            // lagi setelah CONNECTED. Effect ini secara eksplisit menunggu
            // event token baru dari WebSocketClient, lalu re-sync.
            androidx.compose.runtime.LaunchedEffect(ip, port) {
                viewModel.observeNewToken { newToken ->
                    val mac = serviceLocator.preferences.macAddress.first()
                    settingsViewModel.setActiveDevice(
                        ipAddress = ip,
                        port = port,
                        macAddress = mac,
                        token = newToken,
                        isConnected = true
                    )
                }
            }

            // remoteSize dibaca dari SettingsViewModel singleton supaya preferensi
            // ukuran tampilan tetap sinkron walau RemoteScreen di-recreate
            // (mis. setelah kembali dari Settings, atau setelah rotasi layar).
            val uiPrefs by settingsViewModel.uiPreferences.collectAsStateWithLifecycle()

RemoteScreen(
    viewModel = viewModel,
    onOpenSettings = {
        navController.navigate(Routes.SETTINGS) {
            launchSingleTop = true // Mencegah halaman ditumpuk ganda jika tombol terkena double-tap
        }
    },
    scaleFactor = uiPrefs.remoteSize.scaleFactor,
    keepScreenOn = uiPrefs.keepScreenOn,
    hapticEnabled = uiPrefs.hapticEnabled,                 // SALURKAN STATE GETAR DI SINI
    meshBackgroundEnabled = uiPrefs.meshBackgroundEnabled   // SALURKAN STATE AURORA DI SINI
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
