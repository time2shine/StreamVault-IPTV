package com.streamvault.feature.catalog.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
            VirtualCategoryIds.RECENT -> "⏱️"
            else -> "⚡"
        }
    }
    val lower = category.name.lowercase(Locale.ROOT)
    return when {
        "cricket" in lower -> "🏏"
        "football" in lower || "fifa" in lower -> "⚽"
        "sport" in lower || "t sports" in lower || "sony ten" in lower -> "🏆"
        "bangla news" in lower || "somoy" in lower || "jamuna" in lower || "ekattor" in lower -> "🇧🇩"
        "bangla" in lower || "bd" in lower -> "🇧🇩"
        "international news" in lower || "world news" in lower || "bbc" in lower || "cnn" in lower -> "📰"
        "news" in lower || "khabar" in lower -> "📡"
        "movie" in lower || "cinema" in lower || "film" in lower || "hbo" in lower -> "🎬"
        "hindi" in lower || "drama" in lower || "serial" in lower || "entert" in lower || "natok" in lower || "star plus" in lower || "zee" in lower -> "🎭"
        "kid" in lower || "cartoon" in lower || "disney" in lower || "anim" in lower || "pogo" in lower || "nick" in lower -> "🚀"
        "docu" in lower || "discovery" in lower || "nat geo" in lower || "wild" in lower || "animal" in lower || "history" in lower -> "🌍"
        "music" in lower || "song" in lower || "gaan" in lower || "mtv" in lower -> "🎧"
        "india" in lower || "indian" in lower -> "🇮🇳"
        "pakistan" in lower || "pakistani" in lower -> "🇵🇰"
        "international" in lower || "world" in lower || "global" in lower -> "🌏"
        "travel" in lower || "tour" in lower || "trip" in lower || "holiday" in lower -> "✈️"
        "edu" in lower || "learn" in lower || "science" in lower || "know" in lower -> "🎓"
        "islam" in lower || "quran" in lower || "relig" in lower || "peace" in lower -> "🌙"
        "cook" in lower || "food" in lower || "recipe" in lower || "life" in lower -> "🍳"
        "comedy" in lower || "fun" in lower -> "🎭"
        "all" in lower -> "⚡"
        else -> "⚡"
    }
}

fun resolveCategoryGradient(category: Category): Pair<Color, Color> {
    if (category.isVirtual && category.id == VirtualCategoryIds.FAVORITES) {
        return Color(0xFFF59E0B) to Color(0xFFD97706)
    }
    if (category.isVirtual && category.id == VirtualCategoryIds.RECENT) {
        return Color(0xFF0EA5E9) to Color(0xFF0284C7)
    }
    val lower = category.name.lowercase(Locale.ROOT)
    return when {
        "sport" in lower || "cricket" in lower || "football" in lower -> Color(0xFF10B981) to Color(0xFF047857)
        "bangla news" in lower -> Color(0xFF2563EB) to Color(0xFF1D4ED8)
        "bangla" in lower || "bd" in lower -> Color(0xFF059669) to Color(0xFF991B1B)
        "news" in lower -> Color(0xFF2563EB) to Color(0xFF1D4ED8)
        "movie" in lower || "cinema" in lower -> Color(0xFFF43F5E) to Color(0xFF9F1239)
        "pakistan" in lower || "pakistani" in lower -> Color(0xFF047857) to Color(0xFF064E3B)
        "travel" in lower || "tour" in lower -> Color(0xFF0EA5E9) to Color(0xFF0369A1)
        "hindi" in lower || "drama" in lower || "entert" in lower -> Color(0xFFF59E0B) to Color(0xFFB45309)
        "kid" in lower || "cartoon" in lower -> Color(0xFF06B6D4) to Color(0xFF0284C7)
        "docu" in lower || "wild" in lower -> Color(0xFF14B8A6) to Color(0xFF0F766E)
        "music" in lower -> Color(0xFFA855F7) to Color(0xFF7E22CE)
        "india" in lower || "indian" in lower -> Color(0xFFF59E0B) to Color(0xFF059669)
        "international" in lower || "world" in lower -> Color(0xFF0EA5E9) to Color(0xFF4F46E5)
        "edu" in lower || "learn" in lower || "science" in lower -> Color(0xFF10B981) to Color(0xFF0D9488)
        "all" in lower -> Color(0xFF06B6D4) to Color(0xFF4F46E5)
        else -> Color(0xFF38BDF8) to Color(0xFF1E293B)
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
    val (accentColor, _) = remember(category) { resolveCategoryGradient(category) }
    val icon = remember(category) { resolveCategoryIcon(category) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_scale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) FocusBorder else accentColor.copy(alpha = 0.35f),
        label = "card_border"
    )

    TvClickableSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, borderColor)),
            focusedBorder = Border(BorderStroke(2.5.dp, FocusBorder))
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
                            accentColor.copy(alpha = if (isFocused) 0.28f else 0.16f),
                            Color(0xFF090E1A).copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            // Ambient corner glow flare
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (isFocused) 0.35f else 0.12f),
                                Color.Transparent
                            ),
                            radius = 320f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Icon + LIVE Pill + Channel Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cyberpunk icon container
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(accentColor.copy(alpha = if (isFocused) 0.30f else 0.18f))
                                .border(1.dp, accentColor.copy(alpha = if (isFocused) 0.55f else 0.30f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = icon,
                                fontSize = 20.sp
                            )
                        }

                        // LIVE micro-pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.20f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.40f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 7.dp, vertical = 2.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Text(
                                text = stringResource(R.string.card_live_badge),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFF34D399)
                            )
                        }
                    }

                    if (category.count > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                .padding(
                                    horizontal = if (isPortrait) 7.dp else 9.dp,
                                    vertical = 3.dp
                                )
                        ) {
                            val countText = if (isPortrait) {
                                "${category.count}"
                            } else {
                                stringResource(R.string.home_category_channel_count, category.count)
                            }
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isPortrait) 10.sp else 11.sp
                                ),
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                // Bottom Content: Category Name + Arrow + Accent Console Line
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = if (isPortrait) 18.sp else 21.sp,
                                    lineHeight = if (isPortrait) 22.sp else 25.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isPinned) {
                                Text(text = "📌", fontSize = 13.sp)
                            }
                            if (isLocked) {
                                Text(text = "🔒", fontSize = 13.sp)
                            }
                        }

                        if (isFocused) {
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = FocusBorder
                            )
                        }
                    }

                    // Bottom ambient console accent line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        accentColor.copy(alpha = if (isFocused) 0.95f else 0.50f),
                                        FocusBorder.copy(alpha = if (isFocused) 0.85f else 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
