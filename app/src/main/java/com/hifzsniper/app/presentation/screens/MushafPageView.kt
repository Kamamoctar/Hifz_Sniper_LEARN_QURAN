package com.hifzsniper.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifzsniper.app.domain.PageAyahView
import com.hifzsniper.app.domain.BasmalaPolicy
import com.hifzsniper.app.domain.SurahSummary
import com.hifzsniper.app.presentation.theme.QuranFontFamily
import com.hifzsniper.app.presentation.theme.QuranTypography
import com.hifzsniper.app.presentation.theme.ayahEndSign
import com.hifzsniper.app.presentation.theme.formatArabicDigits

@Composable
fun MushafPageView(
    pageNumber: Int,
    ayahs: List<PageAyahView>,
    surahs: List<SurahSummary>,
    selectedVerseKey: String?,
    modifier: Modifier = Modifier,
    startAyahInSet: Int? = null,
    endAyahInSet: Int? = null,
    currentSetSurahId: Int? = null,
    isPlaying: Boolean = false,
    onSelectAyah: (Int, Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onOpenPageDialog: () -> Unit,
    onDownloadPageAudio: () -> Unit
) {
    val surahsById = surahs.associateBy { it.id }
    val firstAyah = ayahs.firstOrNull()
    val surahInfo = firstAyah?.let { surahsById[it.surahId] }

    val juzNumber = firstAyah?.juzNumber ?: 1

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Juz · Surah name · Page number
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الجزء ${formatArabicDigits(juzNumber)} · الحزب ${formatArabicDigits(firstAyah?.hizbNumber ?: 1)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = surahInfo?.nameArabic ?: "",
                    style = QuranTypography.surahBannerTitle.copy(fontSize = 16.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Page $pageNumber / 604",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mushaf Page Body
            if (ayahs.isEmpty()) {
                Text(
                    "Chargement de la page $pageNumber…",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                // Group ayahs by surah if page crosses surahs
                val ayahsBySurah = ayahs.groupBy { it.surahId }

                ayahsBySurah.forEach { (surahId, surahAyahs) ->
                    val surahMeta = surahsById[surahId]

                    // If first ayah of this surah is on this page, render Surah Banner
                    if (surahAyahs.any { it.ayahNumber == 1 }) {
                        SurahBanner(
                            surahId = surahId,
                            nameArabic = surahMeta?.nameArabic ?: "سورة",
                            nameTransliterated = surahMeta?.nameTransliterated ?: "",
                            ayahCount = surahMeta?.ayahCount ?: surahAyahs.size
                        )
                        if (BasmalaPolicy.shouldRenderHeaderBasmala(surahId)) {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                style = QuranTypography.bismillahText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    // Continuous page typography: each verse remains clickable through annotations.
                    val continuousPageText = buildAnnotatedString {
                        surahAyahs.forEachIndexed { index, ayah ->
                            val isCurrentAyah = ayah.verseKey == selectedVerseKey
                            val isInActiveSet = currentSetSurahId == ayah.surahId &&
                                startAyahInSet != null && endAyahInSet != null &&
                                ayah.ayahNumber in startAyahInSet..endAyahInSet
                            val cleanAyahText = BasmalaPolicy.formatAyahTextForMushaf(
                                surahId = ayah.surahId,
                                ayahNumber = ayah.ayahNumber,
                                textUthmani = ayah.textUthmani
                            )
                            withLink(
                                LinkAnnotation.Clickable(
                                    tag = ayah.verseKey,
                                    linkInteractionListener = { onSelectAyah(ayah.surahId, ayah.ayahNumber) }
                                )
                            ) {
                                withStyle(
                                    SpanStyle(
                                        background = when {
                                            isCurrentAyah && isPlaying -> MaterialTheme.colorScheme.primaryContainer
                                            isCurrentAyah -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                            isInActiveSet -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                        },
                                        fontWeight = if (isCurrentAyah) FontWeight.Bold else FontWeight.Normal
                                    )
                                ) {
                                    append(cleanAyahText)
                                    withStyle(
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    ) {
                                        append(ayahEndSign(ayah.ayahNumber))
                                    }
                                }
                            }
                            if (index != surahAyahs.lastIndex) append(' ')
                        }
                    }
                    Text(
                        text = continuousPageText,
                        style = QuranTypography.pageText.copy(textAlign = TextAlign.Justify),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Footer navigation: Prev Page · Jump · Next Page
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPreviousPage,
                    enabled = pageNumber > 1
                ) {
                    Text("◀ Page ${pageNumber - 1}")
                }

                OutlinedButton(onClick = onOpenPageDialog) {
                    Text("Page $pageNumber 📖")
                }

                OutlinedButton(
                    onClick = onNextPage,
                    enabled = pageNumber < 604
                ) {
                    Text("Page ${pageNumber + 1} ▶")
                }
            }

            // Batch Download Button
            OutlinedButton(
                onClick = onDownloadPageAudio,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📥 Télécharger l'audio de la page (${ayahs.size} versets)")
            }
        }
    }
}

@Composable
fun SurahBanner(
    surahId: Int,
    nameArabic: String,
    nameTransliterated: String,
    ayahCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "سُورَةُ $nameArabic",
                style = QuranTypography.surahBannerTitle,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Sourate $surahId · $nameTransliterated · $ayahCount versets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

