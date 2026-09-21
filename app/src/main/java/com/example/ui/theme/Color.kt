package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// SAHAYAK AI PROFESSIONAL DESIGN SYSTEM PALETTE
// ==========================================

// Primary Palette (Deep Blue & Accents)
val PrimaryDeepBlue = Color(0xFF1F4E79)    // App bar, primary buttons, active states
val PrimaryBlueLight = Color(0xFF2E6DA4)   // Secondary accents, links
val PrimaryBlueDark = Color(0xFF153655)
val PrimaryBlue50 = Color(0xFFEDF4FA)
val PrimaryBlue100 = Color(0xFFD7E5F3)

// Status & Functional Colors
val SuccessGreen = Color(0xFF1F6E43)       // Confirmations, credit/positive amounts (Jama)
val SuccessGreenBg = Color(0xFFE8F5EE)
val WarningAmber = Color(0xFFB75B12)       // Low-confidence OCR flags, pending states
val WarningAmberBg = Color(0xFFFDF3EB)
val ErrorRed = Color(0xFFC0392B)           // Validation errors, debit/negative amounts (Udhaar)
val ErrorRedBg = Color(0xFFFDEEEC)

// Neutrals & Surface Colors (Crisp, High Contrast)
val TextPrimary = Color(0xFF1A1A1A)        // Body text, headings (avoid pure black)
val TextSecondary = Color(0xFF5F6368)      // Captions, hints, timestamps
val TextMuted = Color(0xFF757575)
val SurfaceColor = Color(0xFFFFFFFF)       // Cards, inputs
val BackgroundColor = Color(0xFFF7F9FB)    // Screen background
val BorderColor = Color(0xFFE0E0E0)        // Dividers, input borders
val BorderFocused = Color(0xFF1F4E79)

// Backwards compatibility aliases to ensure complete compatibility across all screens
val PureWhite = Color(0xFFFFFFFF)
val Slate900 = TextPrimary
val Slate800 = Color(0xFF2D3748)
val Slate700 = TextSecondary
val Slate600 = Color(0xFF5A6A85)
val Slate500 = Color(0xFF718096)
val Slate400 = Color(0xFFA0AEC0)
val Slate300 = BorderColor
val Slate200 = Color(0xFFEEF2F6)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = BackgroundColor

val Emerald900 = PrimaryBlueDark
val Emerald800 = PrimaryDeepBlue
val Emerald700 = PrimaryDeepBlue
val Emerald600 = PrimaryBlueLight
val Emerald500 = PrimaryBlueLight
val Emerald300 = Color(0xFF90CDF4)
val Emerald200 = PrimaryBlue100
val Emerald100 = PrimaryBlue100
val Emerald50 = PrimaryBlue50

val Amber900 = Color(0xFF7C2D12)
val Amber800 = WarningAmber
val Amber700 = WarningAmber
val Amber600 = Color(0xFFD97706)
val Amber500 = Color(0xFFF59E0B)
val Amber400 = Color(0xFFFBBF24)
val Amber300 = Color(0xFFFDE68A)
val Amber200 = Color(0xFFFEF3C7)
val Amber100 = WarningAmberBg
val Amber50 = Color(0xFFFFFBEB)

val JamaGreen = SuccessGreen
val JamaGreenBg = SuccessGreenBg
val UdhaarRed = ErrorRed
val UdhaarRedBg = ErrorRedBg
val PendingOrange = WarningAmber
val PendingOrangeBg = WarningAmberBg

val Indigo700 = PrimaryDeepBlue
val Indigo100 = PrimaryBlue100
val GoldYellow = Color(0xFFEAB308)
val Crimson800 = ErrorRed
val Crimson600 = ErrorRed
val Crimson300 = Color(0xFFFCA5A5)
val Crimson100 = ErrorRedBg
val Blue900 = PrimaryDeepBlue
val Blue100 = PrimaryBlue100
val Saffron700 = WarningAmber
val Saffron600 = Color(0xFFD97706)
val Saffron100 = WarningAmberBg
val SkyBlue = PrimaryBlueLight
val PurpleAccent = Color(0xFF6366F1)
