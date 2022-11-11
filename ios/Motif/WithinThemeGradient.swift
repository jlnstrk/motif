//
//  WithinThemeGradient.swift
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

struct WithinThemeGradient<Content>: View where Content : View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        ZStack {
            LinearGradient(
                stops: [
                    .init(color: .accentColorDark, location: 0.0),
                    .init(color: Color(uiColor: .systemBackground), location: 0.33)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .edgesIgnoringSafeArea(.all)

            content()
        }
    }
}
