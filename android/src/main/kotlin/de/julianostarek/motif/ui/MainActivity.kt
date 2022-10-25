package de.julianostarek.motif.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import de.julianostarek.motif.login.LoginViewModel
import de.julianostarek.motif.ui.login.AndroidLoginViewModel
import de.julianostarek.motif.ui.player.AndroidPlayerViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val loginViewModel: AndroidLoginViewModel by viewModels()
    private val playerViewModel: AndroidPlayerViewModel by viewModels()

    private val appleIntentResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data?.let { intent ->
            playerViewModel.shared.playerNegotiation.appleMusicAuthentication.handleIntent(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This app draws behind the system bars, so we want to handle fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val callbackUri = intent?.data?.takeIf { it.scheme == "motif" }
        callbackUri?.let {
            loginViewModel.loginFromCallback(it.toString())
        }

        setContent {
            MotifTheme {
                MotifUi()
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                println("pre collect")
                playerViewModel.playerConnectIntent()
                    .collect { intent ->
                        println("collect intent")
                        appleIntentResult.launch(intent)
                    }
            }
        }
    }
}
