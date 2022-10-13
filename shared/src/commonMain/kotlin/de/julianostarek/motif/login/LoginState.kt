package de.julianostarek.motif.login

sealed interface LoginState {
    object LoggedOut : LoginState
    object LoggedIn : LoginState
}