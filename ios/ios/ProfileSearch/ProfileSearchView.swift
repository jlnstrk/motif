//
//  ProfileSearchView.swift
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
                    NavigationLink(destination: ProfileView(viewModel: ProfileViewModelShim(for: ProfileReference.Simple(simple: profile as! ProfileSimple)))) {
                        ProfileSearchProfile(profile: profile)
                    }
                }
            }
            .listStyle(.grouped)
        default:
            EmptyView()
        }
    }
}
