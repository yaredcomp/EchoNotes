package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.AiIntelligenceRepositoryImpl
import com.example.data.repository.NoteRepositoryImpl
import com.example.ui.chat.ChatWithNoteScreen
import com.example.ui.chat.ChatWithNoteViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.detail.NoteDetailScreen
import com.example.ui.detail.NoteDetailViewModel
import com.example.ui.edit.EditTranscriptScreen
import com.example.ui.edit.EditTranscriptViewModel
import com.example.ui.navigation.Screen
import com.example.ui.recorder.RecorderScreen
import com.example.ui.recorder.RecorderViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.EchoNotesTheme
import com.example.ui.theme.ThemeManager
import com.example.ui.trash.TrashScreen
import com.example.ui.trash.TrashViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ThemeManager.init(applicationContext)
        com.example.data.ai.GeminiAiClient.init(applicationContext)

        val noteRepository = NoteRepositoryImpl(applicationContext)
        val aiRepository = AiIntelligenceRepositoryImpl()

        setContent {
            EchoNotesTheme(darkTheme = ThemeManager.isDarkTheme) {
                EchoNotesApp(
                    noteRepository = noteRepository,
                    aiRepository = aiRepository
                )
            }
        }
    }
}

@Composable
fun EchoNotesApp(
    noteRepository: NoteRepositoryImpl,
    aiRepository: AiIntelligenceRepositoryImpl
) {
    val navController = rememberNavController()

    androidx.compose.runtime.DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, _, _ ->
            com.example.data.audio.GlobalAudioPlayer.stopAndRelease()
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
            com.example.data.audio.GlobalAudioPlayer.stopAndRelease()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(320))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = tween(280))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(320))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut(animationSpec = tween(280))
        }
    ) {
        // 0. Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 1. Dashboard Screen (Google Stitch Ultra-Dark / Clean Light Design)
        composable(Screen.Dashboard.route) {
            val viewModel = remember { DashboardViewModel(navController.context, noteRepository) }
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToRecord = { navController.navigate(Screen.Recorder.route) },
                onNavigateToDetail = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        // 2. Audio Recorder Screen (Live Speech Streaming + Fallback Engine)
        composable(Screen.Recorder.route) {
            val viewModel = remember {
                RecorderViewModel(
                    context = navController.context,
                    noteRepository = noteRepository,
                    aiRepository = aiRepository
                )
            }
            RecorderScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId)) {
                        popUpTo(Screen.Recorder.route) { inclusive = true }
                    }
                }
            )
        }

        // 3. Note Detail Screen
        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            val viewModel = remember(noteId) {
                NoteDetailViewModel(
                    context = navController.context,
                    noteId = noteId,
                    noteRepository = noteRepository,
                    aiRepository = aiRepository
                )
            }
            NoteDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.Edit.createRoute(id))
                },
                onNavigateToChat = { id ->
                    navController.navigate(Screen.Chat.createRoute(id))
                }
            )
        }

        // 4. Chat With Note Screen (Dedicated Isolated Note Chat)
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            val viewModel = remember(noteId) {
                ChatWithNoteViewModel(
                    noteId = noteId,
                    noteRepository = noteRepository,
                    aiRepository = aiRepository
                )
            }
            ChatWithNoteScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. Edit Transcript Screen
        composable(
            route = Screen.Edit.route,
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            val viewModel = remember(noteId) {
                EditTranscriptViewModel(
                    noteId = noteId,
                    noteRepository = noteRepository,
                    aiRepository = aiRepository
                )
            }
            EditTranscriptScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 6. Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTrash = { navController.navigate(Screen.Trash.route) }
            )
        }

        // 7. Trash Collection Screen
        composable(Screen.Trash.route) {
            val viewModel = remember { TrashViewModel(noteRepository) }
            TrashScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Screen.NoteDetail.createRoute(id)) }
            )
        }
    }
}
