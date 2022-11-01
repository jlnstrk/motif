//
//  LoginView.swift
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

struct LoginView: View {
    @Environment(\.openURL) var openURL
    @ObservedObject var viewModel: LoginViewModelShim

    var body: some View {
        VStack {
            Spacer()
            Group {
                Button {
                    let loginUrl = URL(string: viewModel.shared.loginUrl(service: ClientService.applemusic))!
                    openURL(loginUrl)
                } label: {
                    Label {
                        Text("Sign in with Apple")
                    } icon: {
                        Image(systemName: "applelogo")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 32, height: 32)
                    }
                    .frame(maxWidth: .infinity)
                }

                Button {
                    let loginUrl = URL(string: viewModel.shared.loginUrl(service: ClientService.spotify))!
                    openURL(loginUrl)
                } label: {
                    Label {
                        Text("Sign in with Spotify")
                    } icon: {
                        Image("Spotify")
                            .renderingMode(.template)
                            .resizable()
                            .interpolation(.high)
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 32, height: 32)
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .buttonStyle(.borderedProminent)
            .tint(.white)
            .foregroundColor(.black)
            .padding(.horizontal)
        }
        .padding([.bottom])
        .onOpenURL {
            viewModel.shared.loginFromCallback(callbackUrl: $0.absoluteString)
        }
    }
}
