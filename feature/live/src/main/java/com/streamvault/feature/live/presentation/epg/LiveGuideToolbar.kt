package com.streamvault.feature.live.presentation.epg

import com.streamvault.core.ui.layout.isPortraitLayout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.streamvault.core.ui.interaction.TvClickableSurface
import com.streamvault.core.ui.design.BrowseColors

data class LiveGuideToolbarLabels(
    val jumpNow: String,
    val search: String,
    val options: String
)

@Composable
fun LiveGuideToolbarRow(
    selectedCategoryName: String,
    labels: LiveGuideToolbarLabels,
    onOpenCategoryPicker: () -> Unit,
    onJumpToNow: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenOptions: () -> Unit,
    onGuideInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = if (isPortraitLayout()) modifier.horizontalScroll(rememberScrollState()) else modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveGuideToolbarButton(
            label = selectedCategoryName,
            modifier = Modifier.widthIn(min = if (isPortraitLayout()) 120.dp else 200.dp, max = 280.dp),
            isCategory = true,
            onClick = onOpenCategoryPicker,
            onFocused = onGuideInteract
        )
        LiveGuideToolbarButton(label = labels.jumpNow, onClick = onJumpToNow, onFocused = onGuideInteract)
        LiveGuideToolbarButton(label = labels.search, onClick = onOpenSearch, onFocused = onGuideInteract)
        LiveGuideToolbarButton(label = labels.options, onClick = onOpenOptions, onFocused = onGuideInteract)
    }
}

@Composable
fun LiveGuideToolbarButton(
    label: String,
    modifier: Modifier = Modifier,
    isCategory: Boolean = false,
    onClick: () -> Unit,
    onFocused: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier.onFocusChanged {
            if (it.isFocused && !focused) onFocused()
            focused = it.isFocused
        },
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isCategory) Color(0xFF14243C) else Color(0xFF121B2A),
            focusedContainerColor = BrowseColors.BrandStrong,
            contentColor = if (isCategory) BrowseColors.TextPrimary else BrowseColors.TextSecondary,
            focusedContentColor = Color.White
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, if (isCategory) BrowseColors.Brand.copy(alpha = 0.4f) else BrowseColors.Outline.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, BrowseColors.Focus),
                shape = RoundedCornerShape(16.dp)
            )
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.sp,
            fontWeight = if (isCategory || focused) FontWeight.SemiBold else FontWeight.Medium,
            color = if (focused) Color.White else if (isCategory) BrowseColors.TextPrimary else BrowseColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
