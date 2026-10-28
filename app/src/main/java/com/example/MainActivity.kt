package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.SolitaireScreen
import com.example.ui.theme.SolitaireTheme
import com.example.viewmodel.SolitaireViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SolitaireViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.ads.AdMobManager.initialize(this)
        setContent {
            val state by viewModel.uiState.collectAsState()
            SolitaireTheme(darkTheme = state.settings.isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SolitaireScreen(viewModel = viewModel)
                }
            }
        }
    }
}
