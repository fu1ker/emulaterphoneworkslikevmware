package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
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

                    AnimatedContent(
                        targetState = navStack,
                        transitionSpec = {
                            if (targetState.size > initialState.size) {
                                // Deeper into nesting - slide content to the left
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width / 4 } + fadeOut()
                                )
                            } else {
                                // Popping back up - slide content to the right
                                (slideInHorizontally { width -> -width / 4 } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "ScreenTransition"
                    ) { currentStack ->
                        if (currentStack.isEmpty()) {
                            HomeScreen(
                                viewModel = appViewModel,
                                onAppClick = { app ->
                                    appViewModel.pushApp(app)
                                }
                            )
                        } else {
                            val activeApp = currentStack.last()
                            
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
}
