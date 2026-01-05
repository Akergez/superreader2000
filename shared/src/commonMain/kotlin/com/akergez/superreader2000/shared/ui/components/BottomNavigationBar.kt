package com.akergez.superreader2000.shared.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

