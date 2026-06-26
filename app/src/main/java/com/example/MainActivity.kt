package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.screens.AgreementScreen
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

                    val sharedPrefs = remember(context) {
                        context.getSharedPreferences("applet_security_prefs", android.content.Context.MODE_PRIVATE)
                    }
                    var hasAgreed by remember {
                        mutableStateOf(sharedPrefs.getBoolean("has_agreed", false))
                    }

                    val navStack by appViewModel.navigationStack.collectAsStateWithLifecycle()

                    AnimatedContent(
                        targetState = hasAgreed,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.98f)).togetherWith(
                                fadeOut(animationSpec = tween(200))
                            )
                        },
                        label = "AgreementTransition"
                    ) { agreed ->
                        if (!agreed) {
                            AgreementScreen(
                                onAgree = {
                                    sharedPrefs.edit().putBoolean("has_agreed", true).apply()
                                    hasAgreed = true
                                }
                            )
                        } else {
                            AnimatedContent(
                                targetState = navStack,
                                transitionSpec = {
                                    if (targetState.size > initialState.size) {
                                        // Deeper into nesting - subtle slide and fast fade
                                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> (width * 0.15f).toInt() } + fadeIn(animationSpec = tween(220))).togetherWith(
                                            slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> -(width * 0.15f).toInt() } + fadeOut(animationSpec = tween(180))
                                        )
                                    } else {
                                        // Popping back up - subtle slide and fast fade
                                        (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> -(width * 0.15f).toInt() } + fadeIn(animationSpec = tween(220))).togetherWith(
                                            slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> (width * 0.15f).toInt() } + fadeOut(animationSpec = tween(180))
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
    }
}
