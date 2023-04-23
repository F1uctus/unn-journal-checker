package com.f1uctus.unnjournalchecker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate

/**
 * Returns the first key corresponding to the given [value], or `null`
 * if such a value is not present in the map.
 */
fun <K, V> Map<K, V>.getKey(value: V) =
    entries.firstOrNull { it.value == value }?.key


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class JournalScraperUnitTest {
    @Test
    fun extractMenu_works() {
        val menu = JournalScraper.extractMenu(auth)
        assertEquals(5, menu.sections.size)
        assertNotEquals(0, menu.lectors.size)
        assertNotEquals(0, menu.buildings.size)
    }

    @Test
    fun extractSections_works() {
        val menu = JournalScraper.extractMenu(auth)
        val sections = JournalScraper.extractSections(
            LocalDate.of(2023, 4, 25),
            JournalFilter(
                section = menu.sections.getKey("Секция БАДМИНТОН"),
                lector = menu.lectors.getKey("Гутко Светлана Николаевна"),
                building = null
            ),
            auth
        )
        assertEquals(2, sections.size)
    }

    @Test
    fun isAvailableForEnrollment_works() {
        val menu = JournalScraper.extractMenu(auth)
        val sections = JournalScraper.extractSections(
            LocalDate.of(2023, 4, 25),
            JournalFilter(
                section = menu.sections.getKey("Секция БАДМИНТОН"),
                lector = menu.lectors.getKey("Гутко Светлана Николаевна"),
                building = null
            ),
            auth
        )
        assertFalse(JournalScraper.isAvailableForEnrollment(sections[1], auth))
    }

    companion object {
        lateinit var auth: CookieAuth

        @JvmStatic
        @BeforeClass
        fun setup() {
            auth = JournalScraper.authenticate("???", "???")!!
        }
    }
}