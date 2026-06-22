package com.tvhanan.ui.navigation

import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch

object Routes {
    const val SCAN = "scan"
    const val MANUAL = "manual"
    const val REMOTE = "remote/{ip}/{port}"

    fun remoteRoute(device: TvDevice) = "remote/${device.ipAddress}/${device.port}"
}

@Composable
fun TvRemoteNavGraph(
    navController: NavHostController,
    serviceLocator: ServiceLocator
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
                        serviceLocator.preferences?.saveLastIp(device.ipAddress)
                        serviceLocator.preferences?.saveLastPort(device.port.toString())
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
                        serviceLocator.preferences?.saveLastIp(device.ipAddress)
                        serviceLocator.preferences?.saveLastPort(device.port.toString())
                        device.macAddress?.let { serviceLocator.preferences?.saveMacAddress(it) }
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
            RemoteScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.disconnect()
                    navController.popBackStack()
                }
            )
        }
    }
}
