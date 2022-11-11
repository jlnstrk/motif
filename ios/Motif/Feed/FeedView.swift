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
    @StateObject var viewModel: FeedViewModelShim = FeedViewModelShim()

    private let columns: [GridItem] = [
        .init(.adaptive(minimum: 72), spacing: 8)
    ]

    private var sections: [(Recentness, [ProfileWithMotifs])] {
        Dictionary(grouping: viewModel.feedState.profilesOrEmpty, by: { $0.motifs.first!.recentness })
            .sorted(by: { a, b in a.key.rawValue < b.key.rawValue })
    }

    var body: some View {
        ZStack {
            ScrollView {
                if let data = viewModel.feedState as? Shared.FeedState.Data {
                    LazyVGrid(columns: columns, pinnedViews: [.sectionHeaders]) {
                        ForEach(sections, id: \.0) { (recentness, profiles) in
                            Section {
                                ForEach(profiles, id: \.profile.id) { profile in
                                    let motifId = Int(profile.motifs.first!.id_)
                                    NavigationLink(destination: MotifDetailView(
                                        viewModel: MotifDetailViewModelShim(motifId: motifId)
                                    )) {
                                        FeedProfile(profileWithMotifs: profile)
                                    }
                                }
                            } header: {
                                Text(recentness.title)
                                    .font(.headline)
                                    .frame(height: 32)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                            }
                        }
                    }
                    .padding(.horizontal)
                } else {
                    Text("No feed")
                }
            }
            .navigationTitle("Feed")
        }
    }
}

extension Shared.FeedState {
    var profilesOrEmpty: [Shared.ProfileWithMotifs] {
        (self as? Shared.FeedState.Data)?.profiles ?? []
    }
}
