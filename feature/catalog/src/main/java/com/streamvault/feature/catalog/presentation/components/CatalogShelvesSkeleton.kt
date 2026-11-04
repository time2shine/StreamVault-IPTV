package com.streamvault.feature.catalog.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.streamvault.core.ui.accessibility.rememberReducedMotionEnabled
import com.streamvault.core.ui.design.browse.Primary
import com.streamvault.core.ui.design.browse.SurfaceHighlight
import com.streamvault.core.ui.design.browse.TextSecondary

@Composable
fun CatalogShelvesSkeleton(
    statusTitle: String,
    modifier: Modifier = Modifier,
    isSyncing: Boolean = false,
    cardWidth: Int = 136,
    cardHeight: Int = 204
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val infiniteTransition = rememberInfiniteTransition(label = "skeletonPulse")
    val alpha by if (reducedMotion) {
        remember { mutableFloatStateOf(0.10f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.06f,
            targetValue = 0.16f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "skeletonAlpha"
        )
    }

    val placeholderColor = SurfaceHighlight.copy(alpha = alpha)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 48.dp)
    ) {
        // Status header banner
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceHighlight.copy(alpha = 0.4f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Primary,
                strokeWidth = 2.dp
            )
            Text(
                text = statusTitle,
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            userScrollEnabled = false
        ) {
            items(3) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category title bar placeholder
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .width(140.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(placeholderColor)
                    )

                    // Row of skeleton cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        repeat(6) {
                            Box(
                                modifier = Modifier
                                    .width(cardWidth.dp)
                                    .height(cardHeight.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(placeholderColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
