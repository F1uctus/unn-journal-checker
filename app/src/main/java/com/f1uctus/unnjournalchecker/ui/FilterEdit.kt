package com.f1uctus.unnjournalchecker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.f1uctus.unnjournalchecker.JournalFilter
import com.f1uctus.unnjournalchecker.JournalMenu
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme

@Composable
fun DropdownEditBox(items: Map<Int, String>, onSelect: (Int, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val indexedItems = buildMap { items.onEachIndexed(::put) }
    var selectedIndex by remember { mutableStateOf(0) }
    ElevatedCard {
        Text(
            items[indexedItems[selectedIndex]!!.key].toString(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .clickable(onClick = { expanded = true })
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            indexedItems.forEach { (index, entry) ->
                DropdownMenuItem(
                    { Text(text = entry.value) },
                    modifier = Modifier
                        .padding(horizontal = 20.dp),
                    onClick = {
                        selectedIndex = index
                        expanded = false
                        onSelect(entry.key, entry.value)
                    }
                )
            }
        }
    }
}

@Composable
fun FilterEditBox(
    menu: JournalMenu,
    onClose: () -> Unit,
    onSave: (JournalFilter) -> Unit
) {
    var settings by remember {
        mutableStateOf(JournalFilter(0, 0, 0))
    }
    Card {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            DropdownEditBox(menu.sections) { id, _ ->
                settings = settings.copy(section = id)
            }
            DropdownEditBox(menu.lectors) { id, _ ->
                settings = settings.copy(lector = id)
            }
            DropdownEditBox(menu.buildings) { id, _ ->
                settings = settings.copy(building = id)
            }
            Row {
                OutlinedButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, "")
                }
                Spacer(androidx.compose.ui.Modifier.padding(10.dp))
                Button(onClick = { onSave(settings) }) {
                    Icon(Icons.Filled.Done, "")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FilterEditBoxPreview() {
    UNNJournalCheckerTheme {
        FilterEditBox(
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
            ),
            {},
            {},
        )
    }
}
