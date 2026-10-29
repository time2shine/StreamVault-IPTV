package com.streamvault.feature.playback.player.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import com.streamvault.core.ui.components.shell.StatusPill
import com.streamvault.core.ui.design.BrowseColors as AppColors
import com.streamvault.core.ui.design.BrowseColors.Brand as Primary
import com.streamvault.core.ui.design.BrowseShapes
import com.streamvault.core.ui.image.ChannelLogoBadge
import com.streamvault.core.ui.interaction.TvClickableSurface
import com.streamvault.core.ui.layout.isPortraitLayout
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Program
import com.streamvault.feature.playback.R

@Composable
fun PlayerQuickChannelSwitcherOverlay(
    channels: List<Channel>,
    currentChannelId: Long,
    currentProgram: Program?,
    overlayFocusRequester: FocusRequester = remember { FocusRequester() },
    categoryName: String? = null,
    onSelectChannel: (Long) -> Unit,
    onOpenFullChannelList: () -> Unit,
    onOpenEpg: () -> Unit,
    onDismiss: () -> Unit,
    onOverlayInteracted: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val currentIndex = remember(channels, currentChannelId) {
        channels.indexOfFirst { it.id == currentChannelId }.coerceAtLeast(0)
    }

    LaunchedEffect(channels, currentIndex) {
        if (channels.isNotEmpty() && currentIndex in channels.indices) {
            listState.scrollToItem(maxOf(0, currentIndex - 1))
        }
    }

    LaunchedEffect(Unit) {
        overlayFocusRequester.requestFocus()
    }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isPortraitLayout()) 12.dp else 36.dp,
                vertical = if (isPortraitLayout()) 12.dp else 24.dp
            )
    ) {
        PlayerOverlayPanel(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header: Title, Category Name, Channel Count, and Quick Action Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = stringResource(R.string.player_quick_channel_switcher_title),
                        containerColor = AppColors.BrandMuted
                    )
                    if (!categoryName.isNullOrBlank()) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.titleSmall,
                            color = AppColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (channels.isNotEmpty()) {
                        Text(
                            text = "(${channels.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextTertiary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvClickableSurface(
                        onClick = {
                            onOverlayInteracted()
                            onOpenFullChannelList()
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = AppColors.SurfaceEmphasis,
                            focusedContainerColor = Primary
                        ),
                        modifier = Modifier.onFocusChanged {
                            if (it.isFocused) onOverlayInteracted()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.player_quick_channel_switcher_open_list),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    TvClickableSurface(
                        onClick = {
                            onOverlayInteracted()
                            onOpenEpg()
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = AppColors.SurfaceEmphasis,
                            focusedContainerColor = Primary
                        ),
                        modifier = Modifier.onFocusChanged {
                            if (it.isFocused) onOverlayInteracted()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.player_quick_channel_switcher_open_epg),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Carousel of Channel Cards
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(
                    channels,
                    key = { index, channel -> "quick_channel:${channel.id}:${channel.streamId}:$index" },
                    contentType = { _, _ -> "quick_channel_card" }
                ) { index, channel ->
                    val isCurrent = channel.id == currentChannelId

                    TvClickableSurface(
                        onClick = {
                            onOverlayInteracted()
                            onSelectChannel(channel.id)
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isCurrent) AppColors.SurfaceEmphasis else AppColors.Surface.copy(alpha = 0.5f),
                            focusedContainerColor = AppColors.SurfaceEmphasis
                        ),
                        border = ClickableSurfaceDefaults.border(
                            border = Border(
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) Primary.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
                                )
                            ),
                            focusedBorder = Border(
                                border = androidx.compose.foundation.BorderStroke(2.dp, Primary)
                            )
                        ),
                        modifier = Modifier
                            .width(210.dp)
                            .then(
                                if (isCurrent) Modifier.focusRequester(overlayFocusRequester)
                                else Modifier
                            )
                            .onFocusChanged {
                                if (it.isFocused) onOverlayInteracted()
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Top row: channel number & live badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val channelNumber = channel.number.takeIf { it > 0 } ?: (index + 1)
                                Text(
                                    text = "#$channelNumber",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Primary else AppColors.TextSecondary
                                )

                                if (isCurrent) {
                                    StatusPill(
                                        label = stringResource(R.string.player_quick_channel_switcher_now_playing),
                                        containerColor = AppColors.Live
                                    )
                                }
                            }

                            // Middle row: Channel Logo + Channel Name & Program
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(BrowseShapes.Artwork)
                                        .border(1.dp, AppColors.Focus.copy(alpha = 0.08f), BrowseShapes.Artwork)
                                ) {
                                    ChannelLogoBadge(
                                        channelName = channel.name,
                                        logoUrl = channel.logoUrl,
                                        backgroundColor = AppColors.SurfaceEmphasis.copy(alpha = 0.46f),
                                        contentPadding = PaddingValues(4.dp),
                                        textStyle = MaterialTheme.typography.labelMedium,
                                        textColor = AppColors.TextSecondary,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val programLabel = if (isCurrent && currentProgram != null) {
                                        currentProgram.title
                                    } else {
                                        null
                                    }

                                    if (!programLabel.isNullOrBlank()) {
                                        Text(
                                            text = programLabel,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = AppColors.TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
