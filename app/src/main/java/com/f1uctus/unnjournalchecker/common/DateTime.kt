package com.f1uctus.unnjournalchecker.common

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

val LocalDate.startOfWeek: LocalDate
    get() {
        var d = this
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.minusDays(1)
        return d
    }

val hoursMinutesFormat = DateTimeFormatter.ofPattern("HH:mm")!!
val LocalDateTime.toHoursMinutes: String get() = hoursMinutesFormat.format(this)

val dayMonthYearFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")!!

private val yearMonthDayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")!!
val TemporalAccessor.yearMonthDay: String get() = yearMonthDayFmt.format(this)
