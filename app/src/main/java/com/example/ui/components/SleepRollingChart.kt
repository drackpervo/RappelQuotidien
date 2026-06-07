package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LegendToggle
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SleepSummary
import java.util.Calendar
import java.util.Locale

data class SleepChartData(
    val dateKey: String,
    val label: String,
    val actualHours: Float,
    val movingAvgHours: Float
)

@Composable
fun SleepRollingChart(
    sleepRecords: List<SleepSummary>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val targetLineColor = Color(0xFF2E7D32) // Vert de réussite pour l'objectif de 8h
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    // Trier les enregistrements par date croissante
    val sortedSummaries = remember(sleepRecords) {
        sleepRecords.sortedBy { it.dateKey }
    }

    // Préparer les données pour les 7 derniers jours et calculer la moyenne mobile simple (SMA-3)
    val chartData = remember(sortedSummaries) {
        val result = mutableListOf<SleepChartData>()
        val last7 = sortedSummaries.takeLast(7)
        for (i in last7.indices) {
            val item = last7[i]
            val fullIndex = sortedSummaries.indexOf(item)

            var sumMinutes = 0
            var count = 0
            if (fullIndex != -1) {
                // Moyenne mobile de 3 jours (jour J, J-1, J-2)
                for (offset in 0..2) {
                    val idx = fullIndex - offset
                    if (idx >= 0) {
                        sumMinutes += sortedSummaries[idx].durationMinutes
                        count++
                    }
                }
            }
            val movingAvgMins = if (count > 0) sumMinutes / count else item.durationMinutes

            result.add(
                SleepChartData(
                    dateKey = item.dateKey,
                    label = getShortFrenchDayLabel(item.dateKey),
                    actualHours = item.durationMinutes.toFloat() / 60f,
                    movingAvgHours = movingAvgMins.toFloat() / 60f
                )
            )
        }
        result
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sleep_rolling_chart_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // En-tête du graphique
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Analyse & Tendance 📊",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Moyenne mobile glissante sur 3 jours",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (chartData.isEmpty()) {
                // État vide
                EmptyTrendState()
            } else {
                // Légende interactive élégante
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Courbe Réelle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Heures réelles",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Courbe Moyenne Mobile
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(4.dp)
                                .background(tertiaryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Moyenne mobile (3 j.)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Objectif 8h
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(1.dp)
                                .background(targetLineColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cible (8h)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = targetLineColor
                        )
                    }
                }

                // Trouver dynamiquement la valeur maximale réelle ou moyenne
                val maxVal = remember(chartData) {
                    val peakValue = chartData.flatMap { listOf(it.actualHours, it.movingAvgHours) }.maxOrNull() ?: 8f
                    maxOf(peakValue + 1.5f, 10f) // minimum 10h d'échelle de graphique
                }

                // Animation globale du tracé
                val animatedProgress by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1000),
                    label = "trend_animation"
                )

                // Dessin du graphique Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = 10.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 45.dp.toPx()
                    val paddingBottom = 40.dp.toPx()
                    val paddingTop = 15.dp.toPx()
                    val paddingRight = 15.dp.toPx()

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    // 1. Dessiner les lignes horizontales à 4h, 8h (cible), et au max (12h ou maxVal)
                    val gridSteps = listOf(4f, 8f, 12f).filter { it <= maxVal }
                    for (step in gridSteps) {
                        val y = paddingTop + chartHeight * (1.0f - step / maxVal)
                        drawLine(
                            color = if (step == 8f) targetLineColor.copy(alpha = 0.5f) else gridColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = if (step == 8f) 1.5.dp.toPx() else 1.dp.toPx(),
                            pathEffect = if (step == 8f) PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f) else null
                        )

                        // Label de l'heure
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "${step.toInt()}h",
                            topLeft = Offset(8.dp.toPx(), y - 10.dp.toPx()),
                            style = TextStyle(
                                color = if (step == 8f) targetLineColor else onSurfaceVariantColor,
                                fontSize = 10.sp,
                                fontWeight = if (step == 8f) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }

                    // Calculer les coordonnées des points
                    val pointsActual = mutableListOf<Offset>()
                    val pointsAvg = mutableListOf<Offset>()
                    val stepX = if (chartData.size > 1) chartWidth / (chartData.size - 1) else chartWidth

                    for (i in chartData.indices) {
                        val x = paddingLeft + i * stepX
                        
                        val yActual = paddingTop + chartHeight * (1.0f - (chartData[i].actualHours / maxVal))
                        val yAvg = paddingTop + chartHeight * (1.0f - (chartData[i].movingAvgHours / maxVal))

                        pointsActual.add(Offset(x, yActual))
                        pointsAvg.add(Offset(x, yAvg))
                    }

                    // 2. Tracé de l'aire ombrée sous les Heures Réelles
                    if (pointsActual.size >= 2) {
                        val fillPath = Path().apply {
                            moveTo(pointsActual.first().x, paddingTop + chartHeight)
                            for (i in pointsActual.indices) {
                                if (i == 0) {
                                    lineTo(pointsActual[i].x, pointsActual[i].y)
                                } else {
                                    val prev = pointsActual[i - 1]
                                    val curr = pointsActual[i]
                                    val conX1 = prev.x + (curr.x - prev.x) / 2
                                    val conY1 = prev.y
                                    val conX2 = prev.x + (curr.x - prev.x) / 2
                                    val conY2 = curr.y
                                    cubicTo(conX1, conY1, conX2, conY2, curr.x, curr.y)
                                }
                            }
                            lineTo(pointsActual.last().x, paddingTop + chartHeight)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.22f), Color.Transparent),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        // 3. Tracé de la ligne continue (Heures Réelles)
                        val strokeActualPath = Path().apply {
                            for (i in pointsActual.indices) {
                                if (i == 0) {
                                    moveTo(pointsActual[i].x, pointsActual[i].y)
                                } else {
                                    val prev = pointsActual[i - 1]
                                    val curr = pointsActual[i]
                                    val conX1 = prev.x + (curr.x - prev.x) / 2
                                    val conY1 = prev.y
                                    val conX2 = prev.x + (curr.x - prev.x) / 2
                                    val conY2 = curr.y
                                    cubicTo(conX1, conY1, conX2, conY2, curr.x, curr.y)
                                }
                            }
                        }
                        drawPath(
                            path = strokeActualPath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // 4. Tracé de la ligne pointillée (Moyenne Mobile Simple)
                        val strokeAvgPath = Path().apply {
                            for (i in pointsAvg.indices) {
                                if (i == 0) {
                                    moveTo(pointsAvg[i].x, pointsAvg[i].y)
                                } else {
                                    val prev = pointsAvg[i - 1]
                                    val curr = pointsAvg[i]
                                    val conX1 = prev.x + (curr.x - prev.x) / 2
                                    val conY1 = prev.y
                                    val conX2 = prev.x + (curr.x - prev.x) / 2
                                    val conY2 = curr.y
                                    cubicTo(conX1, conY1, conX2, conY2, curr.x, curr.y)
                                }
                            }
                        }
                        drawPath(
                            path = strokeAvgPath,
                            color = tertiaryColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f)
                            )
                        )
                    }

                    // 5. Marqueurs et Textes interactifs
                    for (i in chartData.indices) {
                        val x = paddingLeft + i * stepX
                        val ptActual = pointsActual[i]
                        val ptAvg = pointsAvg[i]

                        // Point marqueur réel
                        drawCircle(
                            color = primaryColor,
                            radius = 4.5.dp.toPx(),
                            center = ptActual
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = ptActual
                        )

                        // Afficher la valeur numérique réelle en haut de la ligne réelle (si < maxVal)
                        val hourValStr = "%.1f".format(chartData[i].actualHours) + "h"
                        val progressTextMeasured = textMeasurer.measure(hourValStr)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = hourValStr,
                            topLeft = Offset(ptActual.x - (progressTextMeasured.size.width / 2), ptActual.y - 20.dp.toPx()),
                            style = TextStyle(
                                color = onSurfaceColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        // Label de l'axe X (jour)
                        val dayLabel = chartData[i].label
                        val labelMeasured = textMeasurer.measure(dayLabel)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = dayLabel,
                            topLeft = Offset(x - (labelMeasured.size.width / 2), height - 25.dp.toPx()),
                            style = TextStyle(
                                color = onSurfaceVariantColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTrendState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Analyse tendancielle indisponible.",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Enregistrez au moins 2 nuits d'historique pour configurer le tracé de la moyenne mobile glissante.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp)
        )
    }
}

private fun getShortFrenchDayLabel(dateKey: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = sdf.parse(dateKey) ?: return dateKey.takeLast(5)
        // Utiliser "E dd" pour "Lun. 07" etc.
        val dayFormat = java.text.SimpleDateFormat("E dd", java.util.Locale.FRENCH)
        dayFormat.format(date).replace(".", "")
    } catch (e: Exception) {
        dateKey.takeLast(5)
    }
}
