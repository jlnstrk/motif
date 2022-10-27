//
//  LoginViewModelShim.swift
//  ios
//
//  Created by Julian Ostarek on 02.10.22.
//  Copyright © 2022 orgName. All rights reserved.
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
