//
//  LoginView.swift
//  ios
//
//  Created by Julian Ostarek on 02.10.22.
//  Copyright © 2022 orgName. All rights reserved.
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
