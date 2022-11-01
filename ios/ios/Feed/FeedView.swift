//
//  FeedView.swift
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
                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .padding(.vertical, 10)
            }
        }
        .navigationTitle("Motif")
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
