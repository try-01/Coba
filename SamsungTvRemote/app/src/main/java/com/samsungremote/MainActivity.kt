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

        setContent {
            val viewModel: RemoteViewModel = viewModel(
                factory = RemoteViewModelFactory(app)
            )

            // Collect one-shot events (e.g. deep exit)
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.viewModelEvents.collect { event ->
                    when (event) {
                        ViewModelEvent.ExitApp -> finishAndRemoveTask()
                    }
                }
            }

            SamsungRemoteTheme {
                RemoteScreen(viewModel = viewModel)
            }
        }
    }
}
