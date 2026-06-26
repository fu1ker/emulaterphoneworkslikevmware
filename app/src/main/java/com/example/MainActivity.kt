package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    
                    // Core state wiring using local constructor injection
                    val database = remember { AppDatabase.getDatabase(context) }
                    val repository = remember { AppRepository(database.appDao()) }
                    val appViewModel: AppViewModel = viewModel(
                        factory = AppViewModelFactory(repository)
                    )

                    val navStack by appViewModel.navigationStack.collectAsState()

                    if (navStack.isEmpty()) {
                        HomeScreen(
                            viewModel = appViewModel,
                            onAppClick = { app ->
                                appViewModel.pushApp(app)
                            }
                        )
                    } else {
                        val activeApp = navStack.last()
                        
                        // Handle physical back button presses correctly
                        BackHandler {
                            appViewModel.popApp()
                        }

                        WorkspaceScreen(
                            app = activeApp,
                            viewModel = appViewModel,
                            onBack = {
                                appViewModel.popApp()
                            }
                        )
                    }
                }
            }
        }
    }
}
