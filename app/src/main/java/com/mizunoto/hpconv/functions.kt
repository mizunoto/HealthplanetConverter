package com.mizunoto.hpconv

import android.content.Context
import android.widget.Toast

enum class ToastLength {
    SHORT,
    LONG
}

fun ToastShow(
    context: Context,
    text: String,
    length: ToastLength
) {
    val toastLength: Int =
        if (length == ToastLength.SHORT) {
            Toast.LENGTH_SHORT
        } else {
            Toast.LENGTH_LONG
        }
    Toast.makeText(context, text, toastLength).show()
}
