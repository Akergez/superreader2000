package com.example.bottomapp.shared.ui.state

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Base ViewModel for multiplatform support
 * This replaces Android's androidx.lifecycle.ViewModel
 */
open class BaseViewModel : ViewModel() {
    override fun onCleared() {
        super.onCleared()
    }
}

/**
 * Multiplatform-compatible state holder
 */
interface IStateHolder<T> {
    val state: StateFlow<T>
}

