package dev.bloc.sample.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Colour primitives
// ─────────────────────────────────────────────────────────────────────────────

// Background / structure
val BlocNavy         = Color(0xFF080E1A)
val BlocNavyLight    = Color(0xFF0D1B2A)
val BlocSurface      = Color(0xFF111827)
val BlocSurfaceAlt   = Color(0xFF151F2E)
val BlocSurfaceVar   = Color(0xFF1A2435)
val BlocBorder       = Color(0xFF1E2E40)

// Text
val BlocTextPrimary   = Color.White
val BlocTextSecondary = Color(0xFF8892A4)
val BlocTextTertiary  = Color(0xFF4A5568)

// Shared accent
val BlocAccentCyan = Color(0xFF00BCD4)

// ─────────────────────────────────────────────────────────────────────────────
// Per-example accent colours
// (intentionally kept here as a central token source; each screen reads its own)
// ─────────────────────────────────────────────────────────────────────────────

object ExamplePalette {
    // Counter — cyan (mirrors iOS)
    val counterPrimary  = Color(0xFF00BCD4)
    val counterVariant  = Color(0xFF00E5FF)

    // Stopwatch — mint-green teal
    val stopwatchPrimary = Color(0xFF00C9A7)
    val stopwatchVariant = Color(0xFF00F5B8)

    // Calculator — amber / warm gold
    val calculatorPrimary = Color(0xFFFFA726)
    val calculatorVariant = Color(0xFFFFCC02)

    // Heartbeat — vivid rose-red
    val heartbeatPrimary = Color(0xFFE53935)
    val heartbeatVariant = Color(0xFFFF5252)

    // Score Board — electric violet
    val scorePrimary  = Color(0xFF7C4DFF)
    val scoreVariant  = Color(0xFFB388FF)

    // Formula One — Ferrari red
    val f1Primary = Color(0xFFE8002D)
    val f1Variant = Color(0xFFFF1E00)

    // Lorcana — deep purple / galaxy
    val lorcanaPrimary = Color(0xFF6A1B9A)
    val lorcanaVariant = Color(0xFFCE93D8)
    val lorcanaGold    = Color(0xFFFFD54F)
}

// ─────────────────────────────────────────────────────────────────────────────
// Material 3 colour scheme
// ─────────────────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary          = BlocAccentCyan,
    onPrimary        = BlocNavy,
    primaryContainer = BlocSurfaceVar,
    background       = BlocNavy,
    surface          = BlocSurface,
    surfaceVariant   = BlocSurfaceVar,
    onBackground     = BlocTextPrimary,
    onSurface        = BlocTextPrimary,
    onSurfaceVariant = BlocTextSecondary,
    outline          = BlocBorder,
    outlineVariant   = Color(0xFF0F1D2B),
    scrim            = Color(0x99000000),
)

// ─────────────────────────────────────────────────────────────────────────────
// Typography scale — mirrors iOS Theme.Font semantic levels
// ─────────────────────────────────────────────────────────────────────────────

private val BlocTypography = Typography(
    // micro  →  9 sp  (status badges, tiny annotations)
    // tiny   → 10 sp  (timestamps, small labels)
    // caption → 11 sp
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    // footnote → 12 sp (log messages, compact labels)
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    // body → 13 sp (standard body text)
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    // callout → 14 sp (prominent body, button labels)
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    // subhead → 16 sp (subheadings, major buttons)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    // label / action buttons
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    // headline → 18 sp (section headings)
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    // title3 → 22 sp (calculator keys, prominent values)
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    // title → 28 sp (screen section titles)
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp,
    ),
    // title2 → 32 sp (hero section titles)
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    // display-level (large counter numerals, hero numbers)
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 64.sp,
        lineHeight = 72.sp,
        letterSpacing = (-2.sp).value.sp,
    ),
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 96.sp,
        lineHeight = 104.sp,
        letterSpacing = (-3.sp).value.sp,
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
// BlocTheme — central design-token namespace
// Mirrors iOS Theme enum structure. Import this in screen composables:
//
//   import dev.bloc.sample.ui.theme.BlocTheme
//
//   Text("Hello", style = BlocTheme.typography.headlineMedium)
//   Spacer(modifier = Modifier.height(BlocTheme.spacing.lg))
//   Box(modifier = Modifier.clip(RoundedCornerShape(BlocTheme.radius.xl)))
// ─────────────────────────────────────────────────────────────────────────────

