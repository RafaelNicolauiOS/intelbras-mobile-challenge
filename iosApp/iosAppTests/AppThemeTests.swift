@testable import iosApp
import SwiftUI
import XCTest

final class AppThemeTests: XCTestCase {
    private func rgb(_ color: Color) -> [Int] {
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        UIColor(color).getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        return [red, green, blue].map { Int(($0 * 255).rounded()) }
    }

    func testHexColorsDecodeEachChannel() {
        XCTAssertEqual(rgb(Color(hex: 0x00B26B)), [0x00, 0xB2, 0x6B])
        XCTAssertEqual(rgb(Color(hex: 0xFFFFFF)), [255, 255, 255])
        XCTAssertEqual(rgb(Color(hex: 0x000000)), [0, 0, 0])
    }

    func testPaletteMatchesTheStyleGuide() {
        XCTAssertEqual(rgb(AppColors.primary), [0x00, 0xB2, 0x6B])
        XCTAssertEqual(rgb(AppColors.text), [0x25, 0x3B, 0x40])
        XCTAssertEqual(rgb(AppColors.error), [0xD7, 0x27, 0x36])
    }

    func testAppFontIsOpenSansAtTheRequestedSize() {
        let font = UIFont.app(16)

        XCTAssertEqual(font.familyName, "Open Sans", "the font is bundled and registered through UIAppFonts")
        let expected = UIFontMetrics.default.scaledValue(for: 16)
        XCTAssertEqual(font.pointSize, expected, accuracy: 0.01)
    }

    func testSemiboldUsesTheWeightAxis() {
        let regular = UIFont.app(16, .regular)
        let semibold = UIFont.app(16, .semibold)

        XCTAssertNotEqual(regular.fontDescriptor, semibold.fontDescriptor)
        XCTAssertEqual(AppFontWeight.regular.rawValue, 400)
        XCTAssertEqual(AppFontWeight.semibold.rawValue, 600)
    }
}
