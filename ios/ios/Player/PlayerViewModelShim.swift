//
//  PlayerViewModel.swift
//  ios
//
//  Created by Julian Ostarek on 02.10.22.
//  Copyright © 2022 orgName. All rights reserved.
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
