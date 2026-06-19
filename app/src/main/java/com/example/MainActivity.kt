package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.InstanceConfig
import com.example.data.TrackRepository
import com.example.player.LocalTrackPlayer
import com.example.ui.HomeDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var player: LocalTrackPlayer
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 0. Initialize instance configuration
        InstanceConfig.init(this)

        // 1. Initialize persistent Room Database layer
        val database = AppDatabase.getDatabase(this)
        val repository = TrackRepository(database.trackDao())

        // 2. Initialize native MediaPlayer helper
        player = LocalTrackPlayer(this)

        // 3. Build and instantiate MainViewModel utilizing custom Factory
        val factory = MainViewModelFactory(application, repository, player)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeDashboard(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release Media player securely to prevent system memory leaks
        if (::player.isInitialized) {
            player.release()
        }
    }
}

/**
 * Custom ViewModelFactory to pass repositories and players safely into ViewModel
 */
class MainViewModelFactory(
    private val application: Application,
    private val repository: TrackRepository,
    private val player: LocalTrackPlayer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository, player) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class parsing MainViewModel")
    }
}
