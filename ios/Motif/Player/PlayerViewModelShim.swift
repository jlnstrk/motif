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
import MediaPlayer

class PlayerViewModelShim: ObservableObject {
    let shared = Shared.PlayerViewModel()

    @Published var remoteState: Shared.RemoteState = Shared.RemoteState.Disconnected()
    @Published var trackImage: UIImage?

    @Published var isPresentingChooser: Bool = false

    private var cancellables: [AnyCancellable] = []

    func authorizeFromUrl(url: URL) {
        shared.playerNegotiation.spotifyConnector.authorizeFromUrl(url: url)
    }
    
    init() {
        let remoteState = createPublisher(for: shared.remoteStateFlow)
            .assertNoFailure()
            .share()

        remoteState.receive(on: DispatchQueue.main)
            .assign(to: \.remoteState, on: self)
            .store(in: &cancellables)
        
        remoteState.map { ($0 as? Shared.RemoteState.ConnectedPlayback)?.track }
            .removeDuplicates { old, new in old?.url == new?.url }
            .throttle(for: 0.25, scheduler: RunLoop.main, latest: true)
            .sink { [weak self] track in
                if let track = track {
                    self?.shared.playerOrNull()?.platform.trackImage(track: track, size: 256) { [weak self] (image, error) in
                        self?.trackImage = image
                    }
                } else {
                    self?.trackImage = nil
                }
            }
            .store(in: &cancellables)
    }
}
