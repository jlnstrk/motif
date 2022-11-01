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
