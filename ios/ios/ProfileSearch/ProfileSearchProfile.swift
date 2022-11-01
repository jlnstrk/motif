//
//  ProfileSearchItem.swift
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

struct ProfileSearchProfile: View {
    let profile: Shared.Profile

    var body: some View {
        HStack {
            AsyncImage(url: profile.photoUrl != nil ? URL(string: profile.photoUrl!) : nil) { phase in
                Group {
                    switch phase {
                    case .empty:
                        ProgressView()
                    case .success(let image):
                        image.resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure:
                        Image(systemName: "person.circle.fill")
                            .resizable()
                    @unknown default:
                        EmptyView()
                    }
                }
                .frame(width: 48, height: 48)
                .clipShape(Circle())
            }
            VStack(alignment: .leading) {
                Text(profile.displayName)
                Text("@\(profile.username)")
                    .foregroundColor(.secondary)
            }
        }
    }
}
