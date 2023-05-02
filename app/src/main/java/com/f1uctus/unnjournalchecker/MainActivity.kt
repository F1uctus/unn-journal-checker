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
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.*
import androidx.navigation.navDeepLink
import com.f1uctus.unnjournalchecker.JournalScraper.FILTER_URL
import com.f1uctus.unnjournalchecker.common.notificationManager
import com.f1uctus.unnjournalchecker.ui.*
import com.f1uctus.unnjournalchecker.ui.theme.UNNJournalCheckerTheme
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "settings")

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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val dataStore = context.dataStore
    val cookie by dataStore.cookie.collectAsState(initial = null)
    val firstRun by dataStore.firstRun.collectAsState(initial = false)

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
            composable(Routes.Settings.route) {
                SettingsPage(navController)
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

    if (firstRun) {
        ExcludeFromPowerSavingDialog {
            scope.launch {
                dataStore.setFirstRun(false)
            }
        }
    }
}

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object Filters : Routes("filters")
    object Settings : Routes("settings")
    object JournalWebPage : Routes("JournalWebPage")
}
