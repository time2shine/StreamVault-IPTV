package com.streamvault.core.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.streamvault.core.ui.accessibility.rememberReducedMotionEnabled
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun rememberCrossfadeImageModel(data: Any?): Any? {
    val context = LocalContext.current
    val reducedMotionEnabled = rememberReducedMotionEnabled()
    return remember(context, data, reducedMotionEnabled) {
        val modelData = when {
            data is String && data.startsWith("local_res://") -> {
                val resName = data.removePrefix("local_res://")
                val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                if (resId != 0) resId else null
            }
            else -> data
        }
        modelData?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(!reducedMotionEnabled)
                .build()
        }
    }
}
