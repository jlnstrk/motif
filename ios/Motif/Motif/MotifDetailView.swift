//
//  MotifDetailView.swift
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

struct MotifDetailView: View {
    @EnvironmentObject var playerViewModel: PlayerViewModelShim
    @StateObject var viewModel: MotifDetailViewModelShim

    var body: some View {
        ZStack(alignment: .top) {
            motif
        }
        .toolbar {
            ToolbarItem(placement: .navigation) {
                HStack {
                    profileImage
                    AnyView(username)
                }
            }
        }
    }

    var profileImage: some View {
        let photoUrl = (viewModel.state as? Shared.MotifDetailState.Data)?.motif.creator.photoUrl
        return AsyncImage(url: photoUrl != nil ? URL(string: photoUrl!) : nil) { phase in
            Group {
                switch phase {
                case .empty:
                    ProgressView()
                case .success(let image):
                    image.resizable()
                        .aspectRatio(contentMode: .fill)
                case .failure:
                    Image(systemName: "person.circle.fill")
                        .resizable()
                        .scaledToFit()
                @unknown default:
                    EmptyView()
                }
            }
            .frame(width: 32, height: 32)
            .clipShape(Circle())
        }
    }

    var username: any View {
        if let username = (viewModel.state as? Shared.MotifDetailState.Data)?.motif.creator.username {
            return Text("@\(username)")
                .font(.headline)
                .fontWeight(.bold)
        } else {
            return EmptyView()
        }
    }

    var motif: some View {
        VStack {
            coverArt
                .frame(width: 196, height: 196)

            let metadata = (viewModel.state as? MotifDetailState.Data)?.motif.metadata
            if let metadata {
                Text(metadata.name)
                    .font(.headline)
                    .fontWeight(.bold)
                Text(metadata.artist)
                    .font(.caption)
            }
            Button {
                let motif = (viewModel.state as? MotifDetailState.Data)?.motif
                if let motif {
                    playerViewModel.shared.play(motif: motif)
                }
            } label: {
                Image(systemName: "play.fill")
            }
            .buttonStyle(.plain)
        }
    }

    var coverArt: some View {
        let photoUrl = (viewModel.state as? MotifDetailState.Data)?.motif.metadata?.coverArtUrl
        let url = photoUrl != nil ? URL(string: photoUrl!) : nil
        return AsyncImage(url: url?.forceHttps) { phase in
            Group {
                switch phase {
                case .empty:
                    ZStack(alignment: .center) {
                        ProgressView()
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                case .success(let image):
                    image.resizable()
                        .aspectRatio(1, contentMode: .fill)
                case .failure:
                    DefaultCoverArt()
                @unknown default:
                    EmptyView()
                }
            }
            .aspectRatio(1, contentMode: .fill)
            .scaledToFit()
            .clipShape(RoundedRectangle(cornerRadius: 4))
        }
    }
}
