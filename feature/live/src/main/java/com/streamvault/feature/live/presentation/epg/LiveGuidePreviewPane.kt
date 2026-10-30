package com.streamvault.feature.live.presentation.epg

import com.streamvault.core.ui.layout.isPortraitLayout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.streamvault.core.ui.design.BrowseColors
import com.streamvault.core.ui.design.BrowseShapes
import com.streamvault.core.ui.image.ChannelLogoBadge
import com.streamvault.core.ui.design.browse.Primary
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.Program
import com.streamvault.feature.live.presentation.time.LocalLiveTimeFormat
import com.streamvault.feature.live.presentation.time.createLiveTimeFormat
import com.streamvault.player.PlayerEngine
import com.streamvault.player.PlayerRenderSurfaceType
import com.streamvault.player.PlayerSurfaceResizeMode
import com.streamvault.player.ui.PlayerRenderView
import java.util.Date

data class LiveGuidePreviewLabels(
    val title: String,
    val placeholderTitle: String,
    val noSchedule: String
)

@Composable
fun LiveGuidePreviewPane(
    previewPlayerEngine: PlayerEngine?,
    isPreviewLoading: Boolean,
    focusedChannel: Channel?,
    focusedProgram: Program?,
    labels: LiveGuidePreviewLabels,
    modifier: Modifier = Modifier
) {
    val renderSurfaceType by (previewPlayerEngine?.renderSurfaceType)?.collectAsStateWithLifecycle(
        initialValue = PlayerRenderSurfaceType.SURFACE_VIEW
    ) ?: remember { mutableStateOf(PlayerRenderSurfaceType.SURFACE_VIEW) }
    val now = currentLiveGuideNow()
    val appTimeFormat = LocalLiveTimeFormat.current
    val timeFormat = remember(appTimeFormat) { appTimeFormat.createLiveTimeFormat() }

    Surface(
        modifier = modifier.height(152.dp),
        colors = SurfaceDefaults.colors(containerColor = Color(0xFF101928)),
        shape = BrowseShapes.Panel,
        border = Border(BorderStroke(1.dp, BrowseColors.Outline.copy(alpha = 0.5f)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color(0xFF132238), Color(0xFF0C1320))))
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .then(if (isPortraitLayout()) Modifier.width(120.dp) else Modifier.fillMaxHeight())
                    .aspectRatio(16f / 9f)
                    .clip(BrowseShapes.Artwork)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF0D1624), Color(0xFF080D15))),
                        BrowseShapes.Artwork
                    )
                    .border(1.dp, Color(0xFF1F304B), BrowseShapes.Artwork),
                contentAlignment = Alignment.Center
            ) {
                if (previewPlayerEngine != null) {
                    PlayerRenderView(
                        playerEngine = previewPlayerEngine,
                        resizeMode = PlayerSurfaceResizeMode.FIT,
                        surfaceType = renderSurfaceType,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isPreviewLoading) {
                        CircularProgressIndicator(
                            color = BrowseColors.Brand,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = BrowseColors.Brand.copy(alpha = 0.6f),
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = labels.placeholderTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            color = BrowseColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            val channel = focusedChannel
            val program = focusedProgram
            if (channel != null) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelLogoBadge(
                            channelName = channel.name,
                            logoUrl = channel.logoUrl,
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Text(
                            text = if (channel.number > 0) "${channel.number}. ${channel.name}" else channel.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrowseColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (program != null) {
                        val isLive = now in program.startTime until program.endTime
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isLive) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE50914).copy(alpha = 0.22f))
                                        .border(1.dp, Color(0xFFE50914).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE50914))
                                    )
                                    Text(
                                        text = "LIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Text(
                                text = program.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrowseColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${timeFormat.format(Date(program.startTime))} – ${timeFormat.format(Date(program.endTime))}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = BrowseColors.TextSecondary
                        )
                        if (isLive) {
                            LinearProgressIndicator(
                                progress = { ((now - program.startTime).toFloat() / (program.endTime - program.startTime).toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth(0.65f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp)),
                                color = BrowseColors.BrandStrong,
                                trackColor = Color(0xFF17263C)
                            )
                        }
                        if (program.description.isNotBlank()) {
                            Text(
                                text = program.description,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = BrowseColors.TextTertiary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = labels.noSchedule,
                            style = MaterialTheme.typography.bodySmall,
                            color = BrowseColors.TextSecondary
                        )
                    }
                }
            } else {
                Text(
                    text = labels.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = BrowseColors.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
