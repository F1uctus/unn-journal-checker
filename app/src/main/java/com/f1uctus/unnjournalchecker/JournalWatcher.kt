package com.f1uctus.unnjournalchecker

import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.f1uctus.unnjournalchecker.common.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.LocalDate

const val notificationChannelId = "Enrolling"
private const val ongoingNotificationId = 151389

fun setNextEnrollmentCheckAlarm(ctx: Context, delay: Duration) {
    val intent = Intent(ctx, EnrollmentCheckAlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        ctx,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
    ctx.alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        System.currentTimeMillis() + delay.toMillis(),
        pendingIntent
    )
    Toast.makeText(ctx, "Проверка журнала через: $delay", Toast.LENGTH_SHORT).show()
}

class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.i("BootCompleteReceiver", "Intent.ACTION_BOOT_COMPLETED")
            setNextEnrollmentCheckAlarm(ctx, Duration.ofSeconds(5)) // TODO test
        }
    }
}

class EnrollmentCheckAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Log.d("EnrollmentCheckAlarm", "Alarm just fired")
        val wl = ctx.powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "journal:ECA")
        wl.acquire(10 * 60 * 1000L /* 10 minutes */)
        try {
            notifyOfAvailableSection(ctx)
        } catch (e: Exception) {
            return
        }
        setNextEnrollmentCheckAlarm(ctx, Duration.ofMinutes(5))
        wl.release()
    }

    private fun notifyOfAvailableSection(ctx: Context) {
        val dataStore = ctx.dataStore
        val credentials = runBlocking {
            dataStore.credentials.first()
        } ?: throw Exception("No credentials")
        val filters = runBlocking {
            dataStore.data
                .map { it[filtersPrefKey] }
                .map { it?.map { Json.decodeFromString<JournalFilter>(it) } ?: setOf() }
                .first()
        }

        val cookie = scraper.authenticate(credentials.first, credentials.second)
            ?: throw Exception("API authentication failed")

        val menu = scraper.extractMenu(cookie)

        val avails = LinkedHashMap<String, Pair<JournalFilter, Section>>()

        var checkDate = LocalDate.now().startOfWeek
        val lastMonth = checkDate.monthValue + 2
        while (checkDate.monthValue <= lastMonth) {
            for (sel in filters) {
                val secName = menu.section(sel) ?: "<N/A>"
                val sections = scraper.extractSections(checkDate, sel, cookie)
                for (sec in sections) {
                    if (!scraper.isAvailableForEnrollment(sec, cookie)) continue

                    avails["${sec.friendlyDate}: $secName (${menu.lectorSurname(sel)})"] =
                        Pair(sel, sec)
                    updateNotification(ctx) { n ->
                        val openJournalIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                scraper.buildFilterUrl(
                                    avails.values.first().second.date,
                                    avails.values.first().first
                                )
                            ),
                        )
                        val intent = PendingIntent.getActivity(
                            ctx,
                            0,
                            openJournalIntent,
                            PendingIntent.FLAG_IMMUTABLE
                        )
                        n.setContentTitle("Доступна запись")
                            .setStyle(
                                NotificationCompat.BigTextStyle()
                                    .bigText(avails.keys.joinToString("\n"))
                            )
                            .setContentIntent(intent)
                    }
                }
            }
            checkDate = checkDate.plusWeeks(1)
        }
    }

    private fun updateNotification(
        ctx: Context,
        builder: (NotificationCompat.Builder) -> NotificationCompat.Builder,
    ) {
        val nb = builder(
            NotificationCompat.Builder(ctx, notificationChannelId)
                .setSmallIcon(R.drawable.ic_running)
                .setOngoing(true)
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            nb.priority = NotificationCompat.PRIORITY_DEFAULT
        }
        ctx.notificationManager.notify(ongoingNotificationId, nb.build())
    }
}
