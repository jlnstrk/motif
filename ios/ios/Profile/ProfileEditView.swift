//
//  ProfileEditView.swift
//  ios
//
//  Created by Julian Ostarek on 01.11.22.
//  Copyright © 2022 orgName. All rights reserved.
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
