//
//  MainView.swift
//  ios
//
//  Created by Julian Ostarek on 02.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI

struct MainView: View {
    @StateObject private var playerViewModel: PlayerViewModelShim = PlayerViewModelShim()
    @StateObject private var profileSearchViewModel: ProfileSearchViewModelShim = ProfileSearchViewModelShim()

    var body: some View {
        TabView {
            NavigationView {
                FeedOrProfileSearchView(
                    playerViewModel: playerViewModel,
                    profileSearchViewModel: profileSearchViewModel
                )
            }
            .searchable(text: $profileSearchViewModel.query, prompt: "Search people")
            .tabItem {
                Image(systemName: "person.3.fill")
                Text("Feed")
            }
            NavigationView {
                EmptyView() // PlayerView(viewModel: playerViewModel)
            }
            .tabItem {
                Image(systemName: "music.note")
                Text("Player")
            }
        }
    }
}
