package com.f1uctus.unnjournalchecker

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Serializable
data class JournalMenu(
    val sections: Map<Int, String>,
    val lectors: Map<Int, String>,
    val buildings: Map<Int, String>,
) {
    fun section(f: JournalFilter): String? =
        if (f.section == 0) null else sections[f.section]

    fun lector(f: JournalFilter): String? =
        if (f.lector == 0) null else lectors[f.lector]

    fun lectorSurname(f: JournalFilter): String? =
        lector(f)?.let { it.split(" ")[0] }

    fun building(f: JournalFilter): String? =
        if (f.building == 0) null else buildings[f.building]

    companion object {
        val empty = JournalMenu(mapOf(0 to ""), mapOf(0 to ""), mapOf(0 to ""))
    }
}

@Serializable
data class JournalFilter(
    val section: Int?,
    val lector: Int?,
    val building: Int?,
    val paused: Boolean = false
) {
    val isEmpty: Boolean
        get() = (section ?: 0) + (lector ?: 0) + (building ?: 0) == 0

    companion object {
        val empty = JournalFilter(0, 0, 0)
    }
}

data class Section(
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val id: Int
) {
    private val dateReprFmt = DateTimeFormatter.ofPattern("dd MMM")!!

    val friendlyDate: String get() = dateReprFmt.format(date)
}
