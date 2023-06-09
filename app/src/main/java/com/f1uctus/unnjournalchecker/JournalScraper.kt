package com.f1uctus.unnjournalchecker

import android.util.Log
import com.f1uctus.unnjournalchecker.common.*
import it.skrape.core.document
import it.skrape.fetcher.*
import it.skrape.selects.*
import it.skrape.selects.html5.*
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

private enum class Patterns(regexString: String) {
    AuthResponse("""\d{4}-\d\d-\d\d\s\d\d:\d\d:\d\d(.*?)OK"""),
    Time("""(\d\d):(\d\d)\s*-\s*(\d\d):(\d\d)"""),
    ClassLink("""getpopup\s*\((\d+)\)"""),
    StyleTop("""top:\s*(\d+)px"""),
    SectionBlockColor(".*#(FFE4C4|B0E0E6).*");

    val regex = Regex(regexString, RegexOption.IGNORE_CASE)
}

@Serializable
data class CookieAuth(val login: String, val hash: String) {
    fun toCookieHeaderString(): String = "login=$login; hash=$hash"
}

object JournalScraper {
    private data class WeekTimetable(
        val timeIntervals: List<String>,
        val sectionLinks: List<DocElement>,
    )

    fun authenticate(login: String, password: String): CookieAuth? {
        val authResponse = try {
            skrape(HttpFetcher) {
                request {
                    method = Method.POST
                    url = "https://journal.unn.ru/auth.php"
                    body {
                        form("login=$login&password=$password")
                    }
                    timeout = 10000
                }
                response { responseBody }
            }
        } catch (e: Exception) {
            Log.w(JournalScraper::class.simpleName, e)
            null
        }
        return authResponse
            ?.let(Patterns.AuthResponse.regex::find)
            ?.let { match -> CookieAuth(login, match.groupValues[1]) }
    }

    fun extractMenu(auth: CookieAuth): JournalMenu {
        return skrape(HttpFetcher) {
            request {
                method = Method.POST
                url = "https://journal.unn.ru/section/getmenu.php"
                headers = mapOf("Cookie" to auth.toCookieHeaderString())
                body {
                    form(
                        listOf(
                            "section=0",
                            "zd=0",
                            "lec=0",
                            "stud=${auth.login}",
                            "date=${LocalDate.now().yearMonthDay}",
                            "type=2",
                            "view=0",
                            "lang=ru",
                        ).joinToString("&")
                    )
                }
            }
            response {
                val mapFromOptions: CssSelector.() -> Map<Int, String> = {
                    findFirst {
                        children.associateBy(
                            { it.attribute("value").toInt() },
                            { it.text.trim() }
                        )
                    }
                }
                JournalMenu(
                    sections = document.select("#section", mapFromOptions)
                        .mapValues { (i, s) -> if (i == 0) "Любая секция" else s },
                    lectors = document.select("#lector", mapFromOptions)
                        .mapValues { (i, s) -> if (i == 0) "Любой преподаватель" else s },
                    buildings = document.select("#zd", mapFromOptions)
                        .mapValues { (i, s) -> if (i == 0) "Любое здание" else s },
                )
            }
        }
    }

    const val FILTER_URL = "https://journal.unn.ru/section/index.php"

    fun buildFilterUrl(
        date: LocalDate,
        selection: JournalFilter,
    ): String {
        return "$FILTER_URL?" + listOf(
            "section[]=${selection.section ?: 0}",
            "zd[]=${selection.building ?: 0}",
            "lec[]=${selection.lector ?: 0}",
            "date=${date.yearMonthDay}",
            "type=2",
            "view=0",
        ).joinToString("&")
    }

    fun extractSections(
        date: LocalDate,
        selection: JournalFilter,
        auth: CookieAuth,
    ): List<Section> {
        val extracted = skrape(HttpFetcher) {
            request {
                method = Method.POST
                url = "https://journal.unn.ru/section/schedule.php"
                headers = mapOf("Cookie" to auth.toCookieHeaderString())
                body {
                    form(
                        listOf(
                            "section=${selection.section ?: 0}",
                            "zd=${selection.building ?: 0}",
                            "lec=${selection.lector ?: 0}",
                            "stud=${auth.login}",
                            "date=${date.yearMonthDay}",
                            "type=2",
                            "view=0",
                            "lang=ru",
                        ).joinToString("&")
                    )
                }
            }
            response {
                WeekTimetable(
                    timeIntervals = document.p {
                        try {
                            findAll {
                                filter { it.text.matches(Patterns.Time.regex) }
                                    .map { it.text }
                            }
                        } catch (e: ElementNotFoundException) {
                            listOf()
                        }
                    },
                    sectionLinks = document.a {
                        try {
                            findAll {
                                filter { e ->
                                    // a clickable section block
                                    (e.attributes["onclick"]?.matches
                                        (Patterns.ClassLink.regex) == true) and
                                        // color of non-occupied section
                                        Patterns.SectionBlockColor.regex
                                            .matches(e.attributes["style"].orEmpty())
                                }
                            }
                        } catch (e: ElementNotFoundException) {
                            listOf()
                        }
                    }
                )
            }
        }

        val sections = ArrayList<Section>()
        for (link in extracted.sectionLinks) {
            try {
                val dayDiv = link.parent.parent.parent

                val dayTotalSections = dayDiv.children[2].children.size
                if (dayTotalSections != extracted.timeIntervals.size) continue

                val topStr = Patterns.StyleTop.regex
                    .find(link.attribute("style"))
                    ?.groupValues?.get(1)
                if (topStr == null || topStr.toIntOrNull() == null) continue
                val top = topStr.toInt()

                val index = (top / 55) - 1
                val timeInterval = extracted.timeIntervals[index]
                val startTime = LocalTime.parse(
                    timeInterval.substringBefore('-').trim(),
                    hoursMinutesFormat
                )
                val endTime = LocalTime.parse(
                    timeInterval.substringAfter('-').trim(),
                    hoursMinutesFormat
                )

                val dayText = dayDiv.children[0].children.last().text
                val dayDate = LocalDate.parse(
                    dayText + "." + LocalDate.now().year,
                    dayMonthYearFormat
                )

                val idStr = Patterns.ClassLink.regex
                    .find(link.attribute("onclick"))
                    ?.groupValues?.get(1)
                if (idStr == null || idStr.toIntOrNull() == null) continue
                val id = idStr.toInt()

                sections.add(Section(dayDate, startTime, endTime, id))
            } catch (e: Exception) {
                continue
            }
        }
        return sections
    }

    fun isAvailableForEnrollment(section: Section, auth: CookieAuth): Boolean {
        if (section.date < LocalDate.now()) return false
        if (section.date == LocalDate.now() && section.end < LocalTime.now()) return false

        val eventInfoText = skrape(HttpFetcher) {
            request {
                method = Method.POST
                url = "https://journal.unn.ru/section/eventinfo.php"
                headers = mapOf("Cookie" to auth.toCookieHeaderString())
                body {
                    form("oid=${section.id}")
                }
            }
            response {
                document.text
            }
        }

        return eventInfoText.contains("Запись на секцию на этот день")
    }
}
