package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.InteractiveCalendar
import com.example.ui.components.MonthlyBarChart
import com.example.ui.components.WeeklyLineChart
import com.example.ui.components.SleepAverageView
import com.example.ui.components.SleepRollingChart
import com.example.ui.components.RechartsDashboardChart
import com.example.ui.components.SleepManualInputForm
import com.example.ui.components.ClockTimerPicker
import com.example.ui.components.FitnessStreakTracker
import com.example.viewmodel.AppViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(viewModel: AppViewModel) {
    val tasks by viewModel.currentTasks.collectAsState()
    val periodType by viewModel.selectedPeriodType.collectAsState()
    val selectedDayKey by viewModel.selectedDayKey.collectAsState("...")
    val selectedWeekKey by viewModel.selectedWeekKey.collectAsState("...")
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsState("...")

    var taskInputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Planification Personnelle",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Planifiez vos actions et restez productif en tout temps hors-ligne.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sélecteur de période (Jour, Semaine, Mois)
        TabRow(
            selectedTabIndex = when (periodType) {
                "DAY" -> 0
                "WEEK" -> 1
                "MONTH" -> 2
                else -> 0
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Tab(
                selected = periodType == "DAY",
                onClick = { viewModel.selectedPeriodType.value = "DAY" },
                text = { Text("Jour") },
                icon = { Icon(Icons.Default.Today, contentDescription = "Jour") }
            )
            Tab(
                selected = periodType == "WEEK",
                onClick = { viewModel.selectedPeriodType.value = "WEEK" },
                text = { Text("Semaine") },
                icon = { Icon(Icons.Default.DateRange, contentDescription = "Semaine") }
            )
            Tab(
                selected = periodType == "MONTH",
                onClick = { viewModel.selectedPeriodType.value = "MONTH" },
                text = { Text("Mois") },
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Mois") }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (periodType == "DAY") {
            // Vue JOUR : Calendrier interactif avec résumés détaillés intégrés de manière transparente
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Widget de Calendrier Mensuel Interactif & de Synthèse de Journée
                InteractiveCalendar(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                // Objectifs de la journée
                Text(
                    text = "Objectifs individuels de la journée",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Formulaire d'ajout rapide pour ce jour précis avec choix de priorité
                var selectedPriority by remember { mutableStateOf("MEDIUM") }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = taskInputText,
                            onValueChange = { taskInputText = it },
                            placeholder = { Text("Ajouter un objectif de tâche...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if (taskInputText.isNotBlank()) {
                                    viewModel.addTask(taskInputText, selectedPriority)
                                    taskInputText = ""
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Priorité :",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = selectedPriority == "HIGH",
                            onClick = { selectedPriority = "HIGH" },
                            label = { Text("🔴 Haute") },
                            modifier = Modifier.testTag("priority_chip_high")
                        )
                        FilterChip(
                            selected = selectedPriority == "MEDIUM",
                            onClick = { selectedPriority = "MEDIUM" },
                            label = { Text("🟡 Moyenne") },
                            modifier = Modifier.testTag("priority_chip_medium")
                        )
                        FilterChip(
                            selected = selectedPriority == "LOW",
                            onClick = { selectedPriority = "LOW" },
                            label = { Text("🟢 Basse") },
                            modifier = Modifier.testTag("priority_chip_low")
                        )
                    }
                }

                // Section Filtrage par priorité
                val currentPriorityFilter by viewModel.selectedPriorityFilter.collectAsState()

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Filtrer par priorité :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "📑 Toutes",
                            "HIGH" to "🔴 Haute",
                            "MEDIUM" to "🟡 Moyenne",
                            "LOW" to "🟢 Basse"
                        ).forEach { (key, label) ->
                            val isSelected = currentPriorityFilter == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectedPriorityFilter.value = key },
                                label = { Text(label) },
                                modifier = Modifier.testTag("filter_chip_$key")
                            )
                        }
                    }
                }

                // Liste d'objectifs journaliers
                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucune tâche ne correspond à vos filtres.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tasks.forEach { task ->
                            TaskItem(task = task, viewModel = viewModel)
                        }
                    }
                }
            }
        } else {
            // Vue SEMAINE & MOIS originale optimisée
            val readablePeriodLabel = when (periodType) {
                "WEEK" -> "Semaine en cours : $selectedWeekKey"
                "MONTH" -> "Mois sélectionné : $selectedMonthKey"
                else -> ""
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = readablePeriodLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

                      // Formulaire d'ajout de tâche
            var selectedPrioritySecondary by remember { mutableStateOf("MEDIUM") }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = taskInputText,
                        onValueChange = { taskInputText = it },
                        placeholder = { Text("Nouvel objectif...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (taskInputText.isNotBlank()) {
                                viewModel.addTask(taskInputText, selectedPrioritySecondary)
                                taskInputText = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Priorité :",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = selectedPrioritySecondary == "HIGH",
                        onClick = { selectedPrioritySecondary = "HIGH" },
                        label = { Text("🔴 Haute") },
                        modifier = Modifier.testTag("priority_chip_high_sec")
                    )
                    FilterChip(
                        selected = selectedPrioritySecondary == "MEDIUM",
                        onClick = { selectedPrioritySecondary = "MEDIUM" },
                        label = { Text("🟡 Moyenne") },
                        modifier = Modifier.testTag("priority_chip_medium_sec")
                    )
                    FilterChip(
                        selected = selectedPrioritySecondary == "LOW",
                        onClick = { selectedPrioritySecondary = "LOW" },
                        label = { Text("🟢 Basse") },
                        modifier = Modifier.testTag("priority_chip_low_sec")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section Filtrage par priorité
            val currentPriorityFilterSecondary by viewModel.selectedPriorityFilter.collectAsState()

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Filtrer par priorité :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "📑 Toutes",
                        "HIGH" to "🔴 Haute",
                        "MEDIUM" to "🟡 Moyenne",
                        "LOW" to "🟢 Basse"
                    ).forEach { (key, label) ->
                        val isSelected = currentPriorityFilterSecondary == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedPriorityFilter.value = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("filter_chip_sec_$key")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Liste de tâches/objectifs
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aucune tâche ne correspond à vos filtres.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ajoutez une tâche ci-dessus pour commencer !",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(task = task, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: com.example.data.PlanningTask,
    viewModel: AppViewModel
) {
    val priorityInfo = when (task.priority.uppercase()) {
        "HIGH" -> Triple("🔴 Haute", Color(0xFFD32F2F), Color(0xFFFFEBEE))
        "LOW" -> Triple("🟢 Basse", Color(0xFF388E3C), Color(0xFFE8F5E9))
        else -> Triple("🟡 Moyenne", Color(0xFFF57C00), Color(0xFFFFF3E0))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (task.isCompleted) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Un petit indicateur vertical de priorité sur la gauche
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (task.isCompleted) Color.Gray.copy(alpha = 0.5f) else priorityInfo.second)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                // Priority Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (task.isCompleted) Color.LightGray.copy(alpha = 0.3f) else priorityInfo.third)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = priorityInfo.first,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (task.isCompleted) Color.Gray else priorityInfo.second
                    )
                }
            }

            IconButton(
                onClick = { viewModel.deleteTask(task) },
                modifier = Modifier.testTag("delete_task_btn_${task.id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Supprimer",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Écran Bien-être rassemblant le Sport Journalier, le Minuteur et le Sommeil.
 */
@Composable
fun BienEtreScreen(viewModel: AppViewModel) {
    var activeSubTab by remember { mutableStateOf(0) } // 0: Sport, 1: Minuteur, 2: Sommeil

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Espace Bien-être",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sélecteur de sous-catégories
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            SegmentedButton(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                icon = { Icon(Icons.Default.DirectionsRun, contentDescription = null) }
            ) {
                Text("Sport")
            }
            SegmentedButton(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                icon = { Icon(Icons.Default.Timer, contentDescription = null) }
            ) {
                Text("Minuteur")
            }
            SegmentedButton(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                icon = { Icon(Icons.Default.Bedtime, contentDescription = null) }
            ) {
                Text("Sommeil")
            }
        }

        // Affichage dynamique du sous-onglet sélectionné
        AnimatedContent(
            targetState = activeSubTab,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            },
            label = "BienEtreNav"
        ) { targetTab ->
            when (targetTab) {
                0 -> SportSection(viewModel)
                1 -> MinuteurSection(viewModel)
                2 -> SommeilSection(viewModel)
            }
        }
    }
}

