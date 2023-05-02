package com.f1uctus.unnjournalchecker

import android.app.*
import android.content.*
import android.net.Uri
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
import com.f1uctus.unnjournalchecker.common.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.*

const val notificationChannelId = "Enrolling"
private const val pendingIntentRequestCode = 248657
private const val ongoingNotificationId = 151389

private inline fun <reified T> pendingIntent(ctx: Context) =
    PendingIntent.getBroadcast(
        ctx,
        pendingIntentRequestCode,
        Intent(ctx, T::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )!!

suspend fun setEnrollmentCheckAlarm(ctx: Context, delay: Duration = Duration.ZERO) {
    val interval = ctx.dataStore.sectionCheckInterval.first()
    val pendingIntent = pendingIntent<PeriodicEnrollmentCheckReceiver>(ctx)
    val triggerTime = Instant.now().plus(delay)
    ctx.alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        triggerTime.toEpochMilli(),
        interval.toMillis(),
        pendingIntent
    )
    Log.i("setEnrollmentCheckAlarm", "Alarm initiated at $triggerTime")
    withContext(IO) {
        notifyCheckStarted(ctx)
    }
}

class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.i("BootCompleteReceiver", "Intent.ACTION_BOOT_COMPLETED")
            runBlocking {
                setEnrollmentCheckAlarm(  // TODO test
                    ctx,
                    Duration.ofSeconds(15)
                )
            }
        }
    }
}

class PeriodicEnrollmentCheckReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Log.i("PeriodicEnrollmentCheckAlarmReceiver", "Started")
        val wl = ctx.powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "journal:ECA"
        )
        wl.acquire(Duration.ofMinutes(1).toMillis())
        notifyCheckStarted(ctx)
        wl.release()
        Log.i("PeriodicEnrollmentCheckAlarmReceiver", "Completed")
    }
}
    }
}

private fun notifyCheckStarted(ctx: Context) {
    Log.i("notifyCheckStarted", "Started")
    val interval = runBlocking { ctx.dataStore.sectionCheckInterval.first() }
    val nextAlarmTime = LocalDateTime.now().plus(interval)
    updateNotification(ctx) { n ->
        n.setContentTitle(ctx.getString(R.string.checkingTheJournal))
    }
    try {
        notifyOfAvailableSection(ctx)
    } catch (e: Exception) {
        updateNotification(ctx) { n ->
            n.setContentTitle(ctx.getString(R.string.journalCheckFailed))
            n.setStyle(BigTextStyle().bigText(e.toString()))
        }
        return
    }
    val nots = ctx.notificationManager.activeNotifications
    if (nots.isEmpty() || nots[0].notification.priority < PRIORITY_DEFAULT) {
        updateNotification(ctx) { n ->
            n.setContentTitle(
                ctx.getString(R.string.nextCheckAt) + nextAlarmTime.toHoursMinutes
            )
        }
    }
    Log.i("notifyCheckStarted", "Completed. Next check at $nextAlarmTime")
}

private fun notifyOfAvailableSection(ctx: Context) {
    val cookie = runBlocking {
        ctx.dataStore.cookie.first()
    } ?: throw Exception("No credentials")
    val filters = runBlocking {
        ctx.dataStore.filters.first()
    }
    val menu = JournalScraper.extractMenu(cookie)
    val avails = LinkedHashMap<String, Pair<JournalFilter, Section>>()

    var checkDate = LocalDate.now().startOfWeek
    val lastMonth = checkDate.monthValue + 2
    while (checkDate.monthValue <= lastMonth) {
        for (f in filters) {
            if (f.paused) continue

            val secName = menu.section(f) ?: "<N/A>"
            val sections = JournalScraper.extractSections(checkDate, f, cookie)
            for (sec in sections) {
                if (!JournalScraper.isAvailableForEnrollment(sec, cookie)) continue

                val key = "${sec.friendlyDate}: $secName (${menu.lectorSurname(f)})"
                avails[key] = Pair(f, sec)
                updateNotification(ctx) { n ->
                    val openJournalIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            JournalScraper.buildFilterUrl(
                                avails.values.first().second.date,
                                avails.values.first().first
                            )
                        ),
                        ctx,
                        MainActivity::class.java
                    )
                    val pendingIntent = TaskStackBuilder.create(ctx).run {
                        addNextIntentWithParentStack(openJournalIntent)
                        getPendingIntent(
                            ongoingNotificationId,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    }
                    n.priority = PRIORITY_DEFAULT
                    n.setContentTitle(ctx.getString(R.string.enrollmentAvailable))
                    n.setStyle(BigTextStyle().bigText(avails.keys.joinToString("\n")))
                    n.setContentIntent(pendingIntent)
                }
            }
        }
        checkDate = checkDate.plusWeeks(1)
    }

    if (avails.isEmpty()) {
        updateNotification(ctx) { n ->
            n.setContentTitle(ctx.getString(R.string.noEnrollmentOptions))
        }
    }
}

private fun updateNotification(
    ctx: Context,
    builder: (NotificationCompat.Builder) -> NotificationCompat.Builder,
) {
    val openAppIntent = pendingIntent<MainActivity>(ctx)
    val nb = builder(
        NotificationCompat.Builder(ctx, notificationChannelId)
            .setSmallIcon(R.drawable.ic_running)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
    )
    val n = nb.build()
    n.flags = n.flags or Notification.FLAG_FOREGROUND_SERVICE
    ctx.notificationManager.notify(ongoingNotificationId, nb.build())
}
