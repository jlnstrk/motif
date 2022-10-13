//
//  MainView.swift
//  ios
//
//  Created by Julian Ostarek on 02.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI

struct MainView: View {
    var body: some View {
        TabView {
            NavigationView {
                FeedView()
            }
            .tabItem {
                Image(systemName: "person.3.fill")
                Text("Feed")
            }
            NavigationView {
                PlayerView()
            }
            .tabItem {
                Image(systemName: "music.note")
                Text("Player")
            }
        }
    }
}
