//
//  ProfileEditViewModelShim.swift
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
import KMPNativeCoroutinesCombine
import Shared

class ProfileEditViewModelShim : ObservableObject {
    let shared = Shared.ProfileEditViewModel()

    @Published var displayName: String = ""
    @Published var username: String = ""
    @Published var biography: String = ""

    @Published var usernameAvailability: ProfileUsernameAvailability = .isOwn
    @Published var submissionStatus: ProfileEditSubmissionStatus = .submitted
    @Published var canSubmit: Bool = false

    private var cancellables: [AnyCancellable] = []

    init() {
        Publishers.CombineLatest3($displayName, $username, $biography)
            .map { displayName, username, biography in
                ProfileEdit(displayName: displayName, username: username, biography: biography)
            }
            .sink(receiveValue: { [weak self] input in
                self?.shared.setInput(input: input)
            })
            .store(in: &cancellables)

        createPublisher(for: shared.usernameAvailabilityNative)
            .assertNoFailure()
            .receive(on: RunLoop.main)
            .assign(to: \.usernameAvailability, on: self)
            .store(in: &cancellables)

        createPublisher(for: shared.submissionStatusNative)
            .assertNoFailure()
            .receive(on: RunLoop.main)
            .assign(to: \.submissionStatus, on: self)
            .store(in: &cancellables)

        createPublisher(for: shared.canSubmitNative)
            .assertNoFailure()
            .receive(on: RunLoop.main)
            .map { value in value.boolValue }
            .assign(to: \.canSubmit, on: self)
            .store(in: &cancellables)

        createPublisher(for: shared.userInputNative)
            .assertNoFailure()
            .receive(on: RunLoop.main)
            .sink(receiveValue: { [weak self] input in
                self?.displayName = input?.displayName ?? ""
                self?.username = input?.username ?? ""
                self?.biography = input?.biography ?? ""
            })
            .store(in: &cancellables)
    }
}
