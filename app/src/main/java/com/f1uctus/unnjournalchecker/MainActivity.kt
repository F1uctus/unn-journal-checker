package com.f1uctus.unnjournalchecker

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.f1uctus.unnjournalchecker.common.*
import com.f1uctus.unnjournalchecker.ui.FilterBox
import com.f1uctus.unnjournalchecker.ui.FilterEditBox
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.time.*


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
val credentialsPrefKey = stringPreferencesKey("credentials")
val filtersPrefKey = stringSetPreferencesKey("filters")

val credentialSep = ":".repeat(10)
val scraper = JournalScraper()

val DataStore<Preferences>.credentials: Flow<Pair<String, String>?>
    get() = data
        .map { it[credentialsPrefKey] }
        .map {
            it?.split(credentialSep, limit = 2)
                ?.let { l -> Pair(l[0], l[1]) }
                ?: return@map null
        }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Enrolling check results",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val receiver = ComponentName(applicationContext, BootCompleteReceiver::class.java)
        applicationContext.packageManager?.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        lifecycleScope.launch {
            applicationContext.dataStore.data.first()
            applicationContext.dataStore.credentials
                .onEach {
                    setContent {
                        App(it)
                    }
                }
                .first()
        }
    }
}

@Composable
fun App(credentials: Pair<String, String>?) {
    val navController = rememberNavController()
    UNNJournalCheckerTheme {
        NavHost(
            navController = navController,
            startDestination = Routes.Login.route
        ) {
            // TODO refactor & test
            //  Temporarily save cookie to the storage
            composable(Routes.Login.route) {
                if (credentials == null) {
                    LoginPage(navController = navController)
                    return@composable
                }
                val cookie = scraper.authenticate(credentials.first, credentials.second)!!
                val menu = scraper.extractMenu(cookie)
                FiltersPage(menu, navController = navController)
            }
            composable("${Routes.Filters.route}/{cookie}") {
                val cookie = Json.decodeFromString<CookieAuth>(
                    it.arguments
                        ?.getString("cookie")
                        ?.let(String::fromBase64)
                        ?: return@composable
                )
                val menu = scraper.extractMenu(cookie)
                FiltersPage(menu, navController = navController)
            }
        }
    }
}

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Filters : Routes("filters")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPage(
    navController: NavHostController,
) {
    val scope = rememberCoroutineScope()
    val dataStore = LocalContext.current.dataStore
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            var username by remember { mutableStateOf(TextFieldValue()) }
            var password by remember { mutableStateOf(TextFieldValue()) }

            TextField(
                label = { Text(text = "Имя пользователя") },
                value = username,
                onValueChange = { username = it }
            )
            TextField(
                label = { Text(text = "Пароль") },
                value = password,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                onValueChange = { password = it }
            )
            Box(modifier = Modifier.padding(horizontal = 40.dp)) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            dataStore.edit { prefs ->
                                prefs[credentialsPrefKey] = listOf(username.text, password.text)
                                    .joinToString(credentialSep)
                            }
                            val cookie = Json.encodeToString(
                                scraper.authenticate(username.text, password.text)!!
                            ).toBase64()
                            navController.navigate("${Routes.Filters.route}/$cookie")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(text = "Войти")
                }
            }
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

@Composable
fun FiltersPage(
    menu: JournalMenu,
    navController: NavHostController,
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
    var filterPopupVisible by remember { mutableStateOf(false) }
    var clearAllDialogVisible by remember { mutableStateOf(false) }
    var logoutDialogVisible by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        //
        //
        if (filterPopupVisible) Popup(
            alignment = Alignment.Center,
            onDismissRequest = { filterPopupVisible = false },
            properties = PopupProperties(focusable = true)
        ) {
            FilterEditBox(menu, onClose = { filterPopupVisible = false }) { f ->
                if (f.isEmpty) return@FilterEditBox
                scope.launch {
                    dataStore.edit { ps ->
                        val x = setOf(Json.encodeToString(f))
                        ps[filtersPrefKey] = ps[filtersPrefKey]?.plus(x) ?: x
                    }
                    filterPopupVisible = false
                    setNextEnrollmentCheckAlarm(context, Duration.ofSeconds(1))
                }
            }
        }
        //
        //
        else if (clearAllDialogVisible) AlertDialog(
            text = { Text("Сбросить все фильтры?") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        dataStore.edit { it.remove(filtersPrefKey) }
                    }
                    clearAllDialogVisible = false
                }) {
                    Text("Да")
                }
            },
            dismissButton = {
                Button(onClick = { clearAllDialogVisible = false }) {
                    Text("Нет")
                }
            },
            onDismissRequest = { clearAllDialogVisible = false }
        )
        //
        //
        else if (logoutDialogVisible) AlertDialog(
            text = { Text("Разлогиниться и сбросить все фильтры?") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        dataStore.edit {
                            it.remove(credentialsPrefKey)
                            it.remove(filtersPrefKey)
                        }
                    }
                    navController.navigate(Routes.Login.route)
                    logoutDialogVisible = false
                }) {
                    Text("Да")
                }
            },
            dismissButton = {
                Button(onClick = { logoutDialogVisible = false }) {
                    Text("Нет")
                }
            },
            onDismissRequest = { clearAllDialogVisible = false }
        )
        //
        //
        else Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row {
                OutlinedIconButton(
                    onClick = { clearAllDialogVisible = true },
                    modifier = Modifier
                        .weight(1f, true)
                        .height(50.dp)
                ) {
                    Icon(Icons.Filled.Delete, "")
                }
                Spacer(Modifier.padding(10.dp))
                OutlinedIconButton(
                    onClick = { logoutDialogVisible = true },
                    modifier = Modifier
                        .weight(1f, true)
                        .height(50.dp)
                ) {
                    Icon(Icons.Filled.ExitToApp, "")
                }
                Spacer(Modifier.padding(10.dp))
                FilledTonalIconButton(
                    onClick = { filterPopupVisible = true },
                    modifier = Modifier
                        .weight(1f, true)
                        .height(50.dp)
                ) {
                    Icon(Icons.Filled.AddCircle, "")
                }
            }
            filters.forEach {
                FilterBox(it, menu) {
                    scope.launch {
                        dataStore.edit { ps ->
                            val x = setOf(Json.encodeToString(it))
                            ps[filtersPrefKey] = ps[filtersPrefKey]?.minus(x) ?: x
                        }
                    }
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
            rememberNavController(),
            setOf(JournalFilter(1, 1, 1)),
        )
    }
}
