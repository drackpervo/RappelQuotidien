package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlanningTask
import com.example.data.SleepSummary
import com.example.data.SportProgress
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun InteractiveCalendar(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val sports by viewModel.allSportProgress.collectAsState()
    val sleepRecords by viewModel.sleepSummaries.collectAsState()
    val selectedDayKey by viewModel.selectedDayKey.collectAsState()

    // Année et Mois affichés localement dans le calendrier
    var currentCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            // S'initialiser au jour sélectionné ou aujourd'hui
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val parsedDate = sdf.parse(selectedDayKey)
                if (parsedDate != null) {
                    time = parsedDate
                }
            } catch (e: Exception) {
                // fallback
            }
        })
    }

    // Régénérer la grille de jours lorsque l'année ou le mois change
    val daysInMonth = remember(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH)) {
        getDaysForCalendar(
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH)
        )
    }

    val displayMonthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val formattedCurrentMonth = remember(currentCalendar.timeInMillis) {
        displayMonthFormat.format(currentCalendar.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    // Informations complémentaires pour chaque jour (Mappage pour accès direct O(1) lors du rendu du calendrier)
    val tasksByDate = remember(tasks) {
        tasks.filter { it.periodType == "DAY" }.groupBy { it.periodKey }
    }
    val sportByDate = remember(sports) {
        sports.associateBy { it.dateKey }
    }
    val sleepByDate = remember(sleepRecords) {
        sleepRecords.associateBy { it.dateKey }
    }

    // Jour cliqué actuellement sélecionné pour le résumé détaillé
    val selectedDateDetail = remember(selectedDayKey, tasksByDate, sportByDate, sleepByDate) {
        getDateSummary(selectedDayKey, tasksByDate, sportByDate, sleepByDate)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // En-tête : Changement de mois
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val nextCal = currentCalendar.clone() as Calendar
                        nextCal.add(Calendar.MONTH, -1)
                        currentCalendar = nextCal
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Mois précédent",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = formattedCurrentMonth,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        val nextCal = currentCalendar.clone() as Calendar
                        nextCal.add(Calendar.MONTH, 1)
                        currentCalendar = nextCal
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Mois suivant",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Jours de la semaine
            val weekDays = listOf("Lu", "Ma", "Me", "Je", "Ve", "Sa", "Di")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Grille des jours du mois
            val weeksCount = daysInMonth.size / 7
            for (w in 0 until weeksCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (d in 0..6) {
                        val index = w * 7 + d
                        val calendarDay = daysInMonth[index]

                        val isCurrentMonth = calendarDay.isCurrentMonth
                        val dateKey = calendarDay.dateKey
                        val isSelected = (dateKey == selectedDayKey)

                        // Analyse de l'état du jour
                        val dayTasks = tasksByDate[dateKey] ?: emptyList()
                        val hasTasks = dayTasks.isNotEmpty()
                        val hasAllTasksCompleted = hasTasks && dayTasks.all { it.isCompleted }
                        val hasPartialTasksCompleted = hasTasks && !hasAllTasksCompleted && dayTasks.any { it.isCompleted }

                        val hasSportCompleted = sportByDate[dateKey]?.isCompleted == true
                        val hasSleepReport = sleepByDate[dateKey] != null

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable {
                                    // Sélectionner cette date
                                    viewModel.selectedDayKey.value = dateKey
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = calendarDay.dayNumber.toString(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected || isCurrentMonth) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    }
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Points de statut
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Tâches (Vert complet, vert de base partiel)
                                    if (hasTasks) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) {
                                                        Color.White
                                                    } else if (hasAllTasksCompleted) {
                                                        Color(0xFF388E3C) // Vert foncé
                                                    } else {
                                                        Color(0xFF81C784) // Vert clair
                                                    }
                                                )
                                        )
                                    }

                                    // Spors (Orange/Rouge)
                                    if (hasSportCompleted) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) Color.White else Color(0xFFF57C00) // Orange
                                                )
                                        )
                                    }

                                    // Sommeil (Bleu/Indigo)
                                    if (hasSleepReport) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) Color.White else Color(0xFF3F51B5) // Indigo
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Détail intelligent du jour sélectionné
    DateSummaryPanel(
        summary = selectedDateDetail,
        onAddInstantTask = {
            viewModel.addTask(it)
        },
        onToggleSport = {
            viewModel.toggleTodaySport()
        }
    )
}

// Représentation d'un jour dans le calendrier
data class CalendarDay(
    val dayNumber: Int,
    val dateKey: String, // format "yyyy-MM-dd"
    val isCurrentMonth: Boolean
)

