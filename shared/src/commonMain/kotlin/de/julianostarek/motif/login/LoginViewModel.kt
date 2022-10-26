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

    private val _state: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.LoggedOut)
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