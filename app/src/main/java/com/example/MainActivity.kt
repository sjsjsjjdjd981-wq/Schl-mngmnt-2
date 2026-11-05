package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.screens.AddStudentScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChangeCoachingYearScreen
import com.example.ui.screens.FeeManagementScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PdfClaimScreen
import com.example.ui.screens.SearchStudentScreen
import com.example.ui.screens.SetupCoachingScreen
import com.example.ui.screens.StudentProfileScreen
import com.example.ui.screens.YourClassesScreen
import com.example.ui.screens.YourStudentsScreen
import com.example.ui.theme.CoachingAppTheme

sealed class Screen {
    object Home : Screen()
    object FeeManagement : Screen()
    object YourStudents : Screen()
    object SetupCoaching : Screen()
    object YourClasses : Screen()
    object AddStudent : Screen()
    object SearchStudent : Screen()
    data class StudentProfile(val studentId: String) : Screen()
    object PdfClaim : Screen()
    object ChangeCoachingYear : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "FirebaseApp init check failed", e)
        }

        try {
            enableEdgeToEdge()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "enableEdgeToEdge skipped or failed on this device", e)
        }

        setContent {
            CoachingAppTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                val message by viewModel.message.collectAsState()

                // Show toast message if any
                LaunchedEffect(message) {
                    message?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearMessage()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentUser == null) {
                        AuthScreen(
                            onAuthSuccess = {
                                // User signed in
                            }
                        )
                    } else {
                        MainAppContent(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Home

    fun navigateTo(screen: Screen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }

    // Intercept back button if on sub-screens
    if (backStack.size > 1) {
        BackHandler {
            navigateBack()
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            is Screen.Home -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToFeeManagement = { navigateTo(Screen.FeeManagement) },
                    onNavigateToYourStudents = { navigateTo(Screen.YourStudents) },
                    onNavigateToSetupCoaching = { navigateTo(Screen.SetupCoaching) },
                    onNavigateToPdfClaim = { navigateTo(Screen.PdfClaim) },
                    onNavigateToChangeCoachingYear = { navigateTo(Screen.ChangeCoachingYear) }
                )
            }

            is Screen.FeeManagement -> {
                FeeManagementScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onStudentClick = { student ->
                        navigateTo(Screen.StudentProfile(student.id))
                    }
                )
            }

            is Screen.YourStudents -> {
                YourStudentsScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onNavigateToAddStudent = { navigateTo(Screen.AddStudent) },
                    onNavigateToSearchStudent = { navigateTo(Screen.SearchStudent) }
                )
            }

            is Screen.SetupCoaching -> {
                SetupCoachingScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onNavigateToYourClasses = { navigateTo(Screen.YourClasses) }
                )
            }

            is Screen.YourClasses -> {
                YourClassesScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }

            is Screen.AddStudent -> {
                AddStudentScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }

            is Screen.SearchStudent -> {
                SearchStudentScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onStudentClick = { student ->
                        navigateTo(Screen.StudentProfile(student.id))
                    }
                )
            }

            is Screen.StudentProfile -> {
                StudentProfileScreen(
                    studentId = screen.studentId,
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }

            is Screen.PdfClaim -> {
                PdfClaimScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }

            is Screen.ChangeCoachingYear -> {
                ChangeCoachingYearScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }
        }
    }
}
