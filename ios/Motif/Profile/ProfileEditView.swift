//
//  ProfileEditView.swift
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

struct ProfileEditView: View {
    @StateObject var viewModel: ProfileEditViewModelShim = ProfileEditViewModelShim()

    var body: some View {
        List {
            Section("Display name") {
                TextField("Display name", text: $viewModel.displayName)
            }
            Section("Username") {
                HStack(spacing: 0) {
                    TextField("Username", text: $viewModel.username)
                    Spacer()
                    AnyView(usernameAvailability)
                }
            }
            Section("Biography") {
                TextField("Biography", text: $viewModel.biography)
            }
        }
        .navigationTitle("Edit Profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    viewModel.shared.submit()
                } label: {
                    switch viewModel.submissionStatus {
                    case .outstanding, .submitted:
                        Text("Done")
                            .fontWeight(.bold)
                            .foregroundColor(.accentColor)
                    case .inProgress:
                        ProgressView()
                    case .failed:
                        Image(systemName: "exclamationmark.circle.fill")
                            .foregroundColor(.red)
                    default:
                        EmptyView()
                    }
                }
                .disabled(!viewModel.canSubmit)
            }
        }
    }

    var usernameAvailability: any View {
        if viewModel.usernameAvailability == ProfileUsernameAvailability.isOwn {
            return EmptyView()
        } else {
            let title: String
            let systemImage: String
            let foregroundColor: Color
            switch viewModel.usernameAvailability {
            case .available:
                title = "Available"
                systemImage = "checkmark.circle.fill"
                foregroundColor = .primary
            case .unavailable:
                title = "Unavailable"
                systemImage = "x.circle.fill"
                foregroundColor = .red
            case .loading:
                return ProgressView()
            default:
                return EmptyView()
            }
            return Label(title, systemImage: systemImage)
                .foregroundColor(foregroundColor)
        }
    }
}
