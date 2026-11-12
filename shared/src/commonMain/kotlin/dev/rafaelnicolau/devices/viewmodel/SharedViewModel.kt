package dev.rafaelnicolau.devices.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel

/**
 * Base for ViewModels shared by Android and iOS. Android clears it through the
 * ViewModelStore; iOS has no such owner, so the Swift side calls [clear] itself.
 */
abstract class SharedViewModel : ViewModel() {
    fun clear() {
        viewModelScope.cancel()
    }
}
