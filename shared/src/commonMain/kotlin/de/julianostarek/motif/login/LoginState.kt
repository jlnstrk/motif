package de.julianostarek.motif.login

sealed class LoginState {
    object NotDetermined : LoginState()
    object LoggedOut : LoginState()
    object LoggedIn : LoginState()
}