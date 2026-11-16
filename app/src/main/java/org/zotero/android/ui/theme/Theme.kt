package org.zotero.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import org.zotero.android.R

/**
 * System-aware theme mode allowing users to follow the OS dark/light setting
 * or explicitly lock Light or Dark mode.
 */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    fun resolveIsDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }
}

// Gmail & Keep inspired scholarly palette (Light constants kept for compatibility)
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

// Subtle pastel tints for Google Keep bento note cards (Light mode)
val KeepNoteSand = Color(0xFFFEF9E7)
val KeepNoteMint = Color(0xFFECFDF5)
val KeepNoteSky = Color(0xFFF0F9FF)
val KeepNoteLavender = Color(0xFFF5F3FF)
val KeepNoteRose = Color(0xFFFFF1F2)
val KeepNoteSlate = Color(0xFFF8FAFC)

// Subtle deep tints for Google Keep bento note cards (Dark mode)
val KeepNoteSandDark = Color(0xFF2B2518)
val KeepNoteMintDark = Color(0xFF152A24)
val KeepNoteSkyDark = Color(0xFF162636)
val KeepNoteLavenderDark = Color(0xFF231D36)
val KeepNoteRoseDark = Color(0xFF311C23)
val KeepNoteSlateDark = Color(0xFF1E293B)

val DarkBg = Color(0xFF0B1120)
val DarkSurface = Color(0xFF111827)
val DarkVariant = Color(0xFF1E293B)

@Immutable
data class ItemTypeIconColors(
    val containerColor: Color,
    val iconTint: Color
)

@Immutable
data class ZoteroExtendedColors(
    val isDark: Boolean,
    val listCanvasBg: Color,
    val listCardSurface: Color,
    val accentPill: Color,
    val onAccentPill: Color,
    val pdfBadgeBg: Color,
    val pdfBadgeText: Color,
    val venueChipBg: Color,
    val venueChipText: Color,
    val openAccessBg: Color,
    val openAccessText: Color,
    val iconTileBg: Color,
    val iconTileTint: Color,
    val folderAccentTint: Color,
    val brandTitleColor: Color,
    val bentoNotePalette: List<Color>
) {
    fun itemTypeIconColors(itemType: org.zotero.android.data.model.ItemType): ItemTypeIconColors =
        if (!isDark) {
            when (itemType) {
                org.zotero.android.data.model.ItemType.JOURNAL_ARTICLE -> ItemTypeIconColors(
                    containerColor = Color(0xFFE0E7FF), // Soft Indigo base
                    iconTint = Color(0xFF3730A3)        // Deep Indigo icon
                )
                org.zotero.android.data.model.ItemType.BOOK -> ItemTypeIconColors(
                    containerColor = Color(0xFFFEF3C7), // Warm Amber base
                    iconTint = Color(0xFFB45309)        // Deep Amber icon
                )
                org.zotero.android.data.model.ItemType.CONFERENCE_PAPER -> ItemTypeIconColors(
                    containerColor = Color(0xFFCCFBF1), // Soft Teal base
                    iconTint = Color(0xFF0F766E)        // Deep Teal icon
                )
                org.zotero.android.data.model.ItemType.PREPRINT -> ItemTypeIconColors(
                    containerColor = Color(0xFFF3E8FF), // Soft Violet base
                    iconTint = Color(0xFF6D28D9)        // Deep Violet icon
                )
                org.zotero.android.data.model.ItemType.THESIS -> ItemTypeIconColors(
                    containerColor = Color(0xFFD1FAE5), // Soft Emerald base
                    iconTint = Color(0xFF047857)        // Deep Emerald icon
                )
                org.zotero.android.data.model.ItemType.REPORT -> ItemTypeIconColors(
                    containerColor = Color(0xFFFFE4E6), // Soft Rose base
                    iconTint = Color(0xFFBE123C)        // Deep Rose icon
                )
                org.zotero.android.data.model.ItemType.WEBPAGE -> ItemTypeIconColors(
                    containerColor = Color(0xFFE0F2FE), // Soft Sky base
                    iconTint = Color(0xFF0369A1)        // Deep Sky icon
                )
            }
        } else {
            when (itemType) {
                org.zotero.android.data.model.ItemType.JOURNAL_ARTICLE -> ItemTypeIconColors(
                    containerColor = Color(0xFF1E295B),
                    iconTint = Color(0xFFA5B4FC)
                )
                org.zotero.android.data.model.ItemType.BOOK -> ItemTypeIconColors(
                    containerColor = Color(0xFF3B2810),
                    iconTint = Color(0xFFFCD34D)
                )
                org.zotero.android.data.model.ItemType.CONFERENCE_PAPER -> ItemTypeIconColors(
                    containerColor = Color(0xFF113835),
                    iconTint = Color(0xFF5EEAD4)
                )
                org.zotero.android.data.model.ItemType.PREPRINT -> ItemTypeIconColors(
                    containerColor = Color(0xFF2E1A47),
                    iconTint = Color(0xFFD8B4FE)
                )
                org.zotero.android.data.model.ItemType.THESIS -> ItemTypeIconColors(
                    containerColor = Color(0xFF0F3528),
                    iconTint = Color(0xFF6EE7B7)
                )
                org.zotero.android.data.model.ItemType.REPORT -> ItemTypeIconColors(
                    containerColor = Color(0xFF3F1624),
                    iconTint = Color(0xFFFDA4AF)
                )
                org.zotero.android.data.model.ItemType.WEBPAGE -> ItemTypeIconColors(
                    containerColor = Color(0xFF122F44),
                    iconTint = Color(0xFF7DD3FC)
                )
            }
        }
}

