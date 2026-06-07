package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {

    // Planning tasks
    fun getTasksByPeriod(periodType: String, periodKey: String): Flow<List<PlanningTask>> {
        return db.planningTaskDao().getTasksByPeriod(periodType, periodKey)
    }

    fun getAllTasks(): Flow<List<PlanningTask>> {
        return db.planningTaskDao().getAllTasks()
    }

    suspend fun insertTask(task: PlanningTask) {
        db.planningTaskDao().insertTask(task)
    }

    suspend fun updateTask(task: PlanningTask) {
        db.planningTaskDao().updateTask(task)
    }

    suspend fun deleteTask(task: PlanningTask) {
        db.planningTaskDao().deleteTask(task)
    }

    // Sport Progress
    fun getAllSportProgress(): Flow<List<SportProgress>> {
        return db.sportProgressDao().getAllSportProgress()
    }

    fun getSportProgressByDateFlow(dateKey: String): Flow<SportProgress?> {
        return db.sportProgressDao().getSportProgressByDateFlow(dateKey)
    }

    suspend fun getSportProgressByDate(dateKey: String): SportProgress? {
        return db.sportProgressDao().getSportProgressByDate(dateKey)
    }

    suspend fun saveSportProgress(progress: SportProgress) {
        db.sportProgressDao().insertSportProgress(progress)
    }

    // Sleep Events and Summaries
    suspend fun insertSleepEvent(event: SleepEvent) {
        db.sleepDao().insertSleepEvent(event)
    }

    suspend fun getSleepEventsSince(since: Long): List<SleepEvent> {
        return db.sleepDao().getSleepEventsSince(since)
    }

    fun getRecentSleepSummaries(): Flow<List<SleepSummary>> {
        return db.sleepDao().getRecentSleepSummaries()
    }

    suspend fun insertSleepSummary(summary: SleepSummary) {
        db.sleepDao().insertSleepSummary(summary)
    }

    // Reminders
    fun getAllReminders(): Flow<List<Reminder>> {
        return db.reminderDao().getAllReminders()
    }

    suspend fun insertReminder(reminder: Reminder) {
        db.reminderDao().insertReminder(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        db.reminderDao().deleteReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        db.reminderDao().updateReminder(reminder)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getRepository(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = AppRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }
}
