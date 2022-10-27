//
//  ProfileSearchItem.swift
//  ios
//
//  Created by Julian Ostarek on 27.10.22.
//  Copyright © 2022 orgName. All rights reserved.
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
