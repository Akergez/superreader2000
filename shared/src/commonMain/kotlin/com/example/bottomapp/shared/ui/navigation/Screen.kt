package com.example.bottomapp.shared.ui.navigation

enum class Screen {
    HOME_SCREEN,
    BOOK_SCREEN,
    SETTINGS_SCREEN
}

sealed class Route(val route: String) {
    data object Main : Route("main")
    data object Settings : Route("settings")
    data class BookReader(val fileName: String) : Route("bookReader/{fileName}") {
        fun createRoute(fileName: String) = "bookReader/$fileName"
    }
}

