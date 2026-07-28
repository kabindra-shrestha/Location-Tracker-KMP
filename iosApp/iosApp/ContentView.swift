import Shared
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController(developerMode: isDeveloperMode)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

#if DEBUG
private let isDeveloperMode = true
#else
private let isDeveloperMode = false
#endif

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}
