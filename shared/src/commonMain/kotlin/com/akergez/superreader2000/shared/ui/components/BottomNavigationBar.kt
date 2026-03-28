package com.akergez.superreader2000.shared.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.akergez.superreader2000.shared.constants.Route
import com.akergez.superreader2000.shared.ui.screens.Screen
import org.jetbrains.compose.resources.stringResource
import superreader2000.shared.generated.resources.Res
import superreader2000.shared.generated.resources.title_home
import superreader2000.shared.generated.resources.title_settings

@Composable
fun BottomNavigationBar(
    navController: NavController,
    screen: Screen
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(Res.string.title_home)) },
            selected = screen == Screen.HOME_SCREEN,
            onClick = {
                if (navController.currentDestination?.route != Route.Main) {
                    navController.navigate(Route.Main) {
                        popUpTo(Route.Settings) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null
                )
            },
            label = {
                Text(
                    stringResource(
                        Res.string.title_settings
                    )
                )
            },
            selected = screen == Screen.SETTINGS_SCREEN,
            onClick = {
                if (navController.currentDestination?.route != Route.Settings) {
                    navController.navigate(Route.Settings) {
                        launchSingleTop = true
                    }
                }
            }
        )
    }
}

@Composable
fun AppNavigationRail(
    navController: NavController,
    screen: Screen,
    modifier: Modifier = Modifier
) {
    NavigationRail(modifier = modifier.fillMaxHeight()) {
        Spacer(Modifier.weight(1f))

        NavigationRailItem(
            selected = screen == Screen.HOME_SCREEN,
            onClick = {
                if (navController.currentDestination?.route != Route.Main) {
                    navController.navigate(Route.Main) {
                        popUpTo(Route.Settings) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(Res.string.title_home)) }
        )
        Spacer(Modifier.height(8.dp))
        NavigationRailItem(
            selected = screen == Screen.SETTINGS_SCREEN,
            onClick = {
                if (navController.currentDestination?.route != Route.Settings) {
                    navController.navigate(Route.Settings) { launchSingleTop = true }
                }
            },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text(stringResource(Res.string.title_settings)) }
        )

        Spacer(Modifier.weight(1f))
    }
}
