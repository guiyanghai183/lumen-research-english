package com.lumen.researchenglish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.lumen.researchenglish.ui.AppViewModel
import com.lumen.researchenglish.ui.LumenApp
import com.lumen.researchenglish.ui.theme.LumenTheme

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LumenTheme {
                LumenApp(viewModel = appViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appViewModel.onAppForegrounded()
    }

    override fun onStop() {
        appViewModel.onAppBackgrounded()
        super.onStop()
    }
}
