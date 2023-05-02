package com.f1uctus.unnjournalchecker.ui.filters

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.f1uctus.unnjournalchecker.*
import com.f1uctus.unnjournalchecker.ui.DropdownEditBox
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun FilterEditBox(
    filter: MutableState<JournalFilter>,
    demoMenu: JournalMenu? = null,
) {
    var f by filter
    val scope = rememberCoroutineScope()
    val dataStore = LocalContext.current.dataStore
    val menu by (demoMenu?.let(::mutableStateOf)?.let { remember { it } })
        ?: dataStore.menu.collectAsState(initial = null)
    Column(
        verticalArrangement = Arrangement.spacedBy(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .defaultMinSize(minWidth = 100.dp, minHeight = 155.dp)
    ) {
        if (menu == null) {
            CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
            LaunchedEffect(0) {
                scope.launch {
                    dataStore.setMenu(
                        dataStore.cookie.first()?.let(JournalScraper::extractMenu)
                    )
                }
            }
        } else {
            DropdownEditBox(menu!!.sections) { id, _ ->
                f = f.copy(section = id)
            }
            DropdownEditBox(menu!!.lectors) { id, _ ->
                f = f.copy(lector = id)
            }
            DropdownEditBox(menu!!.buildings) { id, _ ->
                f = f.copy(building = id)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FilterEditBoxPreview() {
    UNNJournalCheckerTheme {
        FilterEditBox(
            remember {
                mutableStateOf(JournalFilter(1, 1, 1))
            },
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
        )
    }
}
