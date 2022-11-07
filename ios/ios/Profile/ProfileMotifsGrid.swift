//
//  ProfileFeedGrid.swift
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

struct ProfileMotifsGrid: View {
    let motifs: [Shared.MotifSimple]
    
    private let columns: [GridItem] = [
        .init(.adaptive(minimum: 96), spacing: 8)
    ]
    
    private var sections: [(Recentness, [Shared.MotifSimple])] {
        Dictionary(grouping: motifs, by: { $0.recentness })
            .sorted(by: { a, b in a.key.rawValue < b.key.rawValue })
    }
    
    var body: some View {
        LazyVGrid(columns: columns, spacing: 8, pinnedViews: [.sectionHeaders]) {
            ForEach(sections, id: \.0) { (key, motifs) in
                Section {
                    ForEach(motifs, id: \.id_) { motif in
                        NavigationLink(destination: MotifDetailView(viewModel: MotifDetailViewModelShim(motifId: Int(motif.id_)))) {
                            ProfileMotifsGridItem(motif: motif)
                        }
                    }
                } header: {
                    Text(key.title)
                        .font(.headline)
                        .frame(height: 32)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
        .padding(.horizontal)
    }
}

enum Recentness: Int {
    case today
    case lastWeek
    case older
}

extension Recentness {
    var title: String {
        switch self {
        case .today:
            return "Today"
        case .lastWeek:
            return "Last week"
        case .older:
            return "Older"
        }
    }
}

extension Shared.Motif {
    var recentness: Recentness {
        switch Calendar.current.dateComponents([.day], from: self.createdAt.toNSDate(), to: .now).day ?? 0 {
        case 0:
            return .today
        case 2..<7:
            return .lastWeek
        default:
            return .older
        }
    }
}
