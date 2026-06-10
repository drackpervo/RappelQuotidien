package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SleepManualInputForm(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // États pour le formulaire
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var bedtimeHour by remember { mutableStateOf(23) }
    var bedtimeMinute by remember { mutableStateOf(0) }
    var wakeupHour by remember { mutableStateOf(7) }
    var wakeupMinute by remember { mutableStateOf(0) }
    
    // Calcul automatique initial des millisecondes
    val wakeTimeCal = remember(selectedDate, wakeupHour, wakeupMinute) {
        (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, wakeupHour)
            set(Calendar.MINUTE, wakeupMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val bedtimeCal = remember(selectedDate, bedtimeHour, bedtimeMinute, wakeupHour, wakeupMinute) {
        (selectedDate.clone() as Calendar).apply {
            if (bedtimeHour > wakeupHour || (bedtimeHour == wakeupHour && bedtimeMinute > wakeupMinute)) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            set(Calendar.HOUR_OF_DAY, bedtimeHour)
            set(Calendar.MINUTE, bedtimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // Durée calculée automatiquement (minutes)
    val calculatedMinutes = remember(wakeTimeCal, bedtimeCal) {
        val diff = wakeTimeCal.timeInMillis - bedtimeCal.timeInMillis
        (diff / (1000 * 60)).toInt().coerceAtLeast(0)
    }

    // Permettre l'ajustement de la durée via un slider (initialisé par calculatedMinutes mais modifiable)
    var useManualDurationSlider by remember { mutableStateOf(false) }
    var manualDurationHours by remember { mutableStateOf(8f) }
    var manualDurationMinutes by remember { mutableStateOf(0f) }

    // Efficacité du sommeil (de 30% à 100%)
    var efficiency by remember { mutableStateOf(85f) }

    val finalDurationMinutes = if (useManualDurationSlider) {
        (manualDurationHours.toInt() * 60 + manualDurationMinutes.toInt()).coerceAtLeast(0)
    } else {
        calculatedMinutes
    }

    // Gestionnaires de dialogue date / heure
    val dateSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val frenchDateSdf = remember { SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH) }
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            selectedDate = cal
        },
        selectedDate.get(Calendar.YEAR),
        selectedDate.get(Calendar.MONTH),
        selectedDate.get(Calendar.DAY_OF_MONTH)
    )

    val bedtimePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            bedtimeHour = hour
            bedtimeMinute = minute
        },
        bedtimeHour, bedtimeMinute, true
    )

    val wakeupPickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            wakeupHour = hour
            wakeupMinute = minute
        },
        wakeupHour, wakeupMinute, true
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sleep_manual_input_form"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Titre de la section formulaire
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ajouter une nuit manuellement 📝",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 1. Sélection de la date (Jour de réveil)
            Text(
                text = "Jour du réveil :",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bouton custom DatePicker
                Button(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("form_date_picker_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = frenchDateSdf.format(selectedDate.time).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Raccourcis rapides
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        selectedDate = cal
                    },
                    modifier = Modifier.weight(0.7f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                ) {
                    Text("Hier ◀", style = MaterialTheme.typography.bodySmall)
                }

                OutlinedButton(
                    onClick = {
                        selectedDate = Calendar.getInstance()
                    },
                    modifier = Modifier.weight(0.7f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                ) {
                    Text("Aujourd'hui", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Sélection Coucher / Réveil
            Text(
                text = "Horaires approximatifs :",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Heure du coucher button
                Card(
                    onClick = { bedtimePickerDialog.show() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_bedtime_btn"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🛌 Coucher", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%02dh %02d".format(bedtimeHour, bedtimeMinute),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Heure du réveil button
                Card(
                    onClick = { wakeupPickerDialog.show() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_wake_time_btn"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⏰ Réveil", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "%02dh %02d".format(wakeupHour, wakeupMinute),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode d'input de durée : Switch / Choix d'ajustement direct de la durée
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ajuster la durée manuellement",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = useManualDurationSlider,
                    onCheckedChange = {
                        useManualDurationSlider = it
                        if (it) {
                            // Initialiser la durée manuelle à la valeur calculée
                            manualDurationHours = (calculatedMinutes / 60).toFloat().coerceIn(1f, 16f)
                            manualDurationMinutes = (calculatedMinutes % 60).toFloat()
                        }
                    },
                    modifier = Modifier.testTag("duration_adjust_switch")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Affichage / curseur de durée de sommeil
            if (!useManualDurationSlider) {
                // Lecture automatique
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Durée calculée : %d h %02d min".format(calculatedMinutes / 60, calculatedMinutes % 60),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                // Curseur de durée de sommeil (Heures + Minutes)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Heures de sommeil :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${manualDurationHours.toInt()} h",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = manualDurationHours,
                        onValueChange = { manualDurationHours = it },
                        valueRange = 1f..16f,
                        steps = 15,
                        modifier = Modifier.testTag("hours_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Minutes de sommeil :",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${manualDurationMinutes.toInt()} min",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = manualDurationMinutes,
                        onValueChange = { manualDurationMinutes = it },
                        valueRange = 0f..59f,
                        steps = 59,
                        modifier = Modifier.testTag("minutes_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Efficacité / Qualité du sommeil
            val qualityLabel = when {
                efficiency < 60 -> "Mauvais / Sommeil agité 😴"
                efficiency < 75 -> "Passable / Réveils nocturnes 😐"
                efficiency < 88 -> "Bon sommeil / Très reposant 🙂"
                else -> "Sommeil excellent & réparateur 🌟"
            }

            val qualityColor = when {
                efficiency < 60 -> MaterialTheme.colorScheme.error
                efficiency < 75 -> Color(0xFFF59E0B)
                efficiency < 88 -> MaterialTheme.colorScheme.primary
                else -> Color(0xFF10B981)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Qualité estimée :",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${efficiency.toInt()}% - $qualityLabel",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = qualityColor
                )
            }
            Slider(
                value = efficiency,
                onValueChange = { efficiency = it },
                valueRange = 30f..100f,
                steps = 70,
                colors = SliderDefaults.colors(
                    activeTrackColor = qualityColor,
                    thumbColor = qualityColor
                ),
                modifier = Modifier.testTag("efficiency_slider")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bouton de validation / insertion de la nuit
            Button(
                onClick = {
                    val finalDateKey = dateSdf.format(selectedDate.time)
                    viewModel.insertManualSleepSummary(
                        dateKey = finalDateKey,
                        bedtimeMillis = bedtimeCal.timeInMillis,
                        wakeTimeMillis = wakeTimeCal.timeInMillis,
                        durationMinutes = finalDurationMinutes,
                        efficiency = efficiency.toInt()
                    )
                    Toast.makeText(context, "Nuit du $finalDateKey enregistrée ✅ !", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_sleep_summary_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Enregistrer cette nuit de sommeil 💾",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