object BlocTheme {

    // ── Palette ───────────────────────────────────────────────────────────────
    // Shared chrome colours — glass-morphism fill layers + text hierarchy.
    // Per-screen accents live in ExamplePalette above.

    object Palette {
        // Text hierarchy
        val textPrimary:    Color = BlocTextPrimary
        val textSecondary:  Color = BlocTextSecondary
        val textTertiary:   Color = BlocTextTertiary
        val textQuaternary: Color = Color.White.copy(alpha = 0.35f)
        val textDisabled:   Color = Color.White.copy(alpha = 0.20f)
        val textHint:       Color = Color.White.copy(alpha = 0.15f)

        // Surfaces (glass morphism layers)
        val surfaceUltraSubtle: Color = Color.White.copy(alpha = 0.03f)
        val surfaceSubtle:      Color = Color.White.copy(alpha = 0.05f)
        val surface:            Color = Color.White.copy(alpha = 0.08f)
        val surfaceMedium:      Color = Color.White.copy(alpha = 0.10f)

        // Borders / strokes
        val borderFaint:  Color = Color.White.copy(alpha = 0.06f)
        val border:       Color = Color.White.copy(alpha = 0.08f)
        val borderMedium: Color = Color.White.copy(alpha = 0.12f)
        val borderStrong: Color = Color.White.copy(alpha = 0.20f)

        // Dividers
        val divider: Color = Color.White.copy(alpha = 0.08f)

        // Semantic aliases
        val background: Color = BlocNavy
        val card:       Color = BlocSurface
        val cardAlt:    Color = BlocSurfaceAlt
    }

    // ── Spacing ───────────────────────────────────────────────────────────────
    // Common padding / gap values — mirrors iOS Theme.Spacing.

    object Spacing {
        val xxxs: Dp = 3.dp
        val xxs:  Dp = 4.dp
        val xs:   Dp = 6.dp
        val sm:   Dp = 8.dp
        val md:   Dp = 12.dp
        val lg:   Dp = 16.dp
        val xl:   Dp = 20.dp
        val xxl:  Dp = 24.dp
        val xxxl: Dp = 32.dp
        val huge: Dp = 40.dp
        val max:  Dp = 48.dp
    }

    // ── Radius ────────────────────────────────────────────────────────────────
    // Common corner radii — mirrors iOS Theme.Radius.

    object Radius {
        val xs:   Dp = 4.dp
        val sm:   Dp = 8.dp
        val md:   Dp = 10.dp
        val lg:   Dp = 12.dp
        val xl:   Dp = 14.dp
        val xxl:  Dp = 16.dp
        val xxxl: Dp = 20.dp
        val huge: Dp = 24.dp
        val full: Dp = 999.dp   // pill / circle
    }

    // ── Typography ────────────────────────────────────────────────────────────
    // Semantic font sizes — mirrors iOS Theme.Font.
    // Accessing via BlocTheme.typography forwards to MaterialTheme.typography,
    // so callers get the full Material type system with our custom sizes.

    val typography: Typography get() = BlocTypography

    // Convenience sp values for Modifier / Canvas usage
    object FontSize {
        val micro:    TextUnit = 9.sp
        val tiny:     TextUnit = 10.sp
        val caption:  TextUnit = 11.sp
        val footnote: TextUnit = 12.sp
        val body:     TextUnit = 13.sp
        val callout:  TextUnit = 14.sp
        val subhead:  TextUnit = 16.sp
        val headline: TextUnit = 18.sp
        val title3:   TextUnit = 22.sp
        val title:    TextUnit = 28.sp
        val title2:   TextUnit = 32.sp
        // display sizes — for hero numerals, counters, etc.
        val display1: TextUnit = 48.sp
        val display2: TextUnit = 64.sp
        val display3: TextUnit = 96.sp
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BlocTheme Composable — wraps MaterialTheme with Bloc's design tokens
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BlocTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = BlocTypography,
        content     = content,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// MaterialTheme shortcuts — call these from composables to access tokens
// ─────────────────────────────────────────────────────────────────────────────

/** Shorthand for [MaterialTheme.colorScheme]. */
val MaterialTheme.blocColors
    @Composable @ReadOnlyComposable get() = colorScheme
