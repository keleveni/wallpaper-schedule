package com.keleveni.wallpaperscheduler.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList

/**
 * UI は日本語のみのため、端末の言語設定に関わらず日本語の字形で表示する。
 * 指定しないと、日本語以外の言語設定の端末では漢字が中国語の字形になる。
 */
private val JapaneseTypography = Typography().run {
    fun TextStyle.ja() = copy(localeList = LocaleList("ja"))
    copy(
        displayLarge = displayLarge.ja(),
        displayMedium = displayMedium.ja(),
        displaySmall = displaySmall.ja(),
        headlineLarge = headlineLarge.ja(),
        headlineMedium = headlineMedium.ja(),
        headlineSmall = headlineSmall.ja(),
        titleLarge = titleLarge.ja(),
        titleMedium = titleMedium.ja(),
        titleSmall = titleSmall.ja(),
        bodyLarge = bodyLarge.ja(),
        bodyMedium = bodyMedium.ja(),
        bodySmall = bodySmall.ja(),
        labelLarge = labelLarge.ja(),
        labelMedium = labelMedium.ja(),
        labelSmall = labelSmall.ja(),
    )
}

@Composable
fun WallpaperSchedulerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, typography = JapaneseTypography, content = content)
}
