//
//  FeedProfile.swift
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

struct FeedProfile: View {
    let profileWithMotifs: ProfileWithMotifs

    var body: some View {
        let photoUrl = profileWithMotifs.profile.photoUrl
        GeometryReader { proxy in
            AsyncImage(url: photoUrl != nil ? URL(string: photoUrl!) : nil) { phase in
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
                .clipShape(Circle())
                .padding(.all, 5)
                .frame(width: proxy.size.width, height: proxy.size.height)
                .overlay(
                    Circle()
                        .strokeBorder(
                            LinearGradient(
                                colors: [.accentColorDark, .accentColorLight],
                                startPoint: .leading,
                                endPoint: .trailing
                            ),
                            lineWidth: 2.5
                        )
                )
                .overlay(GeometryReader { proxy in
                    Text(String(profileWithMotifs.motifs.count))
                        .foregroundColor(Color.black)
                        .font(.system(size: 14, weight: .bold))
                        .frame(height: 18)
                        .background(
                            Capsule()
                                .fill(Color.white)
                                .frame(minWidth: 18)
                        )
                        .position(x: proxy.size.width - 16, y: proxy.size.height - 16)
                })
            }
        }
        .aspectRatio(1, contentMode: .fit)
    }
}
