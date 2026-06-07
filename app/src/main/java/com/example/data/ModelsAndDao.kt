package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Entité représentant une tâche ou un objectif planifié.
 */
@Entity(tableName = "tasks")
data class PlanningTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val periodType: String, // "DAY", "WEEK", "MONTH"
    val periodKey: String,   // "YYYY-MM-DD", "YYYY-Www", "YYYY-MM"
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entité représentant le progrès de l'activité sportive d'un jour.
 */
@Entity(tableName = "sport_activities")
data class SportProgress(
    @PrimaryKey val dateKey: String, // "YYYY-MM-DD"
    val exerciseName: String,
    val isCompleted: Boolean = false,
    val completionTime: Long = 0L
)

/**
 * Entité pour enregistrer les événements d'activité de l'écran ou téléphone.
 */
@Entity(tableName = "sleep_events")
data class SleepEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val eventType: String // "SCREEN_ON", "SCREEN_OFF"
)

/**
 * Résumé de la qualité et durée de sommeil calculée pour une nuit donnée.
 */
@Entity(tableName = "sleep_summaries")
data class SleepSummary(
    @PrimaryKey val dateKey: String, // "YYYY-MM-DD" (jour de réveil)
    val bedtimeMillis: Long,
    val wakeTimeMillis: Long,
    val durationMinutes: Int,
    val efficiency: Int // Pourcentage par exemple 0-100%
)

/**
 * Entité représentant un rappel quotidien personnalisé.
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val message: String,
    val isEnabled: Boolean = true
)

/**
 * Dao pour les tâches de planification.
 */
@Dao
interface PlanningTaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<PlanningTask>>

    @Query("SELECT * FROM tasks WHERE periodType = :periodType AND periodKey = :periodKey ORDER BY createdAt DESC")
    fun getTasksByPeriod(periodType: String, periodKey: String): Flow<List<PlanningTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PlanningTask)

    @Update
    suspend fun updateTask(task: PlanningTask)

    @Delete
    suspend fun deleteTask(task: PlanningTask)
}

/**
 * Dao pour le suivi de sport.
 */
@Dao
interface SportProgressDao {
    @Query("SELECT * FROM sport_activities ORDER BY dateKey DESC")
    fun getAllSportProgress(): Flow<List<SportProgress>>

    @Query("SELECT * FROM sport_activities WHERE dateKey = :dateKey")
    suspend fun getSportProgressByDate(dateKey: String): SportProgress?

    @Query("SELECT * FROM sport_activities WHERE dateKey = :dateKey")
    fun getSportProgressByDateFlow(dateKey: String): Flow<SportProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSportProgress(progress: SportProgress)
}

/**
 * Dao pour le suivi du sommeil.
 */
@Dao
interface SleepDao {
    @Query("SELECT * FROM sleep_events WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getSleepEventsSince(since: Long): List<SleepEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepEvent(event: SleepEvent)

    @Query("SELECT * FROM sleep_summaries ORDER BY dateKey DESC LIMIT 30")
    fun getRecentSleepSummaries(): Flow<List<SleepSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepSummary(summary: SleepSummary)
}

/**
 * Dao pour la gestion des rappels personnalisés.
 */
@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY hour ASC, minute ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Update
    suspend fun updateReminder(reminder: Reminder)
}
