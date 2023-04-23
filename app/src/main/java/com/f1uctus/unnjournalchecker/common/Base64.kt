package com.f1uctus.unnjournalchecker.common

import android.util.Base64

fun String.fromBase64(): String {
    return String(Base64.decode(this, Base64.NO_WRAP))
}

fun String.toBase64(): String {
    return Base64.encodeToString(toByteArray(), Base64.NO_WRAP)
}
