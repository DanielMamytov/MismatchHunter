package com.example.mismatchhunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mismatchhunter.di.AppContainer
import com.example.mismatchhunter.ui.navigation.AppNav
import com.example.mismatchhunter.ui.theme.MismatchHunterTheme
import com.example.mismatchhunter.ui.viewmodel.AppViewModelFactory
import com.example.mismatchhunter.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = remember { AppContainer(applicationContext) }
            val settingsVm: SettingsViewModel = viewModel(factory = AppViewModelFactory(container.settingsRepository, container.sessionRepository, container.episodeRepository, container.noteRepository))
            val settings by settingsVm.settings.collectAsState()
            val accent = if (settings.accentColor == "lightBlue") Color(0xFF6CB8FF) else Color(0xFF2474FF)
            MismatchHunterTheme(darkTheme = settings.darkTheme, accent = accent) {
                AppNav(container)
            }
        }
    }
}
