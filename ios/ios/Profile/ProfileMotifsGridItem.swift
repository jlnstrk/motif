//
//  ProfileMotifsGridItem.swift
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

struct ProfileMotifsGridItem: View {
    let motif: MotifSimple
    static let formatter: RelativeDateTimeFormatter = {
        var formatter = RelativeDateTimeFormatter()
        formatter.dateTimeStyle = .named
        formatter.unitsStyle = .full
        return formatter
    }()

    var body: some View {
        let photoUrl = motif.metadata?.coverArtUrl
        let url = photoUrl != nil ? URL(string: photoUrl!) : nil
        VStack(alignment: .center) {
            AsyncImage(url: url?.forceHttps) { phase in
                Group {
                    switch phase {
                    case .empty:
                        ZStack(alignment: .center) {
                            ProgressView()
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    case .success(let image):
                        image.resizable()
                            .aspectRatio(1, contentMode: .fill)
                    case .failure:
                        Image(systemName: "music.note")
                            .resizable()
                    @unknown default:
                        EmptyView()
                    }
                }
                .aspectRatio(1, contentMode: .fill)
                .scaledToFit()
                .clipShape(RoundedRectangle(cornerSize: .init(width: 4, height: 4)))
            }
            Text(Self.formatter.string(for: motif.createdAt.toNSDate())!)
                .font(.caption)
                .fontWeight(.bold)
        }
    }
}

extension URL {
    var forceHttps: URL {
        var components = URLComponents(url: self, resolvingAgainstBaseURL: true)!
        components.scheme = "https"
        return components.url!
    }
}
