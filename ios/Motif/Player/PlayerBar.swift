//
//  PlayerBar.swift
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

struct PlayerBar: View {
    @ObservedObject var viewModel: PlayerViewModelShim
    @State var isPresentingControls: Bool = false

    var body: some View {
        Button {
            withAnimation(.easeInOut(duration: 0.2)) {
                isPresentingControls.toggle()
            }
        } label: {
            VStack {
                HStack(spacing: 12) {
                    if isConnected {
                        Group {
                            if let trackImage = viewModel.trackImage {
                                Image(uiImage: trackImage)
                                    .resizable()
                                    .clipShape(RoundedRectangle(cornerSize: .init(width: 6, height: 6)))
                                    .aspectRatio(contentMode: .fill)
                                    .transition(.opacity.animation(.easeInOut))
                                    .scaledToFit()
                                    .id(viewModel.trackImage)
                            } else {
                                DefaultCoverArt()
                            }
                        }
                        .padding([.leading, .top, .bottom], 8)
                    }

                    metadataAndService
                    Spacer()
                    if isConnected {
                        playPauseButton
                    } else {
                        openButton
                    }
                }
                .frame(height: 56)
                if isPresentingControls {
                    HStack {
                        openButton
                        Button("Switch") {
                            viewModel.isPresentingChooser = true
                        }
                    }
                    .frame(height: 56)
                }
            }
            .padding(.trailing)
            .onOpenURL(perform: {
                viewModel.authorizeFromUrl(url: $0)
            })
            .background(
                RoundedRectangle(cornerSize: .init(width: 6, height: 6))
                    .fill(Color(uiColor: .secondarySystemBackground))
            )
            .shadow(radius: 8)
        }
        .buttonStyle(.plain)
    }

    var isConnected: Bool {
        viewModel.remoteState is Shared.RemoteState.Connected
    }

    var playPauseButton: some View {
        Group {
            if isConnected {
                let playbackState = viewModel.remoteState as? Shared.RemoteState.ConnectedPlayback
                Button {
                    if let isPaused = playbackState?.isPaused,
                       isPaused {
                        viewModel.shared.resume()
                    }else {
                        viewModel.shared.pause()
                    }
                } label: {
                    Image(systemName: (playbackState?.isPaused ?? false) ? "play.fill" : "pause.fill")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(height: 20)
                }
                .buttonStyle(.plain)
                .disabled(playbackState == nil)
            }
        }
    }

    var metadataAndService: some View {
        VStack(alignment: .leading, spacing: 4) {
            if let connectedState = (viewModel.remoteState as? Shared.RemoteState.Connected) {
                let text: String = {
                    if let playbackState = connectedState as? Shared.RemoteState.ConnectedPlayback {
                        return "\(playbackState.track.title) • \(playbackState.track.artists.joined(separator: ", "))"
                    } else {
                        return "No Playback"
                    }
                }()
                Text(text)
                    .font(.callout)
                    .fontWeight(.bold)

                Label {
                    var label: String? = nil
                    switch connectedState.service {
                    case PlayerPlayerService.appleMusic:
                        let _ = label = "Apple Music"
                    case PlayerPlayerService.spotify:
                        let _ = label = "Spotify"
                    default:
                        let _ = ()
                    }
                    if let label {
                        Text(label)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                } icon: {
                    var name: String? = nil
                    switch connectedState.service {
                    case PlayerPlayerService.appleMusic:
                        let _ = name = "AppleMusic"
                    case PlayerPlayerService.spotify:
                        let _ = name = "Spotify"
                    default:
                        let _ = ()
                    }
                    if let name {
                        Image(name)
                            .resizable()
                            .interpolation(.high)
                            .frame(width: 16, height: 16)
                    }
                }
            } else {
                Label("Not Connected", systemImage: "exclamationmark.circle.fill")
                    .foregroundColor(.secondary)
                    .padding(.leading)
            }
        }
    }

    var openButton: some View {
        Group {
            if let connectedState = viewModel.remoteState as? Shared.RemoteState.Connected {
                Button("Open") {

                }
            } else {
                Button("Connect") {
                    viewModel.isPresentingChooser = true
                }
            }
        }
    }
}
