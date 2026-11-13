import Shared
import XCTest

/// Devices shared by the iOS tests.
enum Fixtures {
    static let alarm = DeviceAlarm(id: "a1", name: "Central da Casa", isFavorite: false, macAddress: "1A2B3C4D5E6F", password: "1234")
    static let camera = DeviceVideo(id: "v1", name: "Câmera Garagem", isFavorite: true, serial: "SN-001", username: "admin", password: "x")

    static func repository(_ devices: [Device] = [alarm, camera]) -> InMemoryDeviceRepository {
        InMemoryDeviceRepository(initial: devices)
    }
}

extension XCTestCase {
    /// Waits, yielding the main run loop, until `condition` holds. The shared ViewModels
    /// publish from Kotlin coroutines on the main queue, so state changes arrive asynchronously.
    @MainActor
    func waitUntil(
        timeout: TimeInterval = 3,
        file: StaticString = #filePath,
        line: UInt = #line,
        _ condition: () -> Bool
    ) async {
        let deadline = Date().addingTimeInterval(timeout)
        while !condition() {
            if Date() > deadline {
                XCTFail("Condition not met within \(timeout)s", file: file, line: line)
                return
            }
            try? await Task.sleep(nanoseconds: 10_000_000)
        }
    }

    /// The first message the center delivers.
    func firstMessage(of center: MessageCenter) async -> UserMessage? {
        for await message in center.messages {
            return message
        }
        return nil
    }
}
