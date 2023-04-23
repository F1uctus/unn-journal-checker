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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.*


const val notificationChannelId = "Enrolling"
private const val ongoingNotificationId = 151389
private val defaultInterval = Duration.ofMinutes(5)

suspend fun setEnrollmentCheckAlarm(ctx: Context, delay: Duration = Duration.ZERO) {
    val intent = Intent(ctx, EnrollmentCheckAlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        ctx,
        0,
        intent,
        PendingIntent.FLAG_MUTABLE
    )
    val triggerTime = Instant.now().plus(delay)
    ctx.alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        triggerTime.toEpochMilli(),
        defaultInterval.toMillis(),
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
            runBlocking { setEnrollmentCheckAlarm(ctx, Duration.ofSeconds(15)) } // TODO test
        }
    }
}

class EnrollmentCheckAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Log.i("EnrollmentCheckAlarm", "Alarm just fired")
        val wl = ctx.powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "journal:ECA")
        wl.acquire(Duration.ofMinutes(1).toMillis())
        notifyCheckStarted(ctx)
        wl.release()
        Log.i("EnrollmentCheckAlarm", "Alarm completed")
    }
}

private fun notifyCheckStarted(ctx: Context) {
    Log.i("notifyCheckStarted", "Check just started")
    val nextAlarmTime = LocalDateTime.now().plus(defaultInterval)
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
    Log.i("notifyCheckStarted", "Next check at $nextAlarmTime")
}

private fun notifyOfAvailableSection(ctx: Context) {
    val cookie = runBlocking {
        ctx.dataStore.cookie.first()
    } ?: throw Exception("No credentials")
    val filters = runBlocking {
        ctx.dataStore.data
            .map { it[filtersPrefKey] }
            .map { it?.map { Json.decodeFromString<JournalFilter>(it) } ?: setOf() }
            .first()
    }

    val menu = JournalScraper.extractMenu(cookie)

    val avails = LinkedHashMap<String, Pair<JournalFilter, Section>>()

    var checkDate = LocalDate.now().startOfWeek
    val lastMonth = checkDate.monthValue + 2
    while (checkDate.monthValue <= lastMonth) {
        for (sel in filters) {
            val secName = menu.section(sel) ?: "<N/A>"
            val sections = JournalScraper.extractSections(checkDate, sel, cookie)
            for (sec in sections) {
                if (!JournalScraper.isAvailableForEnrollment(sec, cookie)) continue

                avails["${sec.friendlyDate}: $secName (${menu.lectorSurname(sel)})"] =
                    Pair(sel, sec)
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
                            PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_MUTABLE)
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
    val openAppIntent = PendingIntent.getActivity(
        ctx,
        0,
        Intent(ctx, MainActivity::class.java),
        PendingIntent.FLAG_MUTABLE
    )
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
