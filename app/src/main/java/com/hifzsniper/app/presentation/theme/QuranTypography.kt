package com.hifzsniper.app.presentation.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.hifzsniper.app.R

val QuranFontFamily = FontFamily(
    Font(R.font.amiri_quran, FontWeight.Normal)
)

val ArabicFontFamily = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)

object QuranTypography {
    val verseText = TextStyle(
        fontFamily = QuranFontFamily,
        fontSize = 28.sp,
        lineHeight = 48.sp,
        textAlign = TextAlign.Right,
        textDirection = TextDirection.Rtl
    )

    val pageText = TextStyle(
        fontFamily = QuranFontFamily,
        fontSize = 23.sp,
        lineHeight = 44.sp,
        textAlign = TextAlign.Justify,
        textDirection = TextDirection.Rtl
    )

    val wordText = TextStyle(
        fontFamily = QuranFontFamily,
        fontSize = 28.sp,
        lineHeight = 48.sp,
        textDirection = TextDirection.Rtl
    )

    val surahBannerTitle = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 32.sp,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Rtl
    )

    val bismillahText = TextStyle(
        fontFamily = QuranFontFamily,
        fontSize = 22.sp,
        lineHeight = 36.sp,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Rtl
    )
}

fun formatArabicDigits(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { char ->
        if (char in '0'..'9') arabicDigits[char - '0'] else char
    }.joinToString("")
}

fun ayahEndSign(ayahNumber: Int): String = " \u06DD${formatArabicDigits(ayahNumber)} "

