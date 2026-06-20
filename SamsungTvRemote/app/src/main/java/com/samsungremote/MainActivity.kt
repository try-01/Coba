package com.samsungremote

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsungremote.ui.RemoteScreen
import com.samsungremote.ui.SamsungRemoteTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val app = application as SamsungRemoteApp
        val logger = app.logger
        logger.i("Act", "onCreate (savedInstanceState=${savedInstanceState != null})")

        setContent {
            val viewModel: RemoteViewModel = viewModel(
                factory = RemoteViewModelFactory(app)
            )

            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.viewModelEvents.collect { event ->
                    when (event) {
                        ViewModelEvent.ExitApp -> {
                            logger.i("Act", "ExitApp event — finishing task")
                            finishAndRemoveTask()
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                    }
                }
            }

            SamsungRemoteTheme {
                RemoteScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val logger = (application as SamsungRemoteApp).logger
        logger.d("Act", "onDestroy")
    }
}
