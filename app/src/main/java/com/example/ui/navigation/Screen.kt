package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Dashboard : Screen("dashboard")
    object Recorder : Screen("recorder")
    object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: String) = "note_detail/$noteId"
    }
    object Chat : Screen("chat/{noteId}") {
        fun createRoute(noteId: String) = "chat/$noteId"
    }
    object Edit : Screen("edit/{noteId}") {
        fun createRoute(noteId: String) = "edit/$noteId"
    }
    object Settings : Screen("settings")
    object Trash : Screen("trash")
}
