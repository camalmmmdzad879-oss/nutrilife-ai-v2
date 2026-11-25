package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.domain.model.AppLanguage
import com.example.domain.model.ReminderSettings
import com.example.i18n.stringsFor
import java.util.concurrent.TimeUnit

class NutriReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val langCode = inputData.getString(KEY_LANG) ?: "az"
        val kind = inputData.getString(KEY_KIND) ?: "general"
        val strings = stringsFor(AppLanguage.fromCode(langCode))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        ensureChannel(applicationContext)

        val body = when (kind) {
            "water" -> strings.waterIntakeTitle + " — " + strings.reminderNotificationBody
            "weight" -> strings.logWeightTitle + " — " + strings.reminderNotificationBody
            "meal" -> strings.todaysMealsTitle + " — " + strings.reminderNotificationBody
            else -> strings.reminderNotificationBody
        }

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            kind.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(strings.reminderNotificationTitle)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(kind.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "nutrilife_reminders"
        const val KEY_LANG = "lang"
        const val KEY_KIND = "kind"

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "NutriLife Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }
}

object ReminderScheduler {
    private const val WORK_MEAL = "nutrilife_meal_reminder"
    private const val WORK_WATER = "nutrilife_water_reminder"
    private const val WORK_WEIGHT = "nutrilife_weight_reminder"
    private const val WORK_DIARY = "nutrilife_diary_reminder"

    fun syncReminders(context: Context, settings: ReminderSettings, language: AppLanguage) {
        val wm = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
        NutriReminderWorker.ensureChannel(context)

        scheduleOrCancel(wm, WORK_MEAL, settings.mealRemindersEnabled, 8L, TimeUnit.HOURS, language, "meal")
        scheduleOrCancel(wm, WORK_WATER, settings.waterRemindersEnabled, settings.waterIntervalHours.toLong().coerceAtLeast(1L), TimeUnit.HOURS, language, "water")
        scheduleOrCancel(wm, WORK_WEIGHT, settings.weightRemindersEnabled, 24L, TimeUnit.HOURS, language, "weight")
        scheduleOrCancel(wm, WORK_DIARY, settings.diaryRemindersEnabled, 12L, TimeUnit.HOURS, language, "diary")
    }

    fun cancelAll(context: Context) {
        val wm = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
        listOf(WORK_MEAL, WORK_WATER, WORK_WEIGHT, WORK_DIARY).forEach { wm.cancelUniqueWork(it) }
    }

    private fun scheduleOrCancel(
        wm: WorkManager,
        workName: String,
        enabled: Boolean,
        interval: Long,
        unit: TimeUnit,
        language: AppLanguage,
        kind: String
    ) {
        if (!enabled) {
            wm.cancelUniqueWork(workName)
            return
        }
        val request = PeriodicWorkRequestBuilder<NutriReminderWorker>(interval, unit)
            .setInputData(
                workDataOf(
                    NutriReminderWorker.KEY_LANG to language.code,
                    NutriReminderWorker.KEY_KIND to kind
                )
            )
            .build()
        wm.enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
