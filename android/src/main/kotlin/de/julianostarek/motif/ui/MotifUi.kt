package de.julianostarek.motif.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.julianostarek.motif.login.LoginState
import de.julianostarek.motif.ui.feed.Feed
import de.julianostarek.motif.ui.player.Player
import de.julianostarek.motif.ui.login.Login
import de.julianostarek.motif.ui.login.AndroidLoginViewModel
import de.julianostarek.motif.ui.player.AndroidPlayerViewModel

@Composable
fun MotifUi(
    uiState: MotifUiState = rememberMotifUiState(),
    loginViewModel: AndroidLoginViewModel = viewModel(),
    playerViewModel: AndroidPlayerViewModel = viewModel()
) {
    val loginState = loginViewModel.state.collectAsState()
    if (loginState.value == LoginState.LoggedIn) {
        Scaffold(
            bottomBar = {
                BottomNavigation {
                    BottomNavigationItem(
                        selected = uiState.navController.currentDestination?.route == Screen.Feed.route,
                        onClick = { uiState.navController.navigate(Screen.Feed.route) },
                        icon = { Icon(Icons.Rounded.ViewList, contentDescription = null) }
                    )
                    BottomNavigationItem(
                        selected = uiState.navController.currentDestination?.route == Screen.Player.route,
                        onClick = { uiState.navController.navigate(Screen.Player.route) },
                        icon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) }
                    )
                }
            }
        ) {
            NavHost(
                navController = uiState.navController,
                startDestination = Screen.Feed.route,
                modifier = Modifier.padding(it)
            ) {
                composable(Screen.Feed.route) {
                    Feed(playerViewModel)
                }
                composable(Screen.Player.route) {
                    Player(playerViewModel)
                }
            }
        }
    } else {
        Login(loginViewModel)
    }
}