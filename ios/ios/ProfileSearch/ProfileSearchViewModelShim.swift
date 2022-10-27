//
//  ProfileSearchViewModelShim.swift
//  ios
//
//  Created by Julian Ostarek on 27.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import Combine
import KMPNativeCoroutinesCombine
import Shared

class ProfileSearchViewModelShim: ObservableObject {
    let shared: Shared.ProfileSearchViewModel = Shared.ProfileSearchViewModel()

    @Published var query: String = ""
    @Published var state: Shared.ProfileSearchState = .NoQuery()

    private var cancellables: [AnyCancellable] = []

    init() {
        createPublisher(for: shared.stateNative)
            .assertNoFailure()
            .receive(on: DispatchQueue.main)
            .assign(to: \.state, on: self)
            .store(in: &cancellables)

        $query
            .flatMap { query in
                let submitQuery: String? = query.count == 0 ? nil : query
                return createPublisher(for: self.shared.setQueryNative(query: submitQuery))
                    .assertNoFailure()
            }
            .sink(receiveValue: { _ in })
            .store(in: &cancellables)
    }
}
