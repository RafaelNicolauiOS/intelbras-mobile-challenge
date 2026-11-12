import Shared
import SwiftUI

struct ContentView: View {
    var body: some View {
        Text(PlatformKt.greeting())
            .padding()
    }
}

#Preview {
    ContentView()
}
