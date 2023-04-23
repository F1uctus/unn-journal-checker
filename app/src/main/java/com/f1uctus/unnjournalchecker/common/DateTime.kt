package com.f1uctus.unnjournalchecker.common

import java.time.DayOfWeek
import java.time.LocalDate

val LocalDate.startOfWeek: LocalDate
    get() {
        var d = this
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.minusDays(1)
        return d
    }
