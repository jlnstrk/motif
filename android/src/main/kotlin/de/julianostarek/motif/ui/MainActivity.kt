package de.julianostarek.motif.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import de.julianostarek.motif.login.LoginViewModel

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This app draws behind the system bars, so we want to handle fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val callbackUri = intent?.data?.takeIf { it.scheme == "motif" }
        callbackUri?.let {
            loginViewModel.loginFromCallback(it.toString())
        }

        setContent {
            OscillyTheme {
                MotifUi()
            }
        }
    }
}
