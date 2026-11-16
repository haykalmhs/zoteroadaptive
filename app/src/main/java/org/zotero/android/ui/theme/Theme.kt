package org.zotero.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.zotero.android.R

// Gmail & Keep inspired scholarly palette
val SlatePrimary = Color(0xFF1E3A3A)
val TealAccentPill = Color(0xFFD3ECE9)
val OnTealAccentPill = Color(0xFF0B2E2B)
val ZoteroBrandRed = Color(0xFFDC2626)
val NavyAvatar = Color(0xFF13294B)
val AvatarNavy = NavyAvatar
val CoralPdfBg = Color(0xFFFDE8E8)
val CoralPdfText = Color(0xFFC5221F)
val VenueChipBg = Color(0xFFEEF2F6)
val VenueChipText = Color(0xFF334155)
val OpenAccessBg = Color(0xFFD1FAE5)
val OpenAccessText = Color(0xFF065F46)

val AlabasterBg = Color(0xFFF8FAFC)
val PureWhiteSurface = Color(0xFFFFFFFF)
val SearchPillSurface = Color(0xFFF1F5F9)
val IconTileBg = Color(0xFFE8EEF5)
val IconTileTint = Color(0xFF475569)

// Subtle pastel tints for Google Keep bento note cards
val KeepNoteSand = Color(0xFFFEF9E7)
val KeepNoteMint = Color(0xFFECFDF5)
val KeepNoteSky = Color(0xFFF0F9FF)
val KeepNoteLavender = Color(0xFFF5F3FF)
val KeepNoteRose = Color(0xFFFFF1F2)
val KeepNoteSlate = Color(0xFFF8FAFC)

val DarkBg = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkVariant = Color(0xFF334155)

// Standardized Inter font family used across Library, Navigation Drawer, Reader, Search, and Notes
val InterFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val MonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium)
)

private val LightColors = lightColorScheme(
    primary = SlatePrimary,
    onPrimary = Color.White,
    primaryContainer = TealAccentPill,
    onPrimaryContainer = OnTealAccentPill,
    secondary = NavyAvatar,
    onSecondary = Color.White,
    secondaryContainer = VenueChipBg,
    onSecondaryContainer = VenueChipText,
    tertiary = CoralPdfText,
    onTertiary = Color.White,
    tertiaryContainer = CoralPdfBg,
    onTertiaryContainer = CoralPdfText,
    background = PureWhiteSurface,
    onBackground = Color(0xFF0F172A),
    surface = PureWhiteSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SearchPillSurface,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF00332C),
    primaryContainer = Color(0xFF1E4E4A),
    onPrimaryContainer = Color(0xFFD3ECE9),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = DarkVariant,
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFFF87171),
    onTertiary = Color(0xFF450A0A),
    tertiaryContainer = Color(0xFF7F1D1D),
    onTertiaryContainer = Color(0xFFFECACA),
    background = DarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155)
)

val ZoteroTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

val ZoteroShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ZoteroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZoteroTypography,
        shapes = ZoteroShapes,
        content = content
    )
}
