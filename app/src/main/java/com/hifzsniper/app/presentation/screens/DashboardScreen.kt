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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifzsniper.app.domain.DayActivity
import com.hifzsniper.app.presentation.AppUiState

@Composable
fun DashboardScreen(state: AppUiState, modifier: Modifier = Modifier) = PageColumn(modifier) {
    Text("Progression & Assiduité", style = MaterialTheme.typography.headlineMedium)

    // Streak Card 🔥
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Série active", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${state.stats.currentStreak}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(" jours 🔥", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Record", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${state.stats.bestStreak} jours 🏆",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                when {
                    state.stats.currentStreak >= 7 -> "Masha'Allah ! Une semaine complète d'assiduité. La clé du Hifz est la régularité."
                    state.stats.currentStreak >= 3 -> "Excellent élan ! Continue chaque jour pour ancrer tes révisions."
                    state.stats.currentStreak >= 1 -> "Séance accomplie aujourd'hui ! Reviens demain pour prolonger la série."
                    else -> "Fais au moins une révision aujourd'hui pour démarrer ta série !"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Heatmap Card (28 derniers jours / 4 semaines)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Activité des 4 dernières semaines", style = MaterialTheme.typography.titleMedium)

            ActivityHeatmapGrid(activities = state.stats.recentActivity)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Moins ", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                ActivityLevelBox(count = 0)
                ActivityLevelBox(count = 2)
                ActivityLevelBox(count = 6)
                ActivityLevelBox(count = 12)
                Text(" Plus", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
            }
        }
    }

    // Stats Grid
    val goal = state.preferences.dailyAyahGoal.coerceAtLeast(1)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Objectif quotidien", style = MaterialTheme.typography.titleMedium)
            Text("${state.stats.dailyCompletedAyahs} / $goal versets travaillés aujourd'hui")
            LinearProgressIndicator(
                progress = { (state.stats.dailyCompletedAyahs.toFloat() / goal).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    StatCard("Révisions totales", state.stats.totalReviews.toString())
    StatCard("Taux de fluidité", "${state.stats.successRate} %")
    StatCard("Cartes dues aujourd'hui", state.stats.dueCards.toString())
    StatCard("Temps estimé aujourd'hui", formatStudyTime(state.stats.estimatedSecondsToday))
    StatCard("Temps estimé sur 7 jours", formatStudyTime(state.stats.estimatedSecondsLast7Days))
    StatCard("Temps estimé sur 30 jours", formatStudyTime(state.stats.estimatedSecondsLast30Days))
    StatCard("Versets actifs / révisés", "${state.stats.learnedAyahs} / ${state.stats.reviewedAyahs}")
    StatCard("Stabilité moyenne", "%.1f".format(state.stats.averageStability))
    StatCard("Charge à venir", "${state.stats.dueNext24Hours} sous 24 h · ${state.stats.dueNext7Days} sous 7 j")
    StatCard("Points faibles actifs", state.weakPoints.size.toString())
    StatCard("Échecs à consolider", state.stats.failedReviews.toString())

    Text(
        "Les durées sont estimées à partir des cartes réellement révisées. Les statistiques Room survivent aux redémarrages.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun formatStudyTime(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
}

@Composable
fun ActivityHeatmapGrid(activities: List<DayActivity>) {
    // 28 days chunked into 4 weeks of 7 days
    val weeks = activities.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                week.forEach { day ->
                    val color = when {
                        day.count == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        day.count in 1..3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        day.count in 4..9 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color)
                            .border(
                                width = if (day.isToday) 2.dp else 1.dp,
                                color = if (day.isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day.count > 0) {
                            Text(
                                "${day.count}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (day.count >= 4) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityLevelBox(count: Int) {
    val color = when {
        count == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        count in 1..3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        count in 4..9 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(12.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}
