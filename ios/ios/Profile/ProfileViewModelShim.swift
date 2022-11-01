//
//  ProfileViewModelShim.swift
//  ios
//
//  Created by Julian Ostarek on 01.11.22.
//  Copyright © 2022 orgName. All rights reserved.
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

        createPublisher(for: shared.stateNative)
            .assertNoFailure()
            .receive(on: DispatchQueue.main)
            .assign(to: \.state, on: self)
            .store(in: &cancellables)
    }

    func refresh() async {
        let _ = await asyncResult(for: shared.refreshSuspendNative())
    }
}
