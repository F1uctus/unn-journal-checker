package com.f1uctus.unnjournalchecker.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.f1uctus.unnjournalchecker.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalWebPage(
    navController: NavHostController,
    url: String,
) {
    val dataStore = LocalContext.current.dataStore
    val cookie by dataStore.cookie.collectAsState(initial = null)
    if (cookie == null) return
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("journal.unn.ru") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    }
                }

            )
        },
        content = { contentPadding ->
            Box(modifier = Modifier.padding(contentPadding)) {
                JournalWebView(url, cookie!!)
            }
        }
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun JournalWebView(
    url: String,
    cookieAuth: CookieAuth,
) {
    AndroidView(factory = {
        WebView(it).apply {
            settings.javaScriptEnabled = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = WebViewClient()
            loadUrl(
                url, mapOf(
                    "Cookie" to cookieAuth.toCookieHeaderString()
                )
            )
        }
    }, update = {
        it.loadUrl(
            url, mapOf(
                "Cookie" to cookieAuth.toCookieHeaderString()
            )
        )
    })
}
