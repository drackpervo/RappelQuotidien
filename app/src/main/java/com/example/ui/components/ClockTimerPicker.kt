package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ClockTimerPicker(
    initialSeconds: Int,
    onDurationChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // État de saisie sous forme de chaîne de caractères (max 6 chiffres)
    var digitBuffer by remember {
        val initialStr = if (initialSeconds > 0) {
            val h = initialSeconds / 3600
            val m = (initialSeconds % 3600) / 60
            val s = initialSeconds % 60
            "%02d%02d%02d".format(h, m, s).dropWhile { it == '0' }
        } else {
            ""
        }
        mutableStateOf(initialStr)
    }

    // Extraction des Heures, Minutes, Secondes théoriques depuis le buffer
    val paddedBuffer = digitBuffer.padStart(6, '0')
    val hours = paddedBuffer.substring(0, 2).toInt()
    val minutes = paddedBuffer.substring(2, 4).toInt()
    val seconds = paddedBuffer.substring(4, 6).toInt()

    val totalCalculatedSeconds = hours * 3600 + minutes * 60 + seconds

    // Mettre à jour le parent dès que les données changent
    LaunchedEffect(totalCalculatedSeconds) {
        onDurationChanged(totalCalculatedSeconds)
    }

    // Fonction d'ajout de chiffres
    fun appendDigit(digit: Char) {
        if (digitBuffer.length < 6) {
            // Ignorer le premier zéro pour éviter d'allonger inutilement
            if (digitBuffer.isEmpty() && digit == '0') return
            digitBuffer += digit
        }
    }

    // Fonction de suppression (backspace)
    fun backspace() {
        if (digitBuffer.isNotEmpty()) {
            digitBuffer = digitBuffer.dropLast(1)
        }
    }

    // Suppression complète
    fun clearAll() {
        digitBuffer = ""
    }

    // Ajout de presets rapides
    fun addPresetSeconds(secs: Int) {
        val currentTotal = hours * 3600 + minutes * 60 + seconds
        val newTotal = (currentTotal + secs).coerceIn(0, 359999) // Max 99h 59m 59s
        
        val newH = newTotal / 3600
        val newM = (newTotal % 3600) / 60
        val newS = newTotal % 60
        
        digitBuffer = "%02d%02d%02d".format(newH, newM, newS).dropWhile { it == '0' }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("clock_timer_picker"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Écran d'affichage principal style App Horloge (Heures Min S)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unité Heures
            DigitGroup(
                value = "%02d".format(hours),
                unit = "h",
                isActive = hours > 0,
                modifier = Modifier.testTag("display_hours")
            )
            
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (hours > 0 || minutes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Unité Minutes
            DigitGroup(
                value = "%02d".format(minutes),
                unit = "m",
                isActive = hours > 0 || minutes > 0,
                modifier = Modifier.testTag("display_minutes")
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (hours > 0 || minutes > 0 || seconds > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Unité Secondes
            DigitGroup(
                value = "%02d".format(seconds),
                unit = "s",
                isActive = hours > 0 || minutes > 0 || seconds > 0,
                modifier = Modifier.testTag("display_seconds")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Boutons de raccourcis rapides (+30s, +1m, +5m)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickPresetChip(label = "+30s", onClick = { addPresetSeconds(30) }, modifier = Modifier.weight(1f))
            QuickPresetChip(label = "+1 min", onClick = { addPresetSeconds(60) }, modifier = Modifier.weight(1f))
            QuickPresetChip(label = "+5 min", onClick = { addPresetSeconds(300) }, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Le Pavé numérique Clavier style Application Horloge
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Lignes de chiffres (1-3, 4-6, 7-9)
            val rows = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9')
            )

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (char in row) {
                        NumpadButton(
                            text = char.toString(),
                            onClick = { appendDigit(char) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Dernière ligne : Clear (X), 0, Backspace (<-)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bouton Effacer Tout (Clear)
                NumpadActionButton(
                    onClick = { clearAll() },
                    modifier = Modifier.weight(1f).testTag("timer_btn_clear")
                ) {
                    Text(
                        text = "Effacer",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                // Chiffre 0
                NumpadButton(
                    text = "0",
                    onClick = { appendDigit('0') },
                    modifier = Modifier.weight(1f)
                )

                // Bouton Retour arrière (Backspace)
                NumpadActionButton(
                    onClick = { backspace() },
                    modifier = Modifier.weight(1f).testTag("timer_btn_backspace")
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Effacer un chiffre",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitGroup(
    value: String,
    unit: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
    }
}

@Composable
private fun QuickPresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun NumpadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1.5f)
            .height(54.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp
                )
            )
        }
    }
}

@Composable
private fun NumpadActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1.5f)
            .height(54.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
