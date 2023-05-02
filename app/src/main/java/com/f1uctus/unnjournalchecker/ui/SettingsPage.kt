package com.f1uctus.unnjournalchecker.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.f1uctus.unnjournalchecker.*
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme
import kotlinx.coroutines.launch
import java.time.Duration

/** 'месяц', 'месяца', 'месяцев' */
fun unitCase(n: Long, vararg titles: String): String = "$n ${
    titles[
        if (n % 100 in 5..19) 2
        else arrayOf(2, 0, 1, 1, 1, 2)[(n % 10).coerceAtMost(5).toInt()]
    ]
}"

fun unitCase(n: Int, vararg titles: String): String =
    unitCase(n.toLong(), *titles)

@Composable
fun SettingsPage(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataStore = context.dataStore

    val sectionCheckInterval by dataStore.sectionCheckInterval
        .collectAsState(initial = defaultSectionCheckInterval)

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, min: Int ->
            scope.launch {
                dataStore.setSectionCheckInterval(
                    Duration.ofHours(hour.toLong()).plusMinutes(min.toLong())
                )
            }
        },
        0,
        10,
        true
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement
                .spacedBy(20.dp, alignment = Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Row {
                Text(
                    "Проверка секций",
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .weight(1f),
                )
            }
            ElevatedCard(
                Modifier
                    .fillMaxWidth()
                    .clickable { timePickerDialog.show() }
            ) {
                Row {
                    Text(
                        "Каждые",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.CenterVertically)
                            .weight(2f),
                    )
                    Box(
                        Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .weight(1f)
                    ) {
                        Text(
                            unitCase(
                                sectionCheckInterval.toMinutes(),
                                "минуту",
                                "минуты",
                                "минут"
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPagePreview() {
    UNNJournalCheckerTheme {
        SettingsPage(rememberNavController())
    }
}
