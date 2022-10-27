//
//  PlayerChooser.swift
//  ios
//
//  Created by Julian Ostarek on 26.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import Shared

struct PlayerChooser: View {
    @ObservedObject var playerViewModel: PlayerViewModelShim

    var body: some View {
        NavigationView {
            List {
                ForEach(playerViewModel.shared.availableServices, id: \.service) { availability in
                    Button(action: {
                        playerViewModel.shared.selectPlayer(service: availability.service)
                    }) {
                        playerItem(availability: availability)
                    }
                    .accentColor(.primary)
                    .disabled(!availability.isInstalled)
                }
                Section {
                    Button("Disconnect") {
                        playerViewModel.shared.disconnect()
                    }
                    .disabled(!(playerViewModel.frontendState is FrontendState.Connected))
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationTitle("Select a player")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    var selectionIndicator: some View {
        Group {
            Spacer()
            VStack {
                Spacer()
                if playerViewModel.frontendState is FrontendState.Connected {
                    Image(systemName: "checkmark.circle.fill")
                } else {
                    ProgressView()
                }
                Spacer()
            }
        }
    }

    func playerItem(availability: PlayerServiceAvailabilityInfoServiceStatus) -> some View {
        HStack {
            VStack(alignment: .leading) {
                HStack {
                    VStack {
                        Spacer()
                        Group {
                            switch availability.service {
                            case PlayerPlayerService.appleMusic:
                                Image("AppleMusic")
                                    .resizable()
                                    .interpolation(.high)
                                    .frame(width: 40, height: 40)
                            case PlayerPlayerService.spotify:
                                Image("Spotify")
                                    .resizable()
                                    .interpolation(.high)
                                    .scaledToFit()
                                    .frame(width: 40, height: 40)
                            default:
                                EmptyView()
                            }
                        }
                        .opacity(availability.isInstalled ? 1.0 : 0.2)
                        .saturation(availability.isInstalled ? 1.0 : 0.0)
                        Spacer()
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        Group {
                            switch availability.service {
                            case PlayerPlayerService.appleMusic:
                                Text("Apple Music")
                                    .font(.headline)
                            case PlayerPlayerService.spotify:
                                Text("Spotify")
                                    .font(.headline)
                            default:
                                EmptyView()
                            }
                        }

                        HStack(alignment: .center) {
                            Image(systemName: availability.isInstalled ? "checkmark.circle.fill" : "xmark.circle.fill")
                            Text(availability.isInstalled ? "Available" : "Not Installed")
                        }
                        .foregroundColor(availability.isInstalled ? .secondary : .red)
                    }
                    .padding([.leading])
                }
            }

            var service: PlayerPlayerService? = nil
            switch playerViewModel.frontendState {
            case let connecting as FrontendState.Connecting:
                let _ = service = connecting.service
            case let connected as FrontendState.Connected:
                let _ = service = connected.service
            default:
                let _ = ()
            }
            if let service = service,
               service == availability.service {
                selectionIndicator
            }
        }
    }
}
