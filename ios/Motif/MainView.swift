//
//  MainView.swift
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

struct MainView: View {
    @StateObject private var playerViewModel: PlayerViewModelShim = PlayerViewModelShim()
    @StateObject private var profileSearchViewModel: ProfileSearchViewModelShim = ProfileSearchViewModelShim()

    var body: some View {
        ZStack {
            TabView {
                NavigationView {
                    WithinThemeGradient {
                        FeedOrProfileSearchView(
                            profileSearchViewModel: profileSearchViewModel
                        )
                    }
                }
                .searchable(text: $profileSearchViewModel.query, prompt: "Search people")
                .tabItem {
                    Image(systemName: "music.note.list")
                    Text("Feed")
                }
                NavigationView {
                    ProfileView(viewModel: ProfileViewModelShim(for: nil))
                }
                .tabItem {
                    Image(systemName: "person.fill")
                    Text("Profile")
                }
            }
            .environmentObject(playerViewModel)
            VStack {
                Spacer()
                PlayerBar(viewModel: playerViewModel)
                    .padding()
                    .padding([.bottom], 48)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .sheet(isPresented: $playerViewModel.isPresentingChooser) {
            PlayerChooser(playerViewModel: playerViewModel)
                .introspectViewController { controller in
                    let pc = controller.presentationController as? UISheetPresentationController
                    pc?.detents = [.medium(), .large()]
                }
        }
    }
}
