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
    private let delegate = Shared.PlayerViewModel()

    @Published var frontendState: FrontendState = FrontendState.Disconnected()
    @Published var trackImage: UIImage?

    private var cancellables: [AnyCancellable] = []

    func connect() {
        delegate.spotifyConnector.connect()
    }

    func authorizeFromUrl(url: URL) {
        delegate.spotifyConnector.authorizeFromUrl(url: url)
    }

    func dump() {
        createPublisher(for: delegate.frontendStateNative)
            .assertNoFailure()
            .receive(on: DispatchQueue.main)
            .assign(to: \.frontendState, on: self)
            .store(in: &cancellables)

        createPublisher(for: delegate.frontendStateNative)
            .assertNoFailure()
            .map { ($0 as? FrontendState.ConnectedPlayback)?.track }
            .removeDuplicates { old, new in old?.uri == new?.uri }
            .debounce(for: 0.25, scheduler: RunLoop.main, options: nil)
            .sink { [weak self] track in
                if let track = track {
                    self?.delegate.spotifyRemote?.imagesApi.getImage(track: track, width: 256, height: 256) { [weak self] (image, error) in
                        self?.trackImage = image
                    }
                }
            }
            .store(in: &cancellables)
    }
}
