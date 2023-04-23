package com.f1uctus.unnjournalchecker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.*
import androidx.navigation.navDeepLink
import com.f1uctus.unnjournalchecker.JournalScraper.FILTER_URL
import com.f1uctus.unnjournalchecker.common.notificationManager
import com.f1uctus.unnjournalchecker.ui.*
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.dataStore by preferencesDataStore(name = "settings")
val cookiePrefKey = stringPreferencesKey("cookie")
val menuPrefKey = stringPreferencesKey("menu")
val filtersPrefKey = stringSetPreferencesKey("filters")

val DataStore<Preferences>.cookie: Flow<CookieAuth?>
    get() = data
        .map { it[cookiePrefKey] }
        .map { it?.let(Json.Default::decodeFromString) }

suspend fun DataStore<Preferences>.setCookie(cookie: CookieAuth?) =
    edit {
        it[cookiePrefKey] = cookie?.let(Json.Default::encodeToString).orEmpty()
    }

suspend fun DataStore<Preferences>.clear() =
    edit {
        it.remove(cookiePrefKey)
        it.remove(menuPrefKey)
        it.remove(filtersPrefKey)
    }

val DataStore<Preferences>.menu: Flow<JournalMenu?>
    get() = data
        .map { it[menuPrefKey] }
        .map { it?.let(Json.Default::decodeFromString) ?: return@map null }

suspend fun DataStore<Preferences>.setMenu(menu: JournalMenu?) =
    edit {
        it[menuPrefKey] = menu?.let(Json.Default::encodeToString).orEmpty()
    }

suspend fun DataStore<Preferences>.addFilter(f: JournalFilter) =
    edit {
        val x = setOf(Json.encodeToString(f))
        it[filtersPrefKey] = it[filtersPrefKey]?.plus(x) ?: x
    }

suspend fun DataStore<Preferences>.removeFilter(f: JournalFilter) =
    edit {
        val x = setOf(Json.encodeToString(f))
        it[filtersPrefKey] = it[filtersPrefKey]?.minus(x) ?: x
    }

suspend fun DataStore<Preferences>.clearFilters() =
    edit { it.remove(filtersPrefKey) }

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

        val receiver = ComponentName(this, BootCompleteReceiver::class.java)
        packageManager?.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        setContent {
            App()
        }
    }
}

@Composable
fun App() {
    val navController = rememberNavController()
    val dataStore = LocalContext.current.dataStore
    val cookie by dataStore.cookie
        .collectAsState(initial = null)
    UNNJournalCheckerTheme {
        NavHost(
            navController = navController,
            startDestination =
            if (cookie == null) Routes.Login.route
            else Routes.Filters.route,
        ) {
            composable(Routes.Login.route) {
                LoginPage(navController)
            }
            composable(Routes.Filters.route) {
                if (cookie == null) {
                    LoginPage(navController)
                } else {
                    FiltersPage(navController)
                }
            }
            composable(
                route = Routes.JournalWebPage.route,
                deepLinks = listOf(navDeepLink {
                    uriPattern = "$FILTER_URL{parameters}"
                })
            ) {
                val url = (it.arguments
                    ?.get("android-support-nav:controller:deepLinkIntent") as Intent?)
                    ?.data
                    ?.toString()
                if (url == null) {
                    FiltersPage(navController)
                } else {
                    JournalWebPage(navController, url)
                }
            }
        }
    }
}

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Filters : Routes("filters")
    object JournalWebPage : Routes("JournalWebPage")
}

