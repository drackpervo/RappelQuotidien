package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FitnessStreakTracker(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val allSportProgress by viewModel.allSportProgress.collectAsState()
    val streak by viewModel.sportStreak.collectAsState()

    // Générer les 7 derniers jours glissants
    val rollingDays = remember(allSportProgress) {
        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDayLabel = SimpleDateFormat("E", java.util.Locale.FRENCH) // "lun.", "mar."...
        val sdfDayNum = SimpleDateFormat("d", Locale.getDefault())

        val list = mutableListOf<DayStreakInfo>()
        val cal = Calendar.getInstance()
        
        // Commencer d'il y a 6 jours jusqu'à aujourd'hui
        cal.add(Calendar.DAY_OF_YEAR, -6)
        
        for (i in 0..6) {
            val dateKey = sdfKey.format(cal.time)
            val dayLabel = sdfDayLabel.format(cal.time).replace(".", "").uppercase()
            val dayNumber = sdfDayNum.format(cal.time)
            
            val isToday = dateKey == viewModel.todayDateKey
            val isCompleted = allSportProgress.any { it.dateKey == dateKey && it.isCompleted }
            
            list.add(
                DayStreakInfo(
                    dateKey = dateKey,
                    dayLabel = dayLabel,
                    dayNumber = dayNumber,
                    isCompleted = isCompleted,
                    isToday = isToday
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Calculer les statistiques simples sur les 7 derniers jours
    val completedCount = rollingDays.count { it.isCompleted }
    val adherencePercent = if (rollingDays.isEmpty()) 0 else (completedCount * 100) / rollingDays.size

    // Formulaire d'ajout d'activité personnalisée pour les jours précédents
    var showAddManualActivity by remember { mutableStateOf(false) }
    var selectedExerciseName by remember { mutableStateOf("Jogging 🏃") }
    // Jour sélectionné dans les 7 jours pour ajout rapide si cliqué
    var selectedDayKeyForToggle by remember { mutableStateOf<String?>(null) }

    val exercisesPreset = listOf(
        "Jogging 🏃", "Musculation 🏋️", "Yoga & Étirements 🧘", 
        "Corde à sauter 🪢", "Vélo / Cardio 🚴", "Marche rapide 🚶"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fitness_streak_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // En-tête de la streak avec dégradé design
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Suivi de Régularité",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Appuyez sur un jour pour l'activer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Badge de streak flamboyant
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF5722),
                                    Color(0xFFFF9800)
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("streak_badge")
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "$streak d'affilée",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7 jours de Fitness Streak Tracker (Ligne Interactive de progression)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("streak_days_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rollingDays.forEach { day ->
                    val borderModifier = if (day.isToday) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        // Rond cliquable représentant l'état du jour
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .then(borderModifier)
                                .clip(CircleShape)
                                .background(
                                    if (day.isCompleted) {
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFF9800).copy(alpha = 0.15f),
                                                Color(0xFFFF5722)
                                            )
                                        )
                                    } else {
                                        Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                )
                                .clickable {
                                    // Toggle simple en arrière-plan
                                    viewModel.toggleSportForDate(day.dateKey, "Activité libre 🏃")
                                }
                                .testTag("streak_day_btn_${day.dateKey}")
                        ) {
                            if (day.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Complété",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = day.dayNumber,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Point indicateur si aujourd'hui
                        if (day.isToday) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(9.dp)) // Aligner l'espace
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Joli résumé des statistiques actuelles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = "Assiduité 7 derniers jours",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$completedCount / 7 jours complétés",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Cercle d'adherence
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { adherencePercent / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 4.dp,
                        trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    )
                    Text(
                        text = "$adherencePercent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action d'ajout d'activité personnalisée pour un jour ou n'importe quel jour libre
            OutlinedButton(
                onClick = { showAddManualActivity = !showAddManualActivity },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_toggle_manual_log"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = if (showAddManualActivity) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showAddManualActivity) "Fermer l'enregistreur" else "Enregistrer une autre activité",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            AnimatedVisibility(
                visible = showAddManualActivity,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Sélectionner le sport :",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Liste horizontale des presets d'exercices
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        items(exercisesPreset.size) { index ->
                            val ex = exercisesPreset[index]
                            FilterChip(
                                selected = selectedExerciseName == ex,
                                onClick = { selectedExerciseName = ex },
                                label = { Text(ex) },
                                modifier = Modifier.testTag("preset_sport_chip_$index")
                            )
                        }
                    }

                    Text(
                        text = "Pour quel jour ?",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Choix du jour parmi les 7 derniers jours
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rollingDays.forEach { dInfo ->
                            val isSelected = selectedDayKeyForToggle == dInfo.dateKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedDayKeyForToggle = if (isSelected) null else dInfo.dateKey
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("select_day_chip_${dInfo.dateKey}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dInfo.dayLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = dInfo.dayNumber,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val keyToUse = selectedDayKeyForToggle ?: viewModel.todayDateKey
                            viewModel.toggleSportForDate(keyToUse, selectedExerciseName)
                            // Réinitialiser les états
                            showAddManualActivity = false
                            selectedDayKeyForToggle = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_submit_manual_sport"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DoneOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedDayKeyForToggle != null) "Valider pour ce jour" else "Valider pour Aujourd'hui",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

data class DayStreakInfo(
    val dateKey: String,
    val dayLabel: String,
    val dayNumber: String,
    val isCompleted: Boolean,
    val isToday: Boolean
)
