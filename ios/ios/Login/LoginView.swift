//
//  LoginView.swift
//  ios
//
//  Created by Julian Ostarek on 02.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI

struct LoginView: View {
    @Environment(\.openURL) var openURL
    @ObservedObject var viewModel: LoginViewModelShim

    var body: some View {
        Button("Log in with Spotify") {
            let loginUrl = URL(string: viewModel.shared.loginUrl())!
            openURL(loginUrl)
        }
        .onOpenURL {
            viewModel.shared.loginFromCallback(callbackUrl: $0.absoluteString)
        }
    }
}
