//
//  PlayerView.swift
//  ios
//
//  Created by Julian Ostarek on 02.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import Shared

struct PlayerView: View {
    @StateObject var viewModel: PlayerViewModelShim = PlayerViewModelShim()

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
                case is FrontendState.ConnectedNoPlayback:
                    Text("Connected")
                case let playback as FrontendState.ConnectedPlayback:
                    Text("Connected: Track: \(playback.track.name ?? "<None>"), paused: \(playback.isPaused.description), position: \(playback.position)")
                default:
                    Text("Other state")
                }
                Button("Connecct & Play") {
                    viewModel.connect()
                }
                Spacer()
            }
        }
        .navigationTitle("Player")
        .onOpenURL(perform: {
            viewModel.authorizeFromUrl(url: $0)
        })
        .onAppear {
            viewModel.dump()
        }
    }
}
