package com.example.bottomapp.shared.ui.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sealed class representing navigation routes.
 * Use URL-style routing for multiplatform compatibility.
 */
sealed class NavigationRoute {
    object Home : NavigationRoute()
    object Settings : NavigationRoute()
    data class PdfReader(val bookId: String) : NavigationRoute()
    data class Fb2Reader(val bookId: String) : NavigationRoute()
    data class EditBook(val bookId: String) : NavigationRoute()

    /**
     * Convert route to string representation
     */
    fun toRoute(): String = when (this) {
        is Home -> "home"
        is Settings -> "settings"
        is PdfReader -> "pdfReader/$bookId"
        is Fb2Reader -> "fb2Reader/$bookId"
        is EditBook -> "editBook/$bookId"
    }

    companion object {
        /**
         * Parse route string back to NavigationRoute
         */
        fun fromRoute(route: String): NavigationRoute = when {
            route == "home" -> Home
            route == "settings" -> Settings
            route.startsWith("pdfReader/") -> PdfReader(route.substringAfter("pdfReader/"))
            route.startsWith("fb2Reader/") -> Fb2Reader(route.substringAfter("fb2Reader/"))
            route.startsWith("editBook/") -> EditBook(route.substringAfter("editBook/"))
            else -> Home
        }
    }
}

/**
 * Navigation back stack entry
 */
data class BackStackEntry(
    val route: NavigationRoute,
    val id: String = generateId()
) {
    companion object {
        private var idCounter = 0
        private fun generateId(): String = (idCounter++).toString()
    }
}

/**
 * Koin-managed navigation state machine.
 * Manages the current route and back stack for the entire application.
 */
class NavigationState {
    private val _currentRoute = MutableStateFlow<NavigationRoute>(NavigationRoute.Home)
    val currentRoute: StateFlow<NavigationRoute> = _currentRoute.asStateFlow()

    private val _backStack = MutableStateFlow<List<BackStackEntry>>(emptyList())
    val backStack: StateFlow<List<BackStackEntry>> = _backStack.asStateFlow()

    private val stateScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Navigate to a new route, pushing current route to back stack
     */
    fun navigateTo(route: NavigationRoute) {
        val currentRoute = _currentRoute.value

        // Don't push if navigating to the same route
        if (currentRoute::class == route::class && currentRoute == route) {
            return
        }

        // Push current route to back stack
        _backStack.value = _backStack.value + BackStackEntry(currentRoute)
        _currentRoute.value = route
    }

    /**
     * Pop from back stack (go back)
     */
    fun popBackStack(): Boolean {
        val backStack = _backStack.value
        return if (backStack.isNotEmpty()) {
            val lastEntry = backStack.last()
            _currentRoute.value = lastEntry.route
            _backStack.value = backStack.dropLast(1)
            true
        } else {
            false
        }
    }

    /**
     * Clear back stack and navigate to route
     */
    fun navigateToAndClearStack(route: NavigationRoute) {
        _currentRoute.value = route
        _backStack.value = emptyList()
    }

    /**
     * Check if we can go back
     */
    fun canGoBack(): Boolean = _backStack.value.isNotEmpty()

    /**
     * Reset navigation to initial state
     */
    fun reset() {
        _currentRoute.value = NavigationRoute.Home
        _backStack.value = emptyList()
    }
}

