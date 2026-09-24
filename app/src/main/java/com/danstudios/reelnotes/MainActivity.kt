package com.danstudios.reelnotes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.danstudios.reelnotes.ui.navigation.Screen
import com.danstudios.reelnotes.ui.screens.NoteDetailScreen
import com.danstudios.reelnotes.ui.screens.NotesListScreen
import com.danstudios.reelnotes.ui.screens.SettingsScreen
import com.danstudios.reelnotes.ui.theme.ReelNotesTheme
import com.danstudios.reelnotes.ui.viewmodel.ReelNotesViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val app by lazy { application as ReelNotesApp }
    private val viewModel: ReelNotesViewModel by viewModels {
        ReelNotesViewModel.Factory(app.repository, app.preferences)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            ReelNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Automatically navigate to note details when a new note is processed
                    LaunchedEffect(Unit) {
                        viewModel.newlyCreatedNoteId.collectLatest { noteId ->
                            navController.navigate(Screen.NoteDetail.createRoute(noteId))
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.NotesList.route
                    ) {
                        composable(Screen.NotesList.route) {
                            NotesListScreen(
                                viewModel = viewModel,
                                onNoteClick = { noteId ->
                                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                                },
                                onSettingsClick = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }

                        composable(
                            route = Screen.NoteDetail.route,
                            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                            NoteDetailScreen(
                                noteId = noteId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)

            if (!sharedText.isNullOrBlank()) {
                viewModel.processSharedUrl(sharedText)
            }
        }
    }
}
