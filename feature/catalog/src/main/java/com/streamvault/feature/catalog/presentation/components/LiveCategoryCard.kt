package com.streamvault.feature.catalog.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.streamvault.core.ui.design.browse.FocusBorder
import com.streamvault.core.ui.interaction.TvClickableSurface
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.VirtualCategoryIds
import com.streamvault.feature.catalog.R
import java.util.Locale

fun resolveCategoryIcon(category: Category): String {
    if (category.isVirtual) {
        return when (category.id) {
            VirtualCategoryIds.FAVORITES -> "⭐"
            VirtualCategoryIds.RECENT -> "🕒"
            else -> "📺"
        }
    }
    val lower = category.name.lowercase(Locale.ROOT)
    return when {
        "sport" in lower || "cricket" in lower || "football" in lower || "fifa" in lower || "t sports" in lower || "sony ten" in lower -> "⚽"
        "bangla" in lower || "bd" in lower -> "🇧🇩"
        "news" in lower || "khabar" in lower || "somoy" in lower || "bbc" in lower || "cnn" in lower -> "📰"
        "movie" in lower || "cinema" in lower || "film" in lower || "hbo" in lower -> "🎬"
        "hindi" in lower || "drama" in lower || "serial" in lower || "entert" in lower || "natok" in lower || "star plus" in lower || "zee" in lower -> "🎭"
        "kid" in lower || "cartoon" in lower || "disney" in lower || "anim" in lower || "pogo" in lower || "nick" in lower -> "👶"
        "docu" in lower || "discovery" in lower || "nat geo" in lower || "wild" in lower || "animal" in lower || "history" in lower -> "🦁"
        "music" in lower || "song" in lower || "gaan" in lower || "mtv" in lower -> "🎵"
        "islam" in lower || "quran" in lower || "relig" in lower || "peace" in lower -> "🕌"
        "info" in lower || "know" in lower || "learn" in lower || "science" in lower -> "📚"
        "cook" in lower || "food" in lower || "recipe" in lower || "life" in lower -> "🍳"
        "comedy" in lower || "fun" in lower -> "😂"
        "all" in lower -> "📺"
        else -> "📺"
    }
}

fun resolveCategoryGradient(category: Category): Pair<Color, Color> {
    if (category.isVirtual && category.id == VirtualCategoryIds.FAVORITES) {
        return Color(0xFFB45309).copy(alpha = 0.50f) to Color(0xFF451A03).copy(alpha = 0.75f)
    }
    if (category.isVirtual && category.id == VirtualCategoryIds.RECENT) {
        return Color(0xFF0369A1).copy(alpha = 0.45f) to Color(0xFF082F49).copy(alpha = 0.75f)
    }
    val lower = category.name.lowercase(Locale.ROOT)
    return when {
        "sport" in lower || "cricket" in lower || "football" in lower -> Color(0xFF047857).copy(alpha = 0.45f) to Color(0xFF064E3B).copy(alpha = 0.80f)
        "bangla" in lower || "bd" in lower -> Color(0xFF15803D).copy(alpha = 0.45f) to Color(0xFF7F1D1D).copy(alpha = 0.75f)
        "news" in lower -> Color(0xFF1D4ED8).copy(alpha = 0.45f) to Color(0xFF1E3A8A).copy(alpha = 0.80f)
        "movie" in lower || "cinema" in lower -> Color(0xFFBE123C).copy(alpha = 0.45f) to Color(0xFF881337).copy(alpha = 0.80f)
        "hindi" in lower || "drama" in lower || "entert" in lower -> Color(0xFFB45309).copy(alpha = 0.45f) to Color(0xFF78350F).copy(alpha = 0.80f)
        "kid" in lower || "cartoon" in lower -> Color(0xFF0E7490).copy(alpha = 0.45f) to Color(0xFF164E63).copy(alpha = 0.80f)
        "docu" in lower || "wild" in lower -> Color(0xFF0F766E).copy(alpha = 0.45f) to Color(0xFF134E4A).copy(alpha = 0.80f)
        "music" in lower -> Color(0xFF7E22CE).copy(alpha = 0.45f) to Color(0xFF581C87).copy(alpha = 0.80f)
        "all" in lower -> Color(0xFF4338CA).copy(alpha = 0.45f) to Color(0xFF312E81).copy(alpha = 0.80f)
        else -> Color(0xFF334155).copy(alpha = 0.45f) to Color(0xFF0F172A).copy(alpha = 0.85f)
    }
}

@Composable
fun LiveCategoryCard(
    category: Category,
    isLocked: Boolean,
    isPinned: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onFocused: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {},
    isPortrait: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val (gradientStart, gradientEnd) = remember(category) { resolveCategoryGradient(category) }
    val icon = remember(category) { resolveCategoryIcon(category) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.04f else 1f, label = "card_scale")
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) FocusBorder else gradientStart.copy(alpha = 0.35f),
        label = "card_border"
    )

    TvClickableSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, borderColor)),
            focusedBorder = Border(BorderStroke(3.dp, FocusBorder))
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(136.dp)
            .scale(scale)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                onFocusChanged(state.isFocused)
                if (state.isFocused) onFocused()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (isFocused) gradientStart.copy(alpha = 0.70f) else gradientStart,
                            if (isFocused) gradientEnd.copy(alpha = 0.90f) else gradientEnd
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = icon,
                            fontSize = 20.sp
                        )
                    }

                    if (category.count > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.Black.copy(alpha = 0.40f))
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.home_category_channel_count, category.count),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = Color.White.copy(alpha = 0.95f)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isPortrait) 18.sp else 21.sp,
                                lineHeight = if (isPortrait) 22.sp else 25.sp
                            ),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPinned) {
                            Text(text = "📌", fontSize = 13.sp)
                        }
                        if (isLocked) {
                            Text(text = "🔒", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
