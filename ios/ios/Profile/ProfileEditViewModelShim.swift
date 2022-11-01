//
//  ProfileEditViewModelShim.swift
//  ios
//
//  Created by Julian Ostarek on 01.11.22.
//  Copyright © 2022 orgName. All rights reserved.
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
