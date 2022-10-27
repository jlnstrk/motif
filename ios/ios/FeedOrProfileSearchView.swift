//
//  FeedOrProfileSearchView.swift
//  ios
//
//  Created by Julian Ostarek on 27.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI

struct FeedOrProfileSearchView: View {
    @ObservedObject var playerViewModel: PlayerViewModelShim
    @ObservedObject var profileSearchViewModel: ProfileSearchViewModelShim
    @Environment(\.isSearching) var isSearching: Bool

    var body: some View {
        if isSearching {
            ProfileSearchView(viewModel: profileSearchViewModel)
        } else {
            FeedView(playerViewModel: playerViewModel)
        }
    }
}
