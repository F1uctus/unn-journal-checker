package com.f1uctus.unnjournalchecker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.f1uctus.unnjournalchecker.JournalFilter
import com.f1uctus.unnjournalchecker.JournalMenu
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme

@Composable
fun FilterBox(
    filter: JournalFilter,
    menu: JournalMenu,
    onDelete: (JournalFilter) -> Unit
) {
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
            ElevatedButton(onClick = { onDelete(filter) }) {
                Icon(Icons.Filled.Delete, "")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FilterBoxPreview() {
    UNNJournalCheckerTheme {
        FilterBox(
            JournalFilter(1, 1, 1),
            JournalMenu(
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
        ) {}
    }
}
