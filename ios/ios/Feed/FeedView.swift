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

    private let columns: [GridItem] = [
        .init(.flexible(minimum: 72, maximum: .infinity), spacing: 8, alignment: .trailing),
        .init(.fixed(72), spacing: 8),
        .init(.fixed(72), spacing: 8),
        .init(.fixed(72), spacing: 8),
        .init(.flexible(minimum: 72, maximum: .infinity), spacing: 8, alignment: .leading),
    ]

    var body: some View {
        ScrollView([.horizontal, .vertical]) {
            if let data = viewModel.feedState as? Shared.FeedState.Data {
                LazyVGrid(columns: columns, alignment: .center) {
                    ForEach(Array(viewModel.feedState.motifGroupsOrEmpty.enumerated()), id: \.element.profile.id_) { index, element in
                        FeedProfile(profileWithMotifs: element)
                            .onTapGesture {
                                playerViewModel.shared.play(motif: element.motifs[0])
                            }
                            .offset(x: index / 5 % 2 == 0 ? -18 : 18)
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
    var motifGroupsOrEmpty: [Shared.ProfileWithMotifs] {
        get {
            if let data = self as? Shared.FeedState.Data {
                return data.motifGroups
            }
            return []
        }
        set {
            print(newValue)
        }
    }
}
