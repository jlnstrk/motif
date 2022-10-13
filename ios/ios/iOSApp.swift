import SwiftUI
import Shared


@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
                .environmentObject(appDelegate)
		}
	}
}
