package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SleepSummary
import com.example.data.SportProgress
import java.text.SimpleDateFormat
import java.util.*

/**
 * structure de données pour le tableau de bord de statistiques combiné (Style Recharts)
 */
data class DashboardDayData(
    val dateKey: String,
    val shortLabel: String,
    val sleepHours: Float,
    val fitnessStreak: Int,
    val sportCompleted: Boolean
)

/**
 * Un tableau de bord interactif inspiré de Recharts, qui trace :
 * 1. La série fitness quotidienne (en Orange vibrant de son cumul réel)
 * 2. Les motifs de sommeil quotidiens (en Violet/Bleu de la durée en heures)
 */
@Composable
fun RechartsDashboardChart(
    sleepRecords: List<SleepSummary>,
    sportProgressList: List<SportProgress>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    // Filtres d'affichage interactifs style légendes Recharts
    var showSleepLine by remember { mutableStateOf(true) }
    var showFitnessLine by remember { mutableStateOf(true) }
    
    // Index sélectionné par clic pour afficher les détails du tooltip interactif
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    // Couleurs de thème de notre charte graphique
    val sleepLineColor = MaterialTheme.colorScheme.primary // Violet/Indigo
    val fitnessLineColor = Color(0xFFF97316) // Orange vibrant (style fitness)
    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val selectionLineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)

    // Calcul dynamique des données combinées sur les 7 derniers jours
    val combinedWeeklyData = remember(sleepRecords, sportProgressList) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLabelSdf = SimpleDateFormat("EE dd", Locale.FRENCH)
        val result = mutableListOf<DashboardDayData>()

        val calendar = Calendar.getInstance()
        val completedDatesSet = sportProgressList.filter { it.isCompleted }.map { it.dateKey }.toSet()

        // Générer les 7 derniers jours de manière ordonnée (du passé vers aujourd'hui)
        val daysList = mutableListOf<Calendar>()
        for (i in 0..6) {
            daysList.add(calendar.clone() as Calendar)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        daysList.reverse()

        for (cal in daysList) {
            val key = sdf.format(cal.time)
            val label = dayLabelSdf.format(cal.time).replace(".", "").replaceFirstChar { it.uppercase() }

            // 1. Durée de sommeil en Heures (s'appuie sur SleepSummary de la base pour cette dateKey)
            val sleepRecord = sleepRecords.find { it.dateKey == key }
            val sleepHours = if (sleepRecord != null) {
                sleepRecord.durationMinutes.toFloat() / 60.0f
            } else {
                0.0f
            }

            // 2. Calcul du de la série d'activité cumulée (fitness streak) jusqu'à ce jour particulier
            val streakVal = calculateHistoricStreak(completedDatesSet, key)
            val completed = completedDatesSet.contains(key)

            result.add(
                DashboardDayData(
                    dateKey = key,
                    shortLabel = label,
                    sleepHours = sleepHours,
                    fitnessStreak = streakVal,
                    sportCompleted = completed
                )
            )
        }
        result
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_dashboard_card")
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.03f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            // En-tête avec titre inspiré de Recharts dashboard
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Dashboard Recharts Interactif 📊",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Analyse croisée Sommeil & Séries Fitness",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Légende interactive - Toggles d'affichage comme les légendes cliquables Recharts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filtre Sommeil
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showSleepLine = !showSleepLine }
                        .background(if (showSleepLine) sleepLineColor.copy(alpha = 0.08f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (showSleepLine) sleepLineColor else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Patrons de Sommeil (heures)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (showSleepLine) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (showSleepLine) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Filtre Fitness Streak
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFitnessLine = !showFitnessLine }
                        .background(if (showFitnessLine) fitnessLineColor.copy(alpha = 0.08f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (showFitnessLine) fitnessLineColor else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Série Fitness (jours)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (showFitnessLine) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (showFitnessLine) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Calculer les échelles maximales pour notre repère cartésien
            val maxSleepScale = 12f // 12 heures max d'échelle de sommeil
            val maxFitnessScale = remember(combinedWeeklyData) {
                (combinedWeeklyData.maxOfOrNull { it.fitnessStreak }?.toFloat() ?: 5f).coerceAtLeast(5f) + 1.5f
            }

            // Capturer les couleurs du thème en dehors du contexte DrawScope non-compilant Composable
            val themePrimaryColor = MaterialTheme.colorScheme.primary
            val themeOnSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

            // Dessin du graphique Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(combinedWeeklyData) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val paddingLeft = 45.dp.toPx()
                                val paddingRight = 15.dp.toPx()
                                val chartWidth = width - paddingLeft - paddingRight

                                val stepX = if (combinedWeeklyData.size > 1) chartWidth / (combinedWeeklyData.size - 1) else chartWidth

                                // Trouver quel axe de colonne X est le plus proche de l'endroit tapé
                                var closestIndex = -1
                                var minDiff = Float.MAX_VALUE
                                for (i in combinedWeeklyData.indices) {
                                    val colX = paddingLeft + i * stepX
                                    val diff = kotlin.math.abs(colX - offset.x)
                                    if (diff < minDiff) {
                                        minDiff = diff
                                        closestIndex = i
                                    }
                                }

                                // Si l'écran a été cliqué à une distance raisonnable de la colonne, l'activer/gérer le tooltip
                                if (closestIndex != -1 && minDiff < stepX * 0.75f) {
                                    selectedDayIndex = if (selectedDayIndex == closestIndex) null else closestIndex
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 45.dp.toPx()
                    val paddingBottom = 40.dp.toPx()
                    val paddingTop = 20.dp.toPx()
                    val paddingRight = 15.dp.toPx()

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    val stepX = if (combinedWeeklyData.size > 1) chartWidth / (combinedWeeklyData.size - 1) else chartWidth

                    // 1. Dessiner la grille de fond horizontale (4 étapes d'échelle)
                    val gridSteps = 4
                    for (step in 0..gridSteps) {
                        val fraction = step.toFloat() / gridSteps.toFloat()
                        val y = paddingTop + chartHeight * (1.0f - fraction)
                        
                        drawLine(
                            color = gridLineColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Graduations Axe Y Gauche (Sommeil - Heures)
                        val sleepValLabel = "${(fraction * maxSleepScale).toInt()}h"
                        drawText(
                            textMeasurer = textMeasurer,
                            text = sleepValLabel,
                            topLeft = Offset(8.dp.toPx(), y - 10.dp.toPx()),
                            style = TextStyle(color = sleepLineColor.copy(alpha = 0.8f), fontSize = 10.sp)
                        )

                        // Graduations Axe Y Droite (Fitness Streak - Jours)
                        val fitnessValLabel = "${(fraction * maxFitnessScale).toInt()}j"
                        val labelSize = textMeasurer.measure(fitnessValLabel)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = fitnessValLabel,
                            topLeft = Offset(width - paddingRight + 4.dp.toPx(), y - 10.dp.toPx()),
                            style = TextStyle(color = fitnessLineColor.copy(alpha = 0.8f), fontSize = 10.sp)
                        )
                    }

                    // Calculer les positions exactes de chaque point
                    val pointsSleep = mutableListOf<Offset>()
                    val pointsFitness = mutableListOf<Offset>()

                    for (i in combinedWeeklyData.indices) {
                        val x = paddingLeft + i * stepX
                        
                        // Heures de sommeil interpolées de 0 à maxSleepScale
                        val sleepFraction = combinedWeeklyData[i].sleepHours / maxSleepScale
                        val ySleep = paddingTop + chartHeight * (1.0f - sleepFraction.coerceIn(0f, 1f))
                        pointsSleep.add(Offset(x, ySleep))

                        // Fitness streak interpolées de 0 à maxFitnessScale
                        val fitnessFraction = combinedWeeklyData[i].fitnessStreak.toFloat() / maxFitnessScale
                        val yFitness = paddingTop + chartHeight * (1.0f - fitnessFraction.coerceIn(0f, 1f))
                        pointsFitness.add(Offset(x, yFitness))
                    }

                    // 2. Ligne d'indicateur verticale si une colonne/un jour est sélectionné (Tooltip cursor)
                    selectedDayIndex?.let { idx ->
                        val selectedX = paddingLeft + idx * stepX
                        drawLine(
                            color = selectionLineColor,
                            start = Offset(selectedX, paddingTop),
                            end = Offset(selectedX, paddingTop + chartHeight),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // 3. Dessiner la courbe du Sommeil (Style Recharts fluide avec ombrage)
                    if (showSleepLine && pointsSleep.size >= 2) {
                        // Courbe dégradée de remplissage sous la courbe de sommeil
                        val sleepFillPath = Path().apply {
                            moveTo(pointsSleep.first().x, paddingTop + chartHeight)
                            for (i in pointsSleep.indices) {
                                if (i == 0) {
                                    lineTo(pointsSleep[i].x, pointsSleep[i].y)
                                } else {
                                    val prev = pointsSleep[i - 1]
                                    val curr = pointsSleep[i]
                                    val conX = prev.x + (curr.x - prev.x) / 2
                                    cubicTo(conX, prev.y, conX, curr.y, curr.x, curr.y)
                                }
                            }
                            lineTo(pointsSleep.last().x, paddingTop + chartHeight)
                            close()
                        }
                        drawPath(
                            path = sleepFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(sleepLineColor.copy(alpha = 0.2f), Color.Transparent),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        // Tracé principal en ligne continue
                        val sleepStrokePath = Path().apply {
                            for (i in pointsSleep.indices) {
                                if (i == 0) {
                                    moveTo(pointsSleep[i].x, pointsSleep[i].y)
                                } else {
                                    val prev = pointsSleep[i - 1]
                                    val curr = pointsSleep[i]
                                    val conX = prev.x + (curr.x - prev.x) / 2
                                    cubicTo(conX, prev.y, conX, curr.y, curr.x, curr.y)
                                }
                            }
                        }
                        drawPath(
                            path = sleepStrokePath,
                            color = sleepLineColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // 4. Dessiner la courbe du Fitness Streak (Style Recharts de marqueur brillant)
                    if (showFitnessLine && pointsFitness.size >= 2) {
                        // Courbe dégradée de remplissage sous la courbe Fitness
                        val fitnessFillPath = Path().apply {
                            moveTo(pointsFitness.first().x, paddingTop + chartHeight)
                            for (i in pointsFitness.indices) {
                                if (i == 0) {
                                    lineTo(pointsFitness[i].x, pointsFitness[i].y)
                                } else {
                                    val prev = pointsFitness[i - 1]
                                    val curr = pointsFitness[i]
                                    val conX = prev.x + (curr.x - prev.x) / 2
                                    cubicTo(conX, prev.y, conX, curr.y, curr.x, curr.y)
                                }
                            }
                            lineTo(pointsFitness.last().x, paddingTop + chartHeight)
                            close()
                        }
                        drawPath(
                            path = fitnessFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(fitnessLineColor.copy(alpha = 0.15f), Color.Transparent),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        // Tracé principal de fitness
                        val fitnessStrokePath = Path().apply {
                            for (i in pointsFitness.indices) {
                                if (i == 0) {
                                    moveTo(pointsFitness[i].x, pointsFitness[i].y)
                                } else {
                                    val prev = pointsFitness[i - 1]
                                    val curr = pointsFitness[i]
                                    val conX = prev.x + (curr.x - prev.x) / 2
                                    cubicTo(conX, prev.y, conX, curr.y, curr.x, curr.y)
                                }
                            }
                        }
                        drawPath(
                            path = fitnessStrokePath,
                            color = fitnessLineColor,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // 5. Dessiner les points indicateurs sur chaque courbe et légendes temporelles bas
                    for (i in combinedWeeklyData.indices) {
                        val x = paddingLeft + i * stepX

                        // Points sur la courbe de sommeil
                        if (showSleepLine) {
                            val sleepPt = pointsSleep[i]
                            val isSelected = selectedDayIndex == i
                            drawCircle(
                                color = sleepLineColor,
                                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                center = sleepPt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                                center = sleepPt
                            )
                        }

                        // Points de fitness
                        if (showFitnessLine) {
                            val fitnessPt = pointsFitness[i]
                            val isSelected = selectedDayIndex == i
                            drawCircle(
                                color = fitnessLineColor,
                                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                center = fitnessPt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                                center = fitnessPt
                            )
                        }

                        // Étiquette de date pour chaque colonne X
                        val label = combinedWeeklyData[i].shortLabel
                        val measured = textMeasurer.measure(label)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = label,
                            topLeft = Offset(x - (measured.size.width / 2), height - 25.dp.toPx()),
                            style = TextStyle(
                                color = if (selectedDayIndex == i) themePrimaryColor else themeOnSurfaceVariant,
                                fontWeight = if (selectedDayIndex == i) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Explication de l'interactivité
            if (selectedDayIndex == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Touchez un jour sur le graphique pour afficher les valeurs exactes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 6. Infobulle / Tooltip interactif style Recharts
            AnimatedVisibility(visible = selectedDayIndex != null) {
                selectedDayIndex?.let { idx ->
                    val dayData = combinedWeeklyData[idx]
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4e-1f))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Statistiques de ${dayData.shortLabel}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Sommeil
                            Column {
                                Text(
                                    text = "Sommeil",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (dayData.sleepHours > 0.0f) {
                                        val h = dayData.sleepHours.toInt()
                                        val m = ((dayData.sleepHours - h) * 60).toInt()
                                        "${h}h ${m}m"
                                    } else "Pas de relevé",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            // Fitness Streak
                            Column {
                                Text(
                                    text = "Série Fitness",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${dayData.fitnessStreak} jours consécutifs 🔥",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = fitnessLineColor
                                )
                            }

                            // Sport aujourd'hui
                            Column {
                                Text(
                                    text = "Sport du jour",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (dayData.sportCompleted) "Complété ✅" else "En cours ⏳",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (dayData.sportCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calcule dynamiquement le nombre de jours consécutifs actifs jusqu'à une date cible historique donnée
 */
private fun calculateHistoricStreak(completedDates: Set<String>, targetDateKey: String): Int {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    var streak = 0

    try {
        val targetDate = sdf.parse(targetDateKey) ?: return 0
        cal.time = targetDate

        // Si la date elle-même n'est pas complétée et que la veille ne l'est pas non plus, la série est à 0
        val isTargetCompleted = completedDates.contains(sdf.format(cal.time))
        if (!isTargetCompleted) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val isYesterdayCompleted = completedDates.contains(sdf.format(cal.time))
            if (!isYesterdayCompleted) return 0
            // On rétablit la veille comme date de début de comptage
        } else {
            // Rétablir la date cible
            cal.time = targetDate
        }

        while (true) {
            val dateStr = sdf.format(cal.time)
            if (completedDates.contains(dateStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
    } catch (e: Exception) {
        // Ignorer silencieusement
    }
    return streak
}