val LightExtendedColors = ZoteroExtendedColors(
    isDark = false,
    listCanvasBg = Color(0xFFF1F5F9),
    listCardSurface = PureWhiteSurface,
    accentPill = TealAccentPill,
    onAccentPill = OnTealAccentPill,
    pdfBadgeBg = CoralPdfBg,
    pdfBadgeText = CoralPdfText,
    venueChipBg = VenueChipBg,
    venueChipText = VenueChipText,
    openAccessBg = OpenAccessBg,
    openAccessText = OpenAccessText,
    iconTileBg = IconTileBg,
    iconTileTint = IconTileTint,
    folderAccentTint = Color(0xFF3B6A9A),
    brandTitleColor = Color(0xFF172B4D),
    bentoNotePalette = listOf(
        KeepNoteSand,
        KeepNoteMint,
        KeepNoteSky,
        KeepNoteLavender,
        KeepNoteRose,
        KeepNoteSlate
    )
)

val DarkExtendedColors = ZoteroExtendedColors(
    isDark = true,
    listCanvasBg = DarkBg,
    listCardSurface = Color(0xFF151F32),
    accentPill = Color(0xFF1E4E4A),
    onAccentPill = Color(0xFFD3ECE9),
    pdfBadgeBg = Color(0xFF4C1D1D),
    pdfBadgeText = Color(0xFFFCA5A5),
    venueChipBg = Color(0xFF263346),
    venueChipText = Color(0xFFE2E8F0),
    openAccessBg = Color(0xFF064E3B),
    openAccessText = Color(0xFF6EE7B7),
    iconTileBg = Color(0xFF1E293B),
    iconTileTint = Color(0xFFCBD5E1),
    folderAccentTint = Color(0xFF7DD3FC),
    brandTitleColor = Color(0xFFF1F5F9),
    bentoNotePalette = listOf(
        KeepNoteSandDark,
        KeepNoteMintDark,
        KeepNoteSkyDark,
        KeepNoteLavenderDark,
        KeepNoteRoseDark,
        KeepNoteSlateDark
    )
)

val LocalZoteroExtendedColors = staticCompositionLocalOf { LightExtendedColors }

object ZoteroThemeTokens {
    val colors: ZoteroExtendedColors
        @Composable
        get() = LocalZoteroExtendedColors.current
}

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
    onBackground = Color(0xFFF8FAFC),
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
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalZoteroExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ZoteroTypography,
            shapes = ZoteroShapes,
            content = content
        )
    }
}
