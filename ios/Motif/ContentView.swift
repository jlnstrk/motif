//
//  ContentView.swift
//
//  Copyright 2022 Julian Ostarek
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

import SwiftUI
import Shared

struct ContentView: View {
    @StateObject var loginViewModel: LoginViewModelShim = LoginViewModelShim()

    var body: some View {
        // LoginView(viewModel: loginViewModel)
        switch loginViewModel.loginState {
        case is Shared.LoginState.LoggedIn:
            MainView()
        case is Shared.LoginState.LoggedOut:
            LoginView(viewModel: loginViewModel)
        default:
            EmptyView()
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
