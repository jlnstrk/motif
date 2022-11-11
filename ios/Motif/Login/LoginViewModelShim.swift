//
//  LoginViewModelShim.swift
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

import Foundation
import Combine
import Shared
import KMPNativeCoroutinesCombine

class LoginViewModelShim: ObservableObject {
    let shared: Shared.LoginViewModel = Shared.LoginViewModel()

    @Published var loginState: Shared.LoginState = .NotDetermined()

    private var cancellables: [AnyCancellable] = []

    init() {
        createPublisher(for: shared.stateNative)
            .assertNoFailure()
            .receive(on: DispatchQueue.main)
            .assign(to: \.loginState, on: self)
            .store(in: &cancellables)
    }
}
