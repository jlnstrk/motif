//
//  PlayerViewModel.swift
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
import Shared
import UIKit
import KMPNativeCoroutinesCore
import KMPNativeCoroutinesCombine
import Combine

class PlayerViewModelShim: ObservableObject {
    let shared = Shared.PlayerViewModel()

    @Published var frontendState: FrontendState = FrontendState.Disconnected()
    @Published var trackImage: UIImage?

    private var cancellables: [AnyCancellable] = []

    func authorizeFromUrl(url: URL) {
        shared.playerNegotiation.spotifyConnector.authorizeFromUrl(url: url)
    }
    
    init() {
        createPublisher(for: shared.frontendStateNative)
            .assertNoFailure()
            .receive(on: DispatchQueue.main)
            .assign(to: \.frontendState, on: self)
            .store(in: &cancellables)
        
        createPublisher(for: shared.frontendStateNative)
            .assertNoFailure()
            .map { ($0 as? Shared.FrontendState.ConnectedPlayback)?.track }
            .removeDuplicates { old, new in old?.url == new?.url }
            .debounce(for: 0.25, scheduler: RunLoop.main, options: nil)
            .sink { [weak self] track in
                if let track = track {
                    self?.shared.playerOrNull()?.platform.trackImage(track: track, size: 256) { [weak self] (image, error) in
                        self?.trackImage = image
                    }
                }
            }
            .store(in: &cancellables)
    }
}
