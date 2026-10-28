package com.streamvault.feature.catalog.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.streamvault.core.ui.design.FocusRestoreHost
import com.streamvault.core.ui.design.browse.OnSurfaceDim
import com.streamvault.core.ui.design.browse.Primary
import com.streamvault.core.ui.design.browse.SurfaceElevated
import com.streamvault.core.ui.design.browse.TextPrimary
import com.streamvault.core.ui.design.requestFocusSafely
import com.streamvault.core.ui.interaction.TvClickableSurface
import com.streamvault.domain.model.Category
import com.streamvault.domain.model.Channel
import com.streamvault.feature.catalog.R
import com.streamvault.feature.catalog.presentation.components.ChannelCard
import com.streamvault.feature.catalog.presentation.components.LiveCategoryCard
import com.streamvault.feature.catalog.presentation.components.resolveCategoryIcon
import kotlinx.coroutines.delay

@Composable
fun HomeCategoryGridView(
    categories: List<Category>,
    totalChannelCount: Int,
    isPortrait: Boolean,
    providerWarnings: List<String>,
    onOpenSettings: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val columns = if (isPortrait) GridCells.Fixed(2) else GridCells.Adaptive(minSize = 220.dp)
    val focusRequesters = remember { mutableMapOf<Long, FocusRequester>() }
    val firstCategoryFocusRequester = remember { FocusRequester() }

    val focusFirstCategory: suspend () -> Unit = {
        if (categories.isNotEmpty()) {
            if (gridState.firstVisibleItemIndex > 0) {
                runCatching { gridState.scrollToItem(0) }
            }
            for (attempt in 0..5) {
                delay(80)
                if (firstCategoryFocusRequester.requestFocusSafely(tag = "HomeCategoryGrid", target = "First category")) {
                    break
                }
            }
        }
    }

    FocusRestoreHost(
        enabled = categories.isNotEmpty(),
        onRestore = {
            focusFirstCategory()
        }
    ) {
        LaunchedEffect(categories.firstOrNull()?.id) {
            focusFirstCategory()
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = if (isPortrait) 16.dp else 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (providerWarnings.isNotEmpty()) {
                    DashboardProviderWarningCard(
                        warnings = providerWarnings,
                        onOpenSettings = onOpenSettings
                    )
                }

                if (categories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = columns,
                        state = gridState,
                        contentPadding = PaddingValues(top = 10.dp, bottom = 52.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = categories,
                            key = { _, category -> category.id }
                        ) { index, category ->
                            val requester = if (index == 0) {
                                firstCategoryFocusRequester
                            } else {
                                focusRequesters.getOrPut(category.id) { FocusRequester() }
                            }
                            LiveCategoryCard(
                                category = category,
                                isLocked = false,
                                isPinned = false,
                                isPortrait = isPortrait,
                                focusRequester = requester,
                                onClick = { onCategoryClick(category) }
                            )
                        }
                    }
                }
            }

            if (categories.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_category_summary,
                            totalChannelCount,
                            categories.size
                        ),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isPortrait) 12.sp else 13.sp
                        ),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeCategoryChannelListView(
    category: Category,
    channels: List<Channel>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    nowMs: Long,
    recordingChannelIds: Set<Long>,
    scheduledChannelIds: Set<Long>,
    isPortrait: Boolean,
    onBack: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val columns = if (isPortrait) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 220.dp)
    val categoryIcon = remember(category) { resolveCategoryIcon(category) }
    val firstChannelFocusRequester = remember { FocusRequester() }

    val focusFirstChannel: suspend () -> Unit = {
        if (channels.isNotEmpty()) {
            if (gridState.firstVisibleItemIndex > 0) {
                runCatching { gridState.scrollToItem(0) }
            }
            for (attempt in 0..5) {
                delay(80)
                if (firstChannelFocusRequester.requestFocusSafely(tag = "HomeCategoryChannelList", target = "First channel")) {
                    break
                }
            }
        }
    }

    FocusRestoreHost(
        enabled = channels.isNotEmpty(),
        onRestore = {
            focusFirstChannel()
        }
    ) {
        LaunchedEffect(channels.firstOrNull()?.id) {
            focusFirstChannel()
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = if (isPortrait) 16.dp else 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    TvClickableSurface(
                        onClick = onBack,
                        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                            containerColor = SurfaceElevated,
                            focusedContainerColor = Primary
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.home_back_to_categories),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = categoryIcon,
                        fontSize = 22.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isPortrait) 18.sp else 22.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isPortrait) "${channels.size}" else stringResource(R.string.home_category_channel_count, channels.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceDim
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            stringResource(R.string.vod_category_picker_search_placeholder),
                            fontSize = 12.sp,
                            color = OnSurfaceDim
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = OnSurfaceDim,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    tint = OnSurfaceDim,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .width(if (isPortrait) 150.dp else 220.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated.copy(alpha = 0.5f),
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (channels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_no_channels_in_category),
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceDim
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = columns,
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = channels,
                        key = { _, channel -> channel.id }
                    ) { index, channel ->
                        ChannelCard(
                            channel = channel,
                            nowMs = nowMs,
                            isRecording = channel.id in recordingChannelIds,
                            isScheduledRecording = channel.id in scheduledChannelIds,
                            onClick = { onChannelClick(channel) },
                            modifier = if (index == 0) Modifier.focusRequester(firstChannelFocusRequester) else Modifier
                        )
                    }
                }
            }
        }
    }
}
