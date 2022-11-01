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

package de.julianostarek.motif.login

import de.julianostarek.motif.SharedViewModel
import de.julianostarek.motif.client.auth.Service
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class LoginViewModel : SharedViewModel(), KoinComponent {
    private val loginRepository: LoginRepository by inject()

    private val _state: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.NotDetermined)
    val state: StateFlow<LoginState> get() = _state

    init {
        viewModelScope.launch {
            loginRepository.auth.collect { auth ->
                _state.value = if (auth != null) LoginState.LoggedIn else LoginState.LoggedOut
            }
        }
    }

    fun loginUrl(service: Service): String = loginRepository.loginUrl(service)

    fun loginFromCallback(callbackUrl: String) = viewModelScope.launch {
        loginRepository.loginFromCallback(callbackUrl)
    }
}