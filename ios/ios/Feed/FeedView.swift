//
//  FeedView.swift
//  ios
//
//  Created by Julian Ostarek on 02.10.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import Shared
import MusicKit
import StoreKit
import MediaPlayer
import KMPNativeCoroutinesAsync

struct FeedView: View {
    @StateObject var viewModel: FeedViewModelShim = FeedViewModelShim()
    @State var uiImage: UIImage?

    var body: some View {
        ScrollView {
            LazyVStack {
                ScrollView(.horizontal) {
                    LazyHStack {
                        if let data = viewModel.feedState as? Shared.FeedState.Data {
                            ForEach($viewModel.feedState.motifGroupsOrEmpty, id: \.creator.id) { item in
                                VStack {
                                    let photoUrl = item.wrappedValue.creator.photoUrl
                                    AsyncImage(url: photoUrl != nil ? URL(string: photoUrl!) : nil) { phase in
                                        Group {
                                            switch phase {
                                            case .empty:
                                                ProgressView()
                                            case .success(let image):
                                                image.resizable()
                                                    .aspectRatio(contentMode: .fit)
                                            case .failure:
                                                Image(systemName: "person.circle.fill")
                                            @unknown default:
                                                EmptyView()
                                            }
                                        }
                                        .frame(width: 80, height: 80)
                                        .clipShape(Circle())
                                        .overlay(Circle().stroke(Color.white, lineWidth: 4))
                                        .overlay(GeometryReader { proxy in
                                            Text(String(item.wrappedValue.motifs.count))
                                                .foregroundColor(Color.black)
                                                .font(.system(size: 14, weight: .bold))
                                                .frame(height: 18)
                                                .background(
                                                    Capsule()
                                                        .fill(Color.white)
                                                        .frame(minWidth: 18)
                                                )
                                                .position(x: 68, y: 68)
                                        })
                                    }
                                    Text(item.wrappedValue.creator.displayName)
                                }
                            }
                        } else {
                            Text("No feed")
                        }
                        Spacer()
                    }
                    .frame(height: 128)
                    .padding(.horizontal)
                    .padding(.top)
                    Button("Play smth") {
                        Shared.AppleMusicKt.abc { uiImage in
                            self.uiImage = uiImage
                        }
                        DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                            let controller = Shared.MusicPlayerController(externalScope: viewModel.shared.viewModelScope)
                            let handle1 = Task {
                                print("start #1")
                                let stream = asyncStream(for: controller.playbackStateChangedNative)
                                for try await abc in stream {
                                    print("state \(abc)")
                                    // try await Task.sleep(nanoseconds: 2_000_000_000)
                                }
                                print("end #1")
                            }
                            let handle2 = Task {
                                print("start #2")
                                let stream = asyncStream(for: controller.currentItemChangedNative)
                                for try await abc in stream {
                                    print("currItem \(abc)")
                                    // try await Task.sleep(nanoseconds: 4_000_000_000)
                                }
                                print("end #2")
                            }

                            DispatchQueue.main.asyncAfter(deadline: .now() + 15) {
                                handle1.cancel()
                            }
                            DispatchQueue.main.asyncAfter(deadline: .now() + 10) {
                                handle2.cancel()
                            }
                        }
                    }
                    if let image = uiImage {
                        Image(uiImage: image)
                    }
                }
                Spacer()
            }
        }
        .navigationTitle("Feed")
        .onAppear {
            viewModel.setup()
        }
    }
}

extension Shared.FeedState {
    var motifGroupsOrEmpty: [FeedMotifGroup] {
        get {
            if let data = self as? Shared.FeedState.Data {
                return data.motifGroups
            }
            return []
        }
        set {
            print(newValue)
        }
    }
}
