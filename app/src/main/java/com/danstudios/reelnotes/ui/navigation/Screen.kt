package com.danstudios.reelnotes.ui.navigation

sealed class Screen(val route: String) {
    object NotesList : Screen("notes_list")
    object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: Long) = "note_detail/$noteId"
    }
    object Settings : Screen("settings")
}
