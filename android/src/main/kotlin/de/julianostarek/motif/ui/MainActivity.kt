/*
 * Copyright 2022 Julian Ostarek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
