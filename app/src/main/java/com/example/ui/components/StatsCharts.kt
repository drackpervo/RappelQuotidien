package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composant de statistiques : Graphique linéaire pour visualiser la progression hebdomadaire.
 * Dessine une courbe de Bezier lisse avec un dégradé élégant sous le tracé.
 */
@Composable
fun WeeklyLineChart(
    daysData: List<Pair<String, Float>>, // Liste de Lundi/Mardi -> Taux d'accomplissement (0.0f à 1.0f)
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Progression des 7 Derniers Jours",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (daysData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune donnée disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 40.dp.toPx()
                    val paddingBottom = 40.dp.toPx()
                    val paddingTop = 10.dp.toPx()
                    val paddingRight = 10.dp.toPx()

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    // 1. Dessiner les lignes de fond horizontales (0 %, 50 %, 100 %)
                    val gridLines = listOf(0.0f, 0.5f, 1.0f)
                    for (line in gridLines) {
                        val y = paddingTop + chartHeight * (1.0f - line)
                        drawLine(
                            color = surfaceVariantColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        // Afficher le pourcentage d'axe Y
                        val percentLabel = "${(line * 100).toInt()}%"
                        drawText(
                            textMeasurer = textMeasurer,
                            text = percentLabel,
                            topLeft = Offset(5.dp.toPx(), y - 10.dp.toPx()),
                            style = TextStyle(color = primaryColor, fontSize = 10.sp)
                        )
                    }

                    // 2. Calculer les points sur le graphique
                    val points = mutableListOf<Offset>()
                    val stepX = if (daysData.size > 1) chartWidth / (daysData.size - 1) else chartWidth

                    for (i in daysData.indices) {
                        val x = paddingLeft + i * stepX
                        val y = paddingTop + chartHeight * (1.0f - daysData[i].second)
                        points.add(Offset(x, y))
                    }

                    // 3. Dessiner le dégradé sous le tracé si nous avons assez de points
                    if (points.size >= 2) {
                        val fillPath = Path().apply {
                            moveTo(points.first().x, paddingTop + chartHeight) // Départ en bas à gauche
                            for (i in points.indices) {
                                if (i == 0) {
                                    lineTo(points[i].x, points[i].y)
                                } else {
                                    // Utilisation de points de contrôle pour des courbes de Bézier fluides
                                    val prev = points[i - 1]
                                    val curr = points[i]
                                    val conX1 = prev.x + (curr.x - prev.x) / 2
                                    val conY1 = prev.y
                                    val conX2 = prev.x + (curr.x - prev.x) / 2
                                    val conY2 = curr.y
                                    cubicTo(conX1, conY1, conX2, conY2, curr.x, curr.y)
                                }
                            }
                            lineTo(points.last().x, paddingTop + chartHeight) // Arrivée en bas à droite
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        // 4. Dessiner le tracé principal en courbe lissée
                        val strokePath = Path().apply {
                            for (i in points.indices) {
                                if (i == 0) {
                                    moveTo(points[i].x, points[i].y)
                                } else {
                                    val prev = points[i - 1]
                                    val curr = points[i]
                                    val conX1 = prev.x + (curr.x - prev.x) / 2
                                    val conY1 = prev.y
                                    val conX2 = prev.x + (curr.x - prev.x) / 2
                                    val conY2 = curr.y
                                    cubicTo(conX1, conY1, conX2, conY2, curr.x, curr.y)
                                }
                            }
                        }
                        drawPath(
                            path = strokePath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // 5. Dessiner les axes horizontaux & labels X (jours)
                    for (i in daysData.indices) {
                        val x = paddingLeft + i * stepX
                        // Point marqueur
                        drawCircle(
                            color = tertiaryColor,
                            radius = 4.dp.toPx(),
                            center = points[i]
                        )

                        val dayLabel = daysData[i].first
                        val measuredText = textMeasurer.measure(dayLabel)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = dayLabel,
                            topLeft = Offset(x - (measuredText.size.width / 2), height - 25.dp.toPx()),
                            style = TextStyle(color = primaryColor, fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Un autre graphique sous forme d'histogramme / diagramme en barres pour le mois
 */
@Composable
fun MonthlyBarChart(
    weeksData: List<Pair<String, Int>>, // Liste par ex: ("Semaine 1", 5 taches faites), ("Semaine 2", 8 faites)
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariantColor = MaterialTheme.colorScheme.secondaryContainer
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Volume d'Activités par Semaine",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (weeksData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune donnée de planification",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                ) {
                    val width = size.width
                    val height = size.height
                    val paddingLeft = 30.dp.toPx()
                    val paddingBottom = 40.dp.toPx()
                    val paddingTop = 10.dp.toPx()
                    val paddingRight = 10.dp.toPx()

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    val maxCount = (weeksData.maxOfOrNull { it.second } ?: 1).coerceAtLeast(5)

                    // Dessiner les lignes d'arrière-plan du diagramme
                    val lineCount = 3
                    for (l in 0..lineCount) {
                        val fraction = l.toFloat() / lineCount
                        val y = paddingTop + chartHeight * (1.0f - fraction)
                        drawLine(
                            color = onSurfaceVariantColor.copy(alpha = 0.15f),
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        val countVal = (fraction * maxCount).toInt()
                        drawText(
                            textMeasurer = textMeasurer,
                            text = countVal.toString(),
                            topLeft = Offset(5.dp.toPx(), y - 10.dp.toPx()),
                            style = TextStyle(color = secondaryColor, fontSize = 10.sp)
                        )
                    }

                    // Dessiner les barres rectangulaires
                    val barSpacing = 40.dp.toPx()
                    val barWidth = (chartWidth - (barSpacing * (weeksData.size - 1))) / weeksData.size

                    for (i in weeksData.indices) {
                        val value = weeksData[i].second
                        val fraction = value.toFloat() / maxCount
                        val barHeight = chartHeight * fraction

                        val x = paddingLeft + i * (barWidth + barSpacing)
                        val y = paddingTop + chartHeight - barHeight

                        // Dessiner la barre
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(tertiaryColor, primaryColor)
                            ),
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Mettre la valeur numérique au dessus de la barre
                        drawText(
                            textMeasurer = textMeasurer,
                            text = value.toString(),
                            topLeft = Offset(x + (barWidth / 2) - 8.dp.toPx(), y - 16.dp.toPx()),
                            style = TextStyle(color = primaryColor, fontSize = 10.sp)
                        )

                        // Label de l'axe X
                        val label = weeksData[i].first
                        val measuredText = textMeasurer.measure(label)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = label,
                            topLeft = Offset(x + (barWidth / 2) - (measuredText.size.width / 2), height - 25.dp.toPx()),
                            style = TextStyle(color = primaryColor, fontSize = 10.sp)
                        )
                    }
                }
            }
        }
    }
}
