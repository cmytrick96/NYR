package com.mnikita.knowyourrunway.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.mnikita.knowyourrunway.R
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import androidx.compose.ui.unit.sp

private val googleFontsProvider = Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val brandFont = GoogleFont("Manrope")

val NYRFontFamily = FontFamily(
    Font(googleFont = brandFont, fontProvider = googleFontsProvider, weight = FontWeight.W400),
    Font(googleFont = brandFont, fontProvider = googleFontsProvider, weight = FontWeight.W500),
    Font(googleFont = brandFont, fontProvider = googleFontsProvider, weight = FontWeight.W600),
    Font(googleFont = brandFont, fontProvider = googleFontsProvider, weight = FontWeight.W700)
)

private val Base = Typography()

// ✅ Luxury hierarchy + tracking (global)
val NYRTypography = Base.copy(
    displayLarge = Base.displayLarge.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W700,
        letterSpacing = (-0.6).sp
    ),
    displayMedium = Base.displayMedium.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W700,
        letterSpacing = (-0.4).sp
    ),
    displaySmall = Base.displaySmall.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W700,
        letterSpacing = (-0.2).sp
    ),

    headlineLarge = Base.headlineLarge.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W700,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = Base.headlineMedium.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W700,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = Base.headlineSmall.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W600
    ),

    titleLarge = Base.titleLarge.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W700,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = Base.titleMedium.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W600,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = Base.titleSmall.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W600
    ),

    bodyLarge = Base.bodyLarge.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = Base.bodyMedium.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.1.sp
    ),
    bodySmall = Base.bodySmall.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W400
    ),

    labelLarge = Base.labelLarge.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.2.sp
    ),
    labelMedium = Base.labelMedium.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.2.sp
    ),
    labelSmall = Base.labelSmall.copy(
        fontFamily = NYRFontFamily,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.2.sp
    )
)