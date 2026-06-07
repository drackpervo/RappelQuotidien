package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.data.SleepSummary
import java.util.Calendar

@Composable
fun SleepAverageView(
    sleepRecords: List<SleepSummary>,
    modifier: Modifier = Modifier
) {
    // Filtrage des enregistrements des 30 derniers jours
    val last30DaysRecords = remember(sleepRecords) {
        val thirtyDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }.timeInMillis

        // Filtrer l'historique sur les dates correspondantes (chaque SleepSummary a une date clé).
        // En plus de la dateKey, on peut utiliser bedtimeMillis/wakeTimeMillis ou simplement prendre les 30 derniers enregistrements (le DAO limite déjà à 30).
        // On prend les 30 derniers enregistrements du DAO pour représenter au plus les 30 dernières nuits enregistrées.
        sleepRecords.take(30)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sleep_average_card"),
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
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // En-tête de la vue section de sommeil
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightsStay,
                            contentDescription = "Analyses de Sommeil",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Aperçu des 30 derniers jours",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Moyenne de Sommeil 🌙",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (last30DaysRecords.isEmpty()) {
                // État vide élégant
                EmptySleepState()
            } else {
                // Calculs des moyennes sur les 30 derniers jours
                val totalNights = last30DaysRecords.size
                val totalMinutes = last30DaysRecords.sumOf { it.durationMinutes }
                val avgMinutes = totalMinutes / totalNights
                val avgHours = avgMinutes / 60
                val avgRemMinutes = avgMinutes % 60

                val totalEfficiency = last30DaysRecords.sumOf { it.efficiency }
                val avgEfficiency = totalEfficiency / totalNights

                val shortestNight = last30DaysRecords.minByOrNull { it.durationMinutes }
                val longestNight = last30DaysRecords.maxByOrNull { it.durationMinutes }

                // Pourcentage par rapport à un objectif décent (8h = 480 minutes)
                val targetMinutes = 480f
                val sleepProgressPercent = (avgMinutes.toFloat() / targetMinutes).coerceIn(0f, 1.2f)

                // Animation du cercle de progression
                val animateProgress by animateFloatAsState(
                    targetValue = sleepProgressPercent,
                    animationSpec = tween(durationMillis = 800),
                    label = "sleep_progress"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicateur circulaire de progression (Gauge) à gauche
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animateProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 10.dp,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${avgHours}h ${avgRemMinutes}m",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "/ nuit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Informations textuelles et d'efficacité à droite
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Qualité générale du sommeil
                        val sleepVerdict = getSleepVerdict(avgMinutes, avgEfficiency)
                        
                        Text(
                            text = sleepVerdict.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = sleepVerdict.color
                        )
                        
                        Text(
                            text = sleepVerdict.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        // Efficacité moyenne sous forme de mini pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$avgEfficiency% d'efficacité",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))

                Spacer(modifier = Modifier.height(16.dp))

                // Grille condensée d'informations détaillées
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Carte Nuit la plus courte
                    if (shortestNight != null) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Nuit la plus courte",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QueryBuilder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${shortestNight.durationMinutes / 60}h ${shortestNight.durationMinutes % 60}m",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Carte Nuit la plus longue
                    if (longestNight != null) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Nuit la plus longue",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF388E3C),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${longestNight.durationMinutes / 60}h ${longestNight.durationMinutes % 60}m",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Carte Statistiques de volume
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Nuits enregistrées",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$totalNights / 30",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySleepState() {
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
            text = "Pas de données d'analyse de sommeil pour l'instant.",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "L'application compile vos heures calmes après que vos nuits ont été analysées.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp)
        )
    }
}

// Classe scellée ou de données pour modéliser le diagnostic de sommeil
data class SleepVerdict(
    val title: String,
    val description: String,
    val color: Color
)

@Composable
private fun getSleepVerdict(avgMinutes: Int, avgEfficiency: Int): SleepVerdict {
    val durationHours = avgMinutes / 60
    return when {
        durationHours >= 7 && avgEfficiency >= 85 -> {
            SleepVerdict(
                title = "Excellent Sommeil ! ✨",
                description = "Vos nuits sont réparatrices et ont une durée idéale pour être en super forme.",
                color = Color(0xFF388E3C)
            )
        }
        durationHours >= 6 && avgEfficiency >= 75 -> {
            SleepVerdict(
                title = "Bon Sommeil 💤",
                description = "La durée globale est correcte et votre rythme semble bien équilibré.",
                color = MaterialTheme.colorScheme.primary
            )
        }
        else -> {
            SleepVerdict(
                title = "Sommeil irrégulier ⚠️",
                description = "Essayez de vous coucher à heures fixes pour améliorer la régularité et rester en alerte.",
                color = Color(0xFFE65100)
            )
        }
    }
}
