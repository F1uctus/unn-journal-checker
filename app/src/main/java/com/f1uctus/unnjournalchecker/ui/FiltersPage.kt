package com.f1uctus.unnjournalchecker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.f1uctus.unnjournalchecker.*
import com.f1uctus.unnjournalchecker.R
import com.f1uctus.unnjournalchecker.common.notificationManager
import com.f1uctus.unnjournalchecker.ui.filters.FilterBox
import com.f1uctus.unnjournalchecker.ui.filters.FilterEditBox
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersPage(
    navController: NavHostController,
    demoMenu: JournalMenu? = null,
    demoFilters: Collection<JournalFilter>? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStore = context.dataStore
    val filters by (demoFilters?.let(::flowOf) ?: dataStore.data
        .map { it[filtersPrefKey] }
        .map {
            it?.map(Json.Default::decodeFromString) ?: listOf()
        })
        .collectAsState(initial = setOf())
    val filter = remember { mutableStateOf(JournalFilter.empty) }
    var filterPopupVisible by remember { mutableStateOf(false) }
    var clearAllDialogVisible by remember { mutableStateOf(false) }
    var logoutDialogVisible by remember { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        floatingActionButton = {
            FloatingActionButton({ filterPopupVisible = true }) {
                Icon(Icons.Filled.AddCircle, "")
            }
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            //
            //
            if (filterPopupVisible) AlertDialog(
                onDismissRequest = { filterPopupVisible = false },
                dismissButton = {
                    OutlinedButton({ filterPopupVisible = false }) {
                        Icon(Icons.Filled.Close, "")
                    }
                },
                confirmButton = {
                    Button({
                        if (filter.value.isEmpty) return@Button
                        scope.launch {
                            dataStore.addFilter(filter.value)
                            filterPopupVisible = false
                            setNextEnrollmentCheckAlarm(context)
                        }
                    }) {
                        Icon(Icons.Filled.Done, "")
                    }
                },
                text = { FilterEditBox(filter, demoMenu) }
            )
            //
            //
            else if (clearAllDialogVisible) AlertDialog(
                text = { Text(stringResource(R.string.confirmResetFiltersDialog)) },
                confirmButton = {
                    Button({
                        scope.launch { dataStore.clearFilters() }
                        context.notificationManager.cancelAll()
                        clearAllDialogVisible = false
                    }) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    Button({ clearAllDialogVisible = false }) {
                        Text(stringResource(R.string.no))
                    }
                },
                onDismissRequest = { clearAllDialogVisible = false }
            )
            //
            //
            else if (logoutDialogVisible) AlertDialog(
                text = { Text(stringResource(R.string.confirmLogoutDialog)) },
                confirmButton = {
                    Button({
                        scope.launch { dataStore.clear() }
                        context.notificationManager.cancelAll()
                        navController.navigate(Routes.Login.route)
                        logoutDialogVisible = false
                    }) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    Button({ logoutDialogVisible = false }) {
                        Text(stringResource(R.string.no))
                    }
                },
                onDismissRequest = { logoutDialogVisible = false }
            )
            //
            //
            else Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    OutlinedIconButton(
                        { clearAllDialogVisible = true },
                        Modifier
                            .weight(1f, true)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Filled.Delete, "")
                    }
                    OutlinedIconButton(
                        { logoutDialogVisible = true },
                        Modifier
                            .weight(1f, true)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Filled.ExitToApp, "")
                    }
                }
                filters.forEach {
                    FilterBox(it, onRefresh = {
                        setNextEnrollmentCheckAlarm(context)
                    }, onDelete = {
                        scope.launch {
                            dataStore.removeFilter(it)
                        }
                    })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FiltersPagePreview() {
    UNNJournalCheckerTheme {
        FiltersPage(
            rememberNavController(),
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
            setOf(JournalFilter(1, 1, 1)),
        )
    }
}