package io.github.tobsef.svgokt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Dark theme with a red accent — the same palette the Python front-end used. */
public object Palette {
    public val Bg: Color = Color(0xFF161616)
    public val Panel: Color = Color(0xFF1F1F1F)
    public val Panel2: Color = Color(0xFF262626)
    public val Border: Color = Color(0xFF333333)
    public val Text: Color = Color(0xFFECECEC)
    public val TextDim: Color = Color(0xFF9A9A9A)
    public val Red: Color = Color(0xFFE23744)
    public val RedHover: Color = Color(0xFFC02734)
    public val Green: Color = Color(0xFF4CAF7D)
    public val White: Color = Color(0xFFFFFFFF)
}

private val SvgPawColors = darkColorScheme(
    primary = Palette.Red,
    onPrimary = Palette.White,
    primaryContainer = Palette.Red,
    onPrimaryContainer = Palette.White,
    secondary = Palette.Panel2,
    onSecondary = Palette.Text,
    background = Palette.Bg,
    onBackground = Palette.Text,
    surface = Palette.Panel,
    onSurface = Palette.Text,
    surfaceVariant = Palette.Panel2,
    onSurfaceVariant = Palette.TextDim,
    outline = Palette.Border,
    error = Palette.Red,
    onError = Palette.White,
)

private val SvgPawTypography = Typography(
    bodyMedium = TextStyle(fontSize = 13.sp, color = Palette.Text),
    bodySmall = TextStyle(fontSize = 11.sp, color = Palette.TextDim),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.Text),
)

@Composable
public fun SvgPawTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SvgPawColors,
        typography = SvgPawTypography,
        content = content,
    )
}
