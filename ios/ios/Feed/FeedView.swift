//
//  FeedView.swift
//  ios
//
//  Created by Julian Ostarek on 02.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import Shared
import MusicKit
import StoreKit
import MediaPlayer
import KMPNativeCoroutinesAsync

struct FeedView: View {
    @ObservedObject var playerViewModel: PlayerViewModelShim
    @StateObject var viewModel: FeedViewModelShim = FeedViewModelShim()

    var body: some View {
        ScrollView([.horizontal, .vertical]) {
            if let data = viewModel.feedState as? Shared.FeedState.Data {
                let grid = data.profilesGrid
                let gridSize = Int(grid.size)
                let columns = [GridItem](
                    repeating: GridItem(.fixed(72), spacing: 8),
                    count: gridSize
                )
                LazyVGrid(columns: columns, alignment: .center) {
                    ForEach(Array(viewModel.feedState.gridOrEmpty.enumerated()), id: \.element?.profile.id_) { index, element in
                        if let profile = element {
                            NavigationLink(destination: PlayerView(
                                viewModel: playerViewModel,
                                profile: profile
                            )) {
                                FeedProfile(profileWithMotifs: profile)
                            }
                                .offset(x: (index / gridSize) % 2 == 0 ? 40 : 0)
                        } else {
                            EmptyView()
                        }
                    }
                }
            } else {
                Text("No feed")
            }
        }
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text("Motif")
                    .font(.custom("Poppins-Bold", size: 24))
            }
        }
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            viewModel.setup()
        }
    }
}

extension Shared.FeedState {
    var gridOrEmpty: [Shared.ProfileWithMotifs?] {
        get {
            if let data = self as? Shared.FeedState.Data {
                return data.profilesGrid.grid as! [Shared.ProfileWithMotifs?]
            }
            return []
        }
        set {
            print(newValue)
        }
    }
}
