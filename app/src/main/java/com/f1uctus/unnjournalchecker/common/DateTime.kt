package com.f1uctus.unnjournalchecker.common

import java.time.*
import java.time.format.DateTimeFormatter

val LocalDate.startOfWeek: LocalDate
    get() {
        var d = this
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.minusDays(1)
        return d
    }

val hoursMinutesFormat = DateTimeFormatter.ofPattern("HH:mm")!!
val LocalTime.toHoursMinutes: String get() = format(hoursMinutesFormat)

val dayMonthYearFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")!!

private val yearMonthDayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")!!
val LocalDate.yearMonthDay: String get() = format(yearMonthDayFmt)