/**
 * Section 1: Sport journalier.
 */
@Composable
fun SportSection(viewModel: AppViewModel) {
    val todaySport by viewModel.todaySportProgress.collectAsState()
    val streak by viewModel.sportStreak.collectAsState()

    val isDone = todaySport?.isCompleted ?: false

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Activité Sportive du Jour 🏋️",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Box colorée avec l'exercice proposé
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewModel.dailyProposedSportName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bouton de validation de l'activité
            Button(
                onClick = { viewModel.toggleTodaySport() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isDone) Icons.Default.Check else Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDone) "Activité validée ! Bravo 🎉" else "Valider l'activité du jour",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Composant de suivi de régularité interactif avec calendrier glissant
            FitnessStreakTracker(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Section 2: Minuteur d'activités.
 */
@Composable
fun MinuteurSection(viewModel: AppViewModel) {
    val isRunning by viewModel.timerIsRunning.collectAsState()
    val remainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val durationSecondsState by viewModel.timerDurationSeconds.collectAsState()
    val activityName by viewModel.timerActivityName.collectAsState()

    var customActivityInput by remember { mutableStateOf("Méditation") }
    var selectedDurationSeconds by remember { mutableStateOf(30) } // Par défaut 30 secondes !

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Minuteur d'Activités ⏱️",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (!isRunning) {
                // Formulaire de configuration
                OutlinedTextField(
                    value = customActivityInput,
                    onValueChange = { customActivityInput = it },
                    label = { Text("Nom de l'activité") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Sélecteur de durée style Application Horloge
                ClockTimerPicker(
                    initialSeconds = selectedDurationSeconds,
                    onDurationChanged = { selectedDurationSeconds = it },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Bouton de lancement
                Button(
                    onClick = { viewModel.startTimer(customActivityInput, selectedDurationSeconds) },
                    enabled = selectedDurationSeconds > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedDurationSeconds > 0) "Lancer le minuteur" else "Définir une durée",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                // Minuteur en cours de décompte (Affichage circulaire)
                Spacer(modifier = Modifier.height(16.dp))
                
                val displayMinutes = remainingSeconds / 60
                val displaySeconds = remainingSeconds % 60
                val percentProgress = remainingSeconds.toFloat() / durationSecondsState.toFloat()

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                ) {
                    CircularProgressIndicator(
                        progress = { percentProgress },
                        modifier = Modifier.size(150.dp),
                        strokeWidth = 6.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%02d:%02d".format(displayMinutes, displaySeconds),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 36.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activityName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Fonctionne en arrière-plan. Une notification vous alertera une fois le temps écoulé.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton d'arrêt
                Button(
                    onClick = { viewModel.stopTimer() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Annuler le minuteur", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

/**
 * Section 3: Suivi du sommeil (offline-first).
 */
@Composable
fun SommeilSection(viewModel: AppViewModel) {
    val sleepHistories by viewModel.sleepSummaries.collectAsState()
    val bHour by viewModel.bedtimeHourState.collectAsState()
    val bMin by viewModel.bedtimeMinuteState.collectAsState()
    val wHour by viewModel.wakeupHourState.collectAsState()
    val wMin by viewModel.wakeupMinuteState.collectAsState()

    val context = LocalContext.current

    // Dialogues de sélection d'heure
    val bedtimePicker = TimePickerDialog(
        context,
        { _, h, m -> viewModel.saveSleepSchedule(h, m, wHour, wMin) },
        bHour, bMin, true
    )

    val wakeupPicker = TimePickerDialog(
        context,
        { _, h, m -> viewModel.saveSleepSchedule(bHour, bMin, h, m) },
        wHour, wMin, true
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Cadre de Sommeil Typique 🌙",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Heure de coucher
                    Card(
                        onClick = { bedtimePicker.show() },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Heure Coucher", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "%02dh %02d".format(bHour, bMin),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Heure de réveil
                    Card(
                        onClick = { wakeupPicker.show() },
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Heure Réveil", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "%02dh %02d".format(wHour, wMin),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "L'application analyse de manière transparente les heures où le téléphone est inactif pendant cette tranche pour calculer l'efficacité et la durée de sommeil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action manuelle d'évaluation
                Button(
                    onClick = {
                        viewModel.runSleepAnalysisManually()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyser maintenant", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Formulaire de saisie manuelle de nuit de sommeil
        SleepManualInputForm(viewModel = viewModel)

        // Nouvelle vue d'analyse moyenne des 30 derniers jours
        SleepAverageView(sleepRecords = sleepHistories)

        // Graphique de tendance du sommeil (Moyenne mobile glissante sur 7 jours)
        SleepRollingChart(sleepRecords = sleepHistories)

        // Rapports de sommeil récents
        Text(
            text = "Nuits Récentes",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (sleepHistories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune nuit calculée pour l'instant.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sleepHistories.forEach { sleep ->
                    val formattedDate = remember(sleep) {
                        try {
                            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val sdfOut = SimpleDateFormat("EEEE d MMMM", Locale.FRENCH)
                            val dateObj = sdfIn.parse(sleep.dateKey)
                            dateObj?.let { sdfOut.format(it).replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.FRENCH) else c.toString() } } ?: "Nuit du ${sleep.dateKey}"
                        } catch (e: Exception) {
                            "Nuit du ${sleep.dateKey}"
                        }
                    }
                    val sdfTime = remember(sleep) { SimpleDateFormat("HH'h'mm", Locale.getDefault()) }
                    val bedtimeStr = remember(sleep) { sdfTime.format(Date(sleep.bedtimeMillis)) }
                    val wakeTimeStr = remember(sleep) { sdfTime.format(Date(sleep.wakeTimeMillis)) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sleep_history_card_${sleep.dateKey}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bedtime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (sleep.efficiency >= 85) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${sleep.efficiency}% Eff.",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (sleep.efficiency >= 85) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSleepSummary(sleep) },
                                        modifier = Modifier.size(32.dp).testTag("delete_sleep_summary_${sleep.dateKey}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer cette nuit",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Durée totale
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Durée : %d h %02d min".format(sleep.durationMinutes / 60, sleep.durationMinutes % 60),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Coucher / Lever
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$bedtimeStr à $wakeTimeStr",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

/**
 * Écran Tableau de bord avec les graphiques de progression.
 */
@Composable
fun StatsScreen(viewModel: AppViewModel) {
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList<PlanningTask>())
    val sportRecords by viewModel.allSportProgress.collectAsState()
    val sleepRecords by viewModel.sleepSummaries.collectAsState()

    // 1. Calcul de progression des 7 derniers jours (Ex: Lundi à Dimanche)
    // On construit une liste dynamique des 7 derniers jours avec taux d'accomplissement (tâches faites / tâches totales)
    val weeklyData = remember(tasks, sportRecords) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLabelSdf = SimpleDateFormat("EE", Locale.getDefault())
        val list = mutableListOf<Pair<String, Float>>()

        val cal = Calendar.getInstance()
        for (i in 0..6) {
            val key = sdf.format(cal.time)
            val dayName = dayLabelSdf.format(cal.time)

            // Tâches
            val dayTasks = tasks.filter { it.periodType == "DAY" && it.periodKey == key }
            val completedTasks = dayTasks.count { it.isCompleted }
            
            // Sport
            val sportCompleted = sportRecords.any { it.dateKey == key && it.isCompleted }

            val totalActivitiesCount = dayTasks.size + 1 // Tâches + Sport
            val completedActivitiesCount = completedTasks + (if (sportCompleted) 1 else 0)

            val rate = if (totalActivitiesCount > 0) {
                completedActivitiesCount.toFloat() / totalActivitiesCount.toFloat()
            } else {
                0.0f
            }

            list.add(0, Pair(dayName, rate))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        list
    }

    // 2. Décomptes de tâches par semaines du mois pour le diagramme en barres
    val monthlyData = remember(tasks) {
        val list = mutableListOf<Pair<String, Int>>()
        val groups = tasks.filter { it.periodType == "WEEK" }.groupBy { it.periodKey }
        
        // On récupère les 4 dernières entrées ou semaines
        val sortedKeys = groups.keys.sorted().takeLast(4)
        for (key in sortedKeys) {
            val weekTasks = groups[key] ?: emptyList()
            val completed = weekTasks.count { it.isCompleted }
            
            // Formatter pour raccourcir "2026-W23" en "S. 23"
            val shortKey = key.substringAfter("-W").let { "Sem $it" }
            list.add(Pair(shortKey, completed))
        }

        if (list.isEmpty()) {
            list.add(Pair("Sem 1", 0))
            list.add(Pair("Sem 2", 0))
            list.add(Pair("Sem 3", 0))
            list.add(Pair("Sem 4", 0))
        }
        list
    }

    // 3. Indicateurs de scores cumulés
    val completedTasksCount = tasks.count { it.isCompleted }
    val totalTasksCount = tasks.size
    val totalSportsCompleted = sportRecords.count { it.isCompleted }

    // Calcul de la durée moyenne de sommeil pour le badge indicateur
    val avgSleepStr = remember(sleepRecords) {
        if (sleepRecords.isEmpty()) {
            "--"
        } else {
            val last30 = sleepRecords.take(30)
            val totalMinutes = last30.sumOf { it.durationMinutes }
            val avgMinutes = totalMinutes / last30.size
            "%dh%02d".format(avgMinutes / 60, avgMinutes % 60)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Tableau de Bord 📈",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Visualisation offline de l'accomplissement de vos objectifs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Cartes d'indicateurs rapides de réussite (Tâches, Sport, Sommeil)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tâches Faites", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$completedTasksCount / $totalTasksCount",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sport", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer, maxLines = 1)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalSportsCompleted s.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sommeil (Moy)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = avgSleepStr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Conteneur de défilement pour les graphiques
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    RechartsDashboardChart(
                        sleepRecords = sleepRecords,
                        sportProgressList = sportRecords
                    )
                }
                item {
                    WeeklyLineChart(daysData = weeklyData)
                }
                item {
                    MonthlyBarChart(weeksData = monthlyData)
                }
                item {
                    SleepAverageView(sleepRecords = sleepRecords)
                }
                item {
                    SleepRollingChart(sleepRecords = sleepRecords)
                }
            }
        }
    }
}

/**
 * Écran de gestion des Rappels (Notifications avec NotificationManager, alarmes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(viewModel: AppViewModel) {
    val reminders by viewModel.allReminders.collectAsState()

    var inputMessage by remember { mutableStateOf("Prendre soin de moi 🌟") }
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }

    val context = LocalContext.current

    // Dialogue pour choisir l'heure du rappel
    val timePicker = TimePickerDialog(
        context,
        { _, h, m ->
            selectedHour = h
            selectedMinute = m
        },
        selectedHour, selectedMinute, true
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Rappels et Notifications 🔔",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Configurez des alarmes précises. Même fermée, l'application reste active pour vous alerter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val customSoundUri by viewModel.completionSoundUri.collectAsState()
        val customSoundName by viewModel.completionSoundName.collectAsState()

        val ringtonePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uri: android.net.Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI, android.net.Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
                if (uri != null) {
                    val ringtone = android.media.RingtoneManager.getRingtone(context, uri)
                    val name = ringtone?.getTitle(context) ?: "Son personnalisé"
                    viewModel.saveCustomSound(uri.toString(), name)
                } else {
                    viewModel.saveCustomSound(null, "Silencieux")
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("custom_sound_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Son de complétion des activités 🎵",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choisissez le son joué en complétant vos tâches et entraînements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(
                            text = "Son Actif",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = customSoundName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(2f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.playCustomSound(context)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Tester",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tester", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION)
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Modifier le son")
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                    putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    if (customSoundUri != null) {
                                        putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.net.Uri.parse(customSoundUri))
                                    }
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Définit le son",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Définit", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Formulaire d'ajout
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ajouter un nouveau rappel",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    label = { Text("Texte de notification") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Heure programmée : %02d:%02d".format(selectedHour, selectedMinute),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Button(
                        onClick = { timePicker.show() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Text("Choisir")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            viewModel.addReminder(selectedHour, selectedMinute, inputMessage)
                            inputMessage = "Prendre soin de moi 🌟"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Créer le Rappel Actif")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Sélection Active de vos Rappels",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Liste des rappels créés
        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Aucun rappel enregistré pour l'instant.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "%02d:%02d".format(reminder.hour, reminder.minute),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = reminder.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = reminder.isEnabled,
                                    onCheckedChange = { viewModel.toggleReminder(reminder) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Bouton de synchronisation globale utilitaire
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.synchronizeReminders() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vérifier et Synchroniser les alarmes")
            }
        }
    }
}
