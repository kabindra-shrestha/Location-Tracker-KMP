import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // This runs before a Compose view controller is created, allowing an active session to
        // restore its CLLocationManager and host delivery callback during app launch.
        #if DEBUG
        MainViewControllerKt.initializeLocationTracking(developerMode: true)
        #else
        MainViewControllerKt.initializeLocationTracking(developerMode: false)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
