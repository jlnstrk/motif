//
//  ProfileViewModelShim.swift
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
import KMPNativeCoroutinesAsync

class ProfileViewModelShim: ObservableObject {
    let shared: Shared.ProfileViewModel

    @Published var state: ProfileState

    private var cancellables: [AnyCancellable] = []

    init(for reference: ProfileReference?) {
        self.shared = Shared.ProfileViewModel(reference: reference)
        self.state = .NotLoaded(reference: reference)

        createPublisher(for: shared.stateFlow)
            .assertNoFailure()
            .receive(on: DispatchQueue.main)
            .assign(to: \.state, on: self)
            .store(in: &cancellables)
    }

    func refresh() async {
        let _ = await asyncResult(for: shared.refreshSuspending())
    }
}
