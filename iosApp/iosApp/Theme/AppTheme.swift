import SwiftUI
import UIKit

/// The palette from the style guide, with the names it uses there.
enum AppColors {
    static let primary = Color(hex: 0x00B26B)
    static let primarySoft = Color(hex: 0xD6F2E7)
    static let text = Color(hex: 0x253B40)
    static let textSecondary = Color(hex: 0x9DB3BD)
    static let background = Color(hex: 0xFFFFFF)
    static let surface = Color(hex: 0xFAFAFA)
    static let surfaceStrong = Color(hex: 0xF1F1F1)
    static let borderMedium = Color(hex: 0xDEDFE0)
    static let borderButton = Color(hex: 0xBEC2C4)
    static let error = Color(hex: 0xD72736)
    static let surfaceDark = Color(hex: 0x1A1B1D)
    static let textOnDark = Color(hex: 0xF5EFF7)
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

/// The bordered card used across the app to group related content.
struct AppCard<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        content
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(AppColors.surface, in: RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(AppColors.surfaceStrong, lineWidth: 1))
    }
}
