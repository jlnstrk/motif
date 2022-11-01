//
//  WithinThemeGradient.swift
//  ios
//
//  Created by Julian Ostarek on 01.11.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI

struct WithinThemeGradient<Content>: View where Content : View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        ZStack {
            LinearGradient(
                stops: [
                    .init(color: .accentColor, location: 0.0),
                    .init(color: .black, location: 0.33)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .edgesIgnoringSafeArea(.all)

            content()
        }
    }
}