// Calcul robuste de la grille de jours pour un mois & année donnés.
private fun getDaysForCalendar(year: Int, month: Int): List<CalendarDay> {
    val days = mutableListOf<CalendarDay>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // On configure le calendrier au 1er jour du mois
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month)
    cal.set(Calendar.DAY_OF_MONTH, 1)

    // Déterminer le jour de la semaine pour le 1er jour (Dimanche = 1, Lundi = 2...)
    val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    // Convertir en format où Lundi = 0, Mardi = 1... Dimanche = 6
    val prevMonthOffset = when (startDayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    // Ajouter les jours du mois précédent pour combler la grille
    val prevMonthCal = cal.clone() as Calendar
    prevMonthCal.add(Calendar.MONTH, -1)
    val maxDaysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    for (i in (maxDaysInPrevMonth - prevMonthOffset + 1)..maxDaysInPrevMonth) {
        prevMonthCal.set(Calendar.DAY_OF_MONTH, i)
        days.add(
            CalendarDay(
                dayNumber = i,
                dateKey = sdf.format(prevMonthCal.time),
                isCurrentMonth = false
            )
        )
    }

    // Jours du mois courant
    val maxDaysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (i in 1..maxDaysInCurrentMonth) {
        cal.set(Calendar.DAY_OF_MONTH, i)
        days.add(
            CalendarDay(
                dayNumber = i,
                dateKey = sdf.format(cal.time),
                isCurrentMonth = true
            )
        )
    }

    // Jours du mois suivant pour clôturer une grille complète (souvent multiples de 7, 42 cases)
    val nextMonthCal = cal.clone() as Calendar
    nextMonthCal.add(Calendar.MONTH, 1)
    val remainingCells = (7 - (days.size % 7)) % 7
    val weeksNeeded = if (days.size <= 35) 5 else 6
    val totalCellsNeeded = weeksNeeded * 7
    val nextMonthDaysToAdd = totalCellsNeeded - days.size

    for (i in 1..nextMonthDaysToAdd) {
        nextMonthCal.set(Calendar.DAY_OF_MONTH, i)
        days.add(
            CalendarDay(
                dayNumber = i,
                dateKey = sdf.format(nextMonthCal.time),
                isCurrentMonth = false
            )
        )
    }

    return days
}

// Structure de données de synthèse d'une journée
data class ParsedDaySummary(
    val dateKey: String,
    val readableDate: String,
    val isToday: Boolean,
    
    // Tâches
    val tasksCount: Int,
    val completedTasksCount: Int,
    val progressTasksFraction: Float,
    val tasksList: List<PlanningTask>,

    // Sport
    val sportActivity: SportProgress?,
    val isSportCompleted: Boolean,

    // Sommeil
    val sleepSummary: SleepSummary?
)

// Récupérer et synthétiser les données d'un jour donné
private fun getDateSummary(
    dateKey: String,
    tasksByDate: Map<String, List<PlanningTask>>,
    sportByDate: Map<String, SportProgress>,
    sleepByDate: Map<String, SleepSummary>
): ParsedDaySummary {
    val sdfSource = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfHuman = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())

    var readableDate = dateKey
    var isToday = false
    try {
        val dateObj = sdfSource.parse(dateKey)
        if (dateObj != null) {
            readableDate = sdfHuman.format(dateObj)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            
            // Vérifier si c'est aujourd'hui
            val todayStr = sdfSource.format(Date())
            isToday = (dateKey == todayStr)
        }
    } catch (e: Exception) {
        // use fallback
    }

    val dayTasks = tasksByDate[dateKey] ?: emptyList()
    val completedTasks = dayTasks.filter { it.isCompleted }
    val progressFraction = if (dayTasks.isNotEmpty()) {
        completedTasks.size.toFloat() / dayTasks.size.toFloat()
    } else {
        0.0f
    }

    val sport = sportByDate[dateKey]
    val sleep = sleepByDate[dateKey]

    return ParsedDaySummary(
        dateKey = dateKey,
        readableDate = readableDate,
        isToday = isToday,
        tasksCount = dayTasks.size,
        completedTasksCount = completedTasks.size,
        progressTasksFraction = progressFraction,
        tasksList = dayTasks,
        sportActivity = sport,
        isSportCompleted = sport?.isCompleted == true,
        sleepSummary = sleep
    )
}

@Composable
fun DateSummaryPanel(
    summary: ParsedDaySummary,
    onAddInstantTask: (String) -> Unit,
    onToggleSport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var quickTaskText by remember { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // En-tête du jour sélectionné
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (summary.isToday) Icons.Default.Stars else Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (summary.isToday) "Aujourd'hui" else "Jour sélectionné",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = summary.readableDate,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -- BLOC 1: TÂCHES --
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = if (summary.progressTasksFraction == 1.0f && summary.tasksCount > 0) {
                        Color(0xFF388E3C)
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Objectifs : ${summary.completedTasksCount} / ${summary.tasksCount} complétés",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (summary.tasksCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { summary.progressTasksFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (summary.progressTasksFraction == 1.0f) Color(0xFF388E3C) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                // Petit listing condensé des tâches de ce jour ciblé
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        summary.tasksList.take(3).forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (task.isCompleted) Color(0xFF388E3C) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (summary.tasksList.size > 3) {
                            Text(
                                text = "+ ${summary.tasksList.size - 3} autre(s) tâche(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aucun objectif planifié pour ce jour.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                
                // Champ d'ajout rapide pour faciliter l'interaction directe !
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickTaskText,
                        onValueChange = { quickTaskText = it },
                        placeholder = { Text("Ajout rapide d'objectif...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (quickTaskText.isNotBlank()) {
                                onAddInstantTask(quickTaskText)
                                quickTaskText = ""
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Ajouter",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(16.dp))

            // -- BLOC 2: SPORT & SOMMEIL --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sous-carte Sport
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = if (summary.isSportCompleted) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sport",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (summary.sportActivity != null) {
                            Text(
                                text = summary.sportActivity.exerciseName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (summary.isSportCompleted) "Complété ! ✅" else "Non complété ❌",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (summary.isSportCompleted) Color(0xFF388E3C) else Color(0xFFD32F2F)
                            )
                        } else {
                            Text(
                                text = "Aucune session enregistrée.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Sous-carte Sommeil
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Brightness4,
                                contentDescription = null,
                                tint = if (summary.sleepSummary != null) Color(0xFF3F51B5) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sommeil",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (summary.sleepSummary != null) {
                            val hours = summary.sleepSummary.durationMinutes / 60
                            val mins = summary.sleepSummary.durationMinutes % 60
                            Text(
                                text = "${hours}h ${mins}min de sommeil",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Efficacité : ${summary.sleepSummary.efficiency}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Pas d'analyse disponible.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}
