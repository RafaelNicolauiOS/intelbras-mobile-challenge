@testable import iosApp
import SwiftUI
import XCTest

/// The dashboard closes the keyboard on taps outside the search field, using the field's
/// frame. If that frame is lost, every tap counts as "outside" and the keyboard can never
/// open, which is what happened on iOS.
final class SearchFocusTests: XCTestCase {
    private let searchFrame = CGRect(x: 16, y: 100, width: 370, height: 48)

    /// SwiftUI reduces the key over every child, including siblings that never set it and
    /// therefore report the default value. Those must not erase the field's frame.
    func testSiblingsWithoutAFrameDoNotEraseTheSearchFrame() {
        var value = SearchFrameKey.defaultValue
        for next in [searchFrame, SearchFrameKey.defaultValue, SearchFrameKey.defaultValue] {
            SearchFrameKey.reduce(value: &value) { next }
        }

        XCTAssertEqual(value, searchFrame)
    }

    func testTheFrameIsFoundWhenSiblingsComeFirst() {
        var value = SearchFrameKey.defaultValue
        for next in [SearchFrameKey.defaultValue, SearchFrameKey.defaultValue, searchFrame] {
            SearchFrameKey.reduce(value: &value) { next }
        }

        XCTAssertEqual(value, searchFrame)
    }
}
