package com.example.bottomapp.backend.exceptions

import android.content.Context
import android.widget.Toast

public class ExceptionsCatcher {
    companion object {
        fun makeToastByThrowable(context: Context, throwable: Throwable) {
            Toast.makeText(
                context,
                throwable.message ?: throwable.toString(),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}