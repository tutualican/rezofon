package studio.rezofon.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import studio.rezofon.app.R

@Immutable
data class RzPalette(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceSunken: Color,
    val outline: Color,
    val lime: Color,
    val amber: Color,
    val cyan: Color,
    val violet: Color,
    val text: Color,
    val textDim: Color,
    val danger: Color,
    val isDark: Boolean,
)

private val DarkPalette = RzPalette(
    bg = Color(0xFF08090B),
    surface = Color(0xFF11131A),
    surfaceAlt = Color(0xFF191C24),
    surfaceSunken = Color(0xFF0C0E13),
    outline = Color(0x1FFFFFFF),
    lime = Color(0xFFC8FF3D),
    amber = Color(0xFFFFB03A),
    cyan = Color(0xFF7BE8DC),
    violet = Color(0xFFBCA8FF),
    text = Color(0xFFF1F3EC),
    textDim = Color(0xFF9AA1AA),
    danger = Color(0xFFFF6B5E),
    isDark = true,
)

private val LightPalette = RzPalette(
    bg = Color(0xFFF1F2EA),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFE9EAE1),
    surfaceSunken = Color(0xFFDCDED3),
    outline = Color(0x14000000),
    lime = Color(0xFF4E6B00),
    amber = Color(0xFF8A4B00),
    cyan = Color(0xFF00695F),
    violet = Color(0xFF5B45A8),
    text = Color(0xFF14160F),
    textDim = Color(0xFF5C6157),
    danger = Color(0xFFB3261E),
    isDark = false,
)

val LocalRzPalette = staticCompositionLocalOf { DarkPalette }

object RzMotion {
    val tile: androidx.compose.animation.core.SpringSpec<Float> =
        spring(dampingRatio = 0.62f, stiffness = 480f)
    val press: androidx.compose.animation.core.SpringSpec<Float> =
        spring(dampingRatio = 0.42f, stiffness = 900f)
    val glide: androidx.compose.animation.core.SpringSpec<Float> =
        spring(dampingRatio = 0.78f, stiffness = 260f)
    val slow: androidx.compose.animation.core.SpringSpec<Float> =
        spring(dampingRatio = 0.9f, stiffness = 120f)
    val spatial: androidx.compose.animation.core.SpringSpec<androidx.compose.ui.unit.Dp> =
        spring(dampingRatio = 0.62f, stiffness = 480f)
    fun <T> expressive() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
}

private fun RzFont(res: Int, weight: FontWeight): Font = Font(
    resId = res,
    weight = weight,
)

val DisplayFamily = FontFamily(
    RzFont(R.font.space_grotesk, FontWeight.Light),
    RzFont(R.font.space_grotesk, FontWeight.Normal),
    RzFont(R.font.space_grotesk, FontWeight.Medium),
    RzFont(R.font.space_grotesk, FontWeight.SemiBold),
    RzFont(R.font.space_grotesk, FontWeight.Bold),
)

val BodyFamily = FontFamily(
    RzFont(R.font.manrope, FontWeight.Light),
    RzFont(R.font.manrope, FontWeight.Normal),
    RzFont(R.font.manrope, FontWeight.Medium),
    RzFont(R.font.manrope, FontWeight.SemiBold),
    RzFont(R.font.manrope, FontWeight.Bold),
)

val RzTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Medium,
        fontSize = 54.sp, lineHeight = 56.sp, letterSpacing = (-1.8).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Medium,
        fontSize = 42.sp, lineHeight = 44.sp, letterSpacing = (-1.4).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Medium,
        fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 0.9.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp, lineHeight = 12.sp, letterSpacing = 1.2.sp,
    ),
)

@Composable
fun RezofonTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.lime,
            onPrimary = Color(0xFF15180A),
            secondary = palette.amber,
            onSecondary = Color(0xFF1A1104),
            tertiary = palette.cyan,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.textDim,
            outline = Color(0xFF3A3F47),
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.lime,
            onPrimary = Color.White,
            secondary = palette.amber,
            onSecondary = Color.White,
            tertiary = palette.cyan,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.textDim,
            outline = Color(0xFFB7BBB0),
            error = palette.danger,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalRzPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = RzTypography,
            content = content,
        )
    }
}
