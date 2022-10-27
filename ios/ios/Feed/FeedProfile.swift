//
//  FeedProfile.swift
//  ios
//
//  Created by Julian Ostarek on 27.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import Shared

struct FeedProfile: View {
    let profileWithMotifs: ProfileWithMotifs

    var body: some View {
        let photoUrl = profileWithMotifs.profile.photoUrl
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
            .frame(width: 72, height: 72)
            .clipShape(Circle())
            .overlay(Circle().strokeBorder(Color.white, lineWidth: 2))
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
                    .position(x: 60, y: 60)
            })
        }
    }
}
