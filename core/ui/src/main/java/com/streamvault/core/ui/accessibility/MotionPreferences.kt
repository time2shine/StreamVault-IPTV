package com.streamvault.core.ui.accessibility

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun isReducedMotionEnabled(context: Context): Boolean {
    val animatorScale = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    return animatorScale == 0f
}

/**
 * Process-wide singleton tracker for the system reduced motion / animator duration setting.
 *
 * Avoids registering dozens of ContentObserver IPC binder handles per movie card during
 * vertical scrolling in lazy lists.
 */
object ReducedMotionTracker {
    @Volatile
    private var isInitialized = false
    private val _enabled = MutableStateFlow(false)
    val enabledFlow: StateFlow<Boolean> = _enabled.asStateFlow()

    fun get(context: Context): Boolean {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    val appContext = context.applicationContext
                    _enabled.value = isReducedMotionEnabled(appContext)
                    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            _enabled.value = isReducedMotionEnabled(appContext)
                        }
                    }
                    val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
                    runCatching {
                        appContext.contentResolver.registerContentObserver(uri, false, observer)
                    }
                    isInitialized = true
                }
            }
        }
        return _enabled.value
    }
}

@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    val initial = ReducedMotionTracker.get(context)
    val state by ReducedMotionTracker.enabledFlow.collectAsState(initial = initial)
    return state
}
