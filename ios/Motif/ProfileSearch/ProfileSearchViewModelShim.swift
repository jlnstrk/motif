//
//  ProfileSearchViewModelShim.swift
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

class ProfileSearchViewModelShim: ObservableObject {
    let shared: Shared.ProfileSearchViewModel = Shared.ProfileSearchViewModel()

    @Published var query: String = ""
    @Published var state: Shared.ProfileSearchState = .NoQuery()

    private var cancellables: [AnyCancellable] = []

    init() {
        createPublisher(for: shared.stateFlow)
            .assertNoFailure()
            .receive(on: DispatchQueue.main)
            .assign(to: \.state, on: self)
            .store(in: &cancellables)

        $query
            .flatMap { query in
                let submitQuery: String? = query.count == 0 ? nil : query
                return createFuture(for: self.shared.setQuery(query: submitQuery))
                    .assertNoFailure()
            }
            .sink(receiveValue: { _ in })
            .store(in: &cancellables)
    }
}
