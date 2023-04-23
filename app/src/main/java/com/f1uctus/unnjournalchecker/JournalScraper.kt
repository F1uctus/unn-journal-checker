package com.f1uctus.unnjournalchecker

import it.skrape.core.document
import it.skrape.fetcher.*
import it.skrape.selects.*
import it.skrape.selects.html5.*
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val hourMinuteFmt = DateTimeFormatter.ofPattern("HH:mm")!!
val dmyFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")!!
val ymdFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")!!

private val authResponseRegex = Regex("""\d{4}-\d\d-\d\d\s\d\d:\d\d:\d\d(.*?)OK""", RegexOption.IGNORE_CASE)
private val timeRegex = Regex("""(\d\d):(\d\d)\s*-\s*(\d\d):(\d\d)""")
private val classLinkRegex = Regex("""getpopup\s*\((\d+)\)""", RegexOption.IGNORE_CASE)
private val styleTopRegex = Regex("""top:\s*(\d+)px""", RegexOption.IGNORE_CASE)

@Serializable
data class CookieAuth(val login: String, val hash: String) {
    fun toFormString(): String = "login=$login; hash=$hash"
}

class JournalScraper {
    private data class WeekTimetable(
        val timeIntervals: List<String>,
        val sectionLinks: List<DocElement>,
    )

    fun authenticate(login: String, password: String): CookieAuth? {
        val authResponse = skrape(HttpFetcher) {
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
        return authResponseRegex.find(authResponse)?.let { match ->
            CookieAuth(login, match.groupValues[1])
        }
    }

    fun extractMenu(auth: CookieAuth): JournalMenu {
        return skrape(HttpFetcher) {
            request {
                method = Method.POST
                url = "https://journal.unn.ru/section/getmenu.php"
                headers = mapOf("Cookie" to auth.toFormString())
                body {
                    form(
                        listOf(
                            "section=0",
                            "zd=0",
                            "lec=0",
                            "stud=${auth.login}",
                            "date=${LocalDate.now().format(ymdFmt)}",
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
                        children.associateBy({ it.attribute("value").toInt() }, { it.text.trim() })
                    }
                }
                JournalMenu(
                    lectors = document.select("#lector", mapFromOptions),
                    sections = document.select("#section", mapFromOptions),
                    buildings = document.select("#zd", mapFromOptions),
                )
            }
        }
    }

    fun buildFilterUrl(
        date: LocalDate,
        selection: JournalFilter,
    ): String {
        return "https://journal.unn.ru/section/index.php?" + listOf(
            "section[]=${selection.section ?: 0}",
            "zd[]=${selection.building ?: 0}",
            "lec[]=${selection.lector ?: 0}",
            "date=${date.format(ymdFmt)}",
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
                headers = mapOf("Cookie" to auth.toFormString())
                body {
                    form(
                        listOf(
                            "section=${selection.section ?: 0}",
                            "zd=${selection.building ?: 0}",
                            "lec=${selection.lector ?: 0}",
                            "stud=${auth.login}",
                            "date=${date.format(ymdFmt)}",
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
                            findAll { filter { it.text.matches(timeRegex) }.map { it.text } }
                        } catch (e: ElementNotFoundException) {
                            listOf()
                        }
                    },
                    sectionLinks = document.a {
                        try {
                            findAll {
                                filter { e ->
                                    // a clickable section block
                                    (e.attributes["onclick"]?.matches(classLinkRegex) == true) and
                                        // color of non-occupied section
                                        Regex(".*#(FFE4C4|B0E0E6).*", RegexOption.IGNORE_CASE)
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

                val topStr = styleTopRegex.find(link.attribute("style"))?.groupValues?.get(1)
                if (topStr == null || topStr.toIntOrNull() == null) continue
                val top = topStr.toInt()

                val index = (top / 55) - 1
                val timeInterval = extracted.timeIntervals[index];
                val startTime = LocalTime.parse(
                    timeInterval.substringBefore('-').trim(),
                    hourMinuteFmt
                )
                val endTime = LocalTime.parse(
                    timeInterval.substringAfter('-').trim(),
                    hourMinuteFmt
                )

                val dayText = dayDiv.children[0].children.last().text
                val dayDate = LocalDate.parse(dayText + "." + LocalDate.now().year, dmyFmt)

                val idStr = classLinkRegex.find(link.attribute("onclick"))?.groupValues?.get(1)
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
                headers = mapOf("Cookie" to auth.toFormString())
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
