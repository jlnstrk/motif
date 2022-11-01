//
//  FeedViewModelShim.swift
//  ios
//
//  Created by Julian Ostarek on 02.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import Shared
import Combine
import KMPNativeCoroutinesCombine

class FeedViewModelShim: ObservableObject {
    let shared: Shared.FeedViewModel = Shared.FeedViewModel()

    @Published var feedState: Shared.FeedState = Shared.FeedState.NotLoading()

    private var cancellables: [AnyCancellable] = []

    init() {
        createPublisher(for: shared.stateNative)
            .assertNoFailure()
            .receive(on: DispatchQueue.main)
            .assign(to: \.feedState, on: self)
            .store(in: &cancellables)
    }
}
