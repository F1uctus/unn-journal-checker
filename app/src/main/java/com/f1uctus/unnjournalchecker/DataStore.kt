package com.f1uctus.unnjournalchecker

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration

// region Cookie ////////////////

private val cookiePrefKey = stringPreferencesKey("cookie")

val DataStore<Preferences>.cookie: Flow<CookieAuth?>
    get() = data
        .map { it[cookiePrefKey] }
        .map { it?.let(Json.Default::decodeFromString) }

suspend fun DataStore<Preferences>.setCookie(value: CookieAuth?) =
    edit {
        it[cookiePrefKey] = value?.let(Json.Default::encodeToString).orEmpty()
    }

// endregion ////////////////////

// region Menu //////////////////

private val menuPrefKey = stringPreferencesKey("menu")

val DataStore<Preferences>.menu: Flow<JournalMenu?>
    get() = data
        .map { it[menuPrefKey] }
        .map { it?.let(Json.Default::decodeFromString) ?: return@map null }

suspend fun DataStore<Preferences>.setMenu(value: JournalMenu?) =
    edit {
        it[menuPrefKey] = value?.let(Json.Default::encodeToString).orEmpty()
    }

// endregion ////////////////////

// region Filters ///////////////

private val filtersPrefKey = stringSetPreferencesKey("filters")

val DataStore<Preferences>.filters: Flow<List<JournalFilter>>
    get() = data
        .map { it[filtersPrefKey] }
        .map {
            it?.map(Json.Default::decodeFromString) ?: listOf()
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

// endregion ////////////////////

// region Journal check interval

val defaultSectionCheckInterval = Duration.ofMinutes(10)!!
private val sectionCheckIntervalPrefKey = longPreferencesKey("sectionCheckInterval")

val DataStore<Preferences>.sectionCheckInterval: Flow<Duration>
    get() = data
        .map { it[sectionCheckIntervalPrefKey] }
        .map { it?.let(Duration::ofMinutes) ?: defaultSectionCheckInterval }

suspend fun DataStore<Preferences>.setSectionCheckInterval(value: Duration?) =
    edit {
        it[sectionCheckIntervalPrefKey] = (value ?: defaultSectionCheckInterval).toMinutes()
    }

// endregion ////////////////////

suspend fun DataStore<Preferences>.clear() =
    edit {
        it.remove(cookiePrefKey)
        it.remove(menuPrefKey)
        it.remove(filtersPrefKey)
        it.remove(sectionCheckIntervalPrefKey)
    }
