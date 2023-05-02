package com.f1uctus.unnjournalchecker.ui.filters

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.f1uctus.unnjournalchecker.*
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun FilterControlBox(
    filter: JournalFilter,
    onPause: (JournalFilter) -> Unit,
    onRefresh: (JournalFilter) -> Unit,
    onDelete: (JournalFilter) -> Unit,
    demoMenu: JournalMenu? = null
) {
    val dataStore = LocalContext.current.dataStore
    val menu by demoMenu
        ?.let(::mutableStateOf)?.let { remember { it } }
        ?: dataStore.menu.filterNotNull()
            .collectAsState(initial = JournalMenu.empty)
    Card {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOfNotNull(
                menu.section(filter),
                menu.lector(filter),
                menu.building(filter),
            ).map { text ->
                ElevatedCard {
                    Text(
                        text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                OutlinedButton({ onDelete(filter) }) {
                    Icon(Icons.Filled.Delete, "")
                }
                ElevatedButton({ onRefresh(filter) }) {
                    Icon(Icons.Filled.Refresh, "")
                }
                ElevatedButton({ onPause(filter) }) {
                    Icon(
                        if (filter.paused)
                            Icons.Filled.PlayArrow
                        else
                            Icons.Filled.Pause,
                        ""
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FilterBoxPreview() {
    UNNJournalCheckerTheme {
        FilterControlBox(
            filter = JournalFilter(1, 1, 1),
            onPause = {},
            onRefresh = {},
            onDelete = {},
            demoMenu = JournalMenu(
                sections = mapOf(
                    1 to "Секция БАДМИНТОН",
                    2 to "Секция ЙОГА"
                ),
                lectors = mapOf(
                    1 to "Канатьев Константин Николаевич",
                    2 to "Беляева Марина Александровна",
                ),
                buildings = mapOf(
                    1 to "Спортивный комплекс на Гагарина"
                )
            )
        )
    }
}
