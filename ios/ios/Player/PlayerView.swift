//
//  PlayerView.swift
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
import Introspect

struct PlayerView: View {
    @ObservedObject var viewModel: PlayerViewModelShim
    @State var isSelectingPlayer: Bool = false
    let profile: ProfileWithMotifs
    
    var body: some View {
        ZStack {
            if let trackImage = viewModel.trackImage {
                Image(uiImage: trackImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                ///.frame(width: proxy.size.width, height: proxy.size.height)
                // .clipped()
                    .edgesIgnoringSafeArea(.all)
                    .transition(.opacity.animation(.easeInOut))
                    .id(trackImage)
                    .overlay(Color.black.opacity(0.5))
                    .blur(radius: 20)
                    .padding(.horizontal)
                    .frame(maxWidth: .infinity)
            }
            VStack(alignment: HorizontalAlignment.center) {
                if let trackImage = viewModel.trackImage {
                    Image(uiImage: trackImage)
                        .resizable()
                        .frame(width: 192, height: 192)
                }
                switch viewModel.frontendState {
                case is FrontendState.Disconnected:
                    Text("Disconnected")
                case let connecting as FrontendState.Connecting:
                    Text("Connecting (\(connecting.service))")
                case let connectedNoPlayback as FrontendState.ConnectedNoPlayback:
                    Text("Connected (\(connectedNoPlayback.service))")
                case let playback as FrontendState.ConnectedPlayback:
                    Text("Connected (\(playback.service)): Track: \(playback.track.title ?? "<None>"), paused: \(playback.isPaused.description), position: \(playback.position)")
                default:
                    Text("Other state")
                }
                Button("Connect") {
                    isSelectingPlayer = true
                }
                Spacer()
            }
        }
        .sheet(isPresented: $isSelectingPlayer) {
            PlayerChooser(playerViewModel: viewModel)
                .introspectViewController { controller in
                    let pc = controller.presentationController as? UISheetPresentationController
                    pc?.detents = [.medium(), .large()]
                }
        }
        .onChange(of: viewModel.frontendState) { frontendState in
            if let _ = frontendState as? FrontendState.Connected,
               isSelectingPlayer {
                isSelectingPlayer = false
            }
        }
        .navigationTitle("Player")
        .onOpenURL(perform: {
            viewModel.authorizeFromUrl(url: $0)
        })
        .onAppear {
            viewModel.shared.play(motif: profile.motifs[0])
        }
    }
}
