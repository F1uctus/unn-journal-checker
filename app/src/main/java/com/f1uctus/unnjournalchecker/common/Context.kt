package com.f1uctus.unnjournalchecker.common

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import androidx.core.content.ContextCompat

val Context.alarmManager
    get() =
        ContextCompat.getSystemService(this, AlarmManager::class.java)!!

val Context.powerManager
    get() =
        ContextCompat.getSystemService(this, PowerManager::class.java)!!

val Context.notificationManager
    get() =
        ContextCompat.getSystemService(this, NotificationManager::class.java)!!
