package com.f1uctus.unnjournalchecker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <K, V> DropdownEditBox(items: Map<K, V>, onSelect: (K, V) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val indexedItems = buildMap { items.onEachIndexed(::put) }
    var selectedIndex by remember { mutableStateOf(0) }
    ElevatedCard {
        Text(
            items[indexedItems[selectedIndex]!!.key].toString(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            indexedItems.forEach { (index, entry) ->
                DropdownMenuItem(
                    { Text(entry.value.toString()) },
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
