package com.f1uctus.unnjournalchecker.ui.pages

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.f1uctus.unnjournalchecker.*
import com.f1uctus.unnjournalchecker.R
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

@Composable
fun Section(
    text: String? = null,
    content: @Composable (ColumnScope.() -> Unit)
) {
    ElevatedCard {
        Column(
            Modifier.padding(15.dp),
            verticalArrangement = Arrangement
                .spacedBy(10.dp, alignment = Alignment.Top),
        ) {
            if (text != null) Text(text)
            content()
        }
    }
}

@Composable
fun SettingsPage(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ds = context.dataStore

    val sectionCheckInterval by ds.sectionCheckInterval
        .collectAsState(initial = defaultSectionCheckInterval)

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour: Int, min: Int ->
            scope.launch {
                ds.setSectionCheckInterval(
                    Duration.ofHours(hour.toLong()).plusMinutes(min.toLong())
                )
            }
        },
        0,
        10,
        true
    )

    var excludeFromPowerSavingDialogIsOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement
                .spacedBy(15.dp, alignment = Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            Section(stringResource(R.string.enrollmentCheckParameters)) {
                OutlinedCard(
                    Modifier
                        .fillMaxWidth()
                        .clickable { timePickerDialog.show() }
                ) {
                    Row {
                        Text(
                            stringResource(R.string.every),
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
            Section(stringResource(R.string.miscellaneous)) {
                OutlinedButton(
                    { excludeFromPowerSavingDialogIsOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.addToPowerSavingWhitelist))
                }
            }
        }
    }

    if (excludeFromPowerSavingDialogIsOpen) {
        ExcludeFromPowerSavingDialog {
            excludeFromPowerSavingDialogIsOpen = false
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
