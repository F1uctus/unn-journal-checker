package com.f1uctus.unnjournalchecker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.f1uctus.unnjournalchecker.*
import com.f1uctus.unnjournalchecker.R
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val dataStore = LocalContext.current.dataStore
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, alignment = CenterVertically),
            horizontalAlignment = CenterHorizontally,
        ) {

            var username by remember { mutableStateOf(TextFieldValue()) }
            var password by remember { mutableStateOf(TextFieldValue()) }
            var authorizing by remember { mutableStateOf(false) }

            TextField(
                label = { Text(stringResource(R.string.username)) },
                value = username,
                onValueChange = { username = it }
            )
            TextField(
                label = { Text(stringResource(R.string.password)) },
                value = password,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                onValueChange = { password = it }
            )
            Box(modifier = Modifier.padding(horizontal = 40.dp)) {
                FilledTonalButton(
                    onClick = {
                        authorizing = true
                        scope.launch {
                            dataStore.setCookie(
                                JournalScraper.authenticate(
                                    username.text,
                                    password.text
                                )
                            )
                            authorizing = false
                            navController.navigate(Routes.Filters.route)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.logIn))
                }
            }
            if (authorizing) LoadingDialog()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPagePreview() {
    UNNJournalCheckerTheme {
        LoginPage(rememberNavController())
    }
}