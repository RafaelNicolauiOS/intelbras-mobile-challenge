import Dispatch
import os

/// Marks every state the Observables publish as a "Points of Interest" event, so Instruments
/// shows when each screen re-renders and how long after a keystroke the new state arrives.
enum Signposts {
    static let signposter = OSSignposter(subsystem: "dev.rafaelnicolau.devices", category: .pointsOfInterest)

    /// Also asserts, in debug builds, that the state is published on the main thread.
    @MainActor
    static func published(_ name: StaticString) {
        assertOnMainThread()
        signposter.emitEvent(name)
    }
}

/// The thread contract of the app: everything that updates a view or handles a text field runs
/// on the main thread. The `@MainActor` annotations let the compiler check it; this catches what
/// the compiler cannot see, such as a Kotlin callback arriving on another thread. Debug only, so
/// a regression crashes during development instead of glitching in production.
@inline(__always)
func assertOnMainThread() {
    #if DEBUG
    dispatchPrecondition(condition: .onQueue(.main))
    #endif
}

// Publishing rule for the Observables: assign a `@Published` property only when the value
// really changed, with `if new != current { current = new }`. `@Published` notifies on every
// assignment, even of an equal value, and each notification is a SwiftUI render. The shared
// state types are Kotlin data classes, so `!=` uses their `equals`. (Passing the property
// `inout` to a helper would not work: an inout access always writes back, and notifies.)
