//
//  ProfileSearchView.swift
//  ios
//
//  Created by Julian Ostarek on 27.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import Shared

struct ProfileSearchView: View {
    @ObservedObject var viewModel: ProfileSearchViewModelShim

    var body: some View {
        switch viewModel.state {
        case is Shared.ProfileSearchState.NoQuery:
            Text("Type to see results")
                .foregroundColor(.secondary)
        case is Shared.ProfileSearchState.Loading:
            ProgressView()
        case is Shared.ProfileSearchState.NoResults:
            Text("Didn't find anyone with that name")
                .foregroundColor(.secondary)
        case let results as Shared.ProfileSearchState.Results:
            List {
                ForEach(results.results, id: \.id_) { profile in
                    ProfileSearchProfile(profile: profile)
                }
            }
            .listStyle(.grouped)
        default:
            EmptyView()
        }
    }
}
