//
//  AppDelegate.swift
//  ios
//
//  Created by Julian Ostarek on 07.08.22.
//  Copyright © 2022 orgName. All rights reserved.
//

import Foundation
import UIKit
import Shared

class AppDelegate: NSObject, UIApplicationDelegate, ObservableObject {

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        IosModuleKt.iosStartKoin()
        return true
    }
}
