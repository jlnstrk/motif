//
//  ProfileView.swift
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
import SwiftUIPullToRefresh

struct ProfileView: View {
    @StateObject var viewModel: ProfileViewModelShim

    @State private var isPresentingUnfollowAlert: Bool = false

    var body: some View {
        RefreshableScrollView(action: {
            await viewModel.refresh()
        }, progress: { state in
            RefreshActivityIndicator(isAnimating: state == .loading)
        }) {
            LazyVStack(alignment: .leading) {
                VStack(alignment: .leading) {
                    HStack(spacing: 16) {
                        profileImage
                        Spacer()
                        VStack(alignment: .center) {
                            AnyView(followersCount)
                            Text("Followers")
                                .font(.caption)
                        }
                        VStack(alignment: .center) {
                            AnyView(followingCount)
                            Text("Following")
                                .font(.caption)
                        }
                        Spacer()
                    }
                    .padding()

                    AnyView(displayName)
                        .padding(.horizontal)

                    AnyView(biography)
                        .padding(.horizontal)

                    if (!viewModel.shared.isMyProfile) {
                        AnyView(followButton)
                            .padding()
                    }
                }
            }
        }
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                if viewModel.shared.isMyProfile {
                    NavigationLink(destination: ProfileEditView()) {
                        Image(systemName: "pencil.circle")
                    }
                }
            }
        }
    }

    var followButton: any View {
        let profile = (viewModel.state as? ProfileState.Loaded)?.profile
        if let profile {
            return Button {
                if profile.follows {
                    isPresentingUnfollowAlert = true
                } else {
                    viewModel.shared.follow()
                }
            } label: {
                if profile.follows {
                    Label("Following", systemImage: "checkmark")
                        .frame(maxWidth: .infinity)
                        .alert(
                            "Unfollow @\(profile.username)?",
                            isPresented: $isPresentingUnfollowAlert
                        ) {
                            Button("Unfollow", role: .destructive) {
                                viewModel.shared.unfollow()
                            }
                            Button("Cancel", role: .cancel) {
                                isPresentingUnfollowAlert = false
                            }
                        }
                } else {
                    Text("Follow")
                        .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.bordered)
        } else {
            return EmptyView()
        }
    }

    var navigationTitle: String {
        let username = (viewModel.state as? ProfileState.Loaded)?.profile.username
        if let username {
            return "@\(username)"
        } else {
            return ""
        }
    }

    var profileImage: some View {
        let photoUrl = (viewModel.state as? ProfileState.Loaded)?.profile.photoUrl
        return AsyncImage(url: photoUrl != nil ? URL(string: photoUrl!) : nil) { phase in
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
                        .scaledToFit()
                @unknown default:
                    EmptyView()
                }
            }
            .frame(width: 88, height: 88)
            .clipShape(Circle())
        }
    }

    var followersCount: any View {
        let followersCount = (viewModel.state as? ProfileState.Loaded)?.profile.followersCount
        if let followersCount {
            return counterText(text: String(followersCount))
        } else {
            return ProgressView()
        }
    }

    var followingCount: any View {
        let followingCount = (viewModel.state as? ProfileState.Loaded)?.profile.followingCount
        if let followingCount {
            return counterText(text: String(followingCount))
        } else {
            return ProgressView()
        }
    }

    var displayName: any View {
        let profile = (viewModel.state as? ProfileState.Loaded)?.profile
        if let profile {
            return Text(profile.displayName)
                .font(.headline)
                .fontWeight(.bold)
        } else {
            return EmptyView()
        }
    }

    var biography: any View {
        let profile = (viewModel.state as? ProfileState.Loaded)?.profile
        if let profile,
           let biography = profile.biography,
           !biography.isEmpty {
            return Text(biography)
        } else {
            return EmptyView()
        }
    }

    func counterText(text: String) -> some View {
        Text(text)
            .font(.body)
            .fontWeight(.bold)
    }
}
