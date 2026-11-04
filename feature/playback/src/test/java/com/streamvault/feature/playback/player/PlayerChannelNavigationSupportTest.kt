package com.streamvault.feature.playback.player

import com.google.common.truth.Truth.assertThat
import com.streamvault.domain.model.Channel
import com.streamvault.domain.model.LiveChannelVariant
import org.junit.Test

class PlayerChannelNavigationSupportTest {

    @Test
    fun resolveLiveChannelIndex_matchesByDirectId() {
        val channel1 = Channel(id = 100L, name = "Star Movies SD", streamUrl = "http://example.com/100.ts")
        val channel2 = Channel(id = 200L, name = "Star Movies HD", streamUrl = "http://example.com/200.ts")

        val index = resolveLiveChannelIndex(
            channelList = listOf(channel1, channel2),
            currentChannelIndex = -1,
            currentContentId = 200L,
            currentStreamUrl = "http://example.com/200.ts"
        )

        assertThat(index).isEqualTo(1)
    }

    @Test
    fun resolveLiveChannelIndex_matchesByVariantRawIdWhenGroupedChannelIdDiffers() {
        // Grouped channel has selectedVariantId / id = 100L, but contains variant 200L (the one actively playing)
        val variantSd = LiveChannelVariant(
            rawChannelId = 100L,
            logicalGroupId = "grp_star_movies",
            providerId = 1L,
            originalName = "Star Movies SD",
            canonicalName = "Star Movies",
            streamUrl = "http://example.com/100.ts"
        )
        val variantHd = LiveChannelVariant(
            rawChannelId = 200L,
            logicalGroupId = "grp_star_movies",
            providerId = 1L,
            originalName = "Star Movies HD",
            canonicalName = "Star Movies",
            streamUrl = "http://example.com/200.ts"
        )
        val groupedChannel = Channel(
            id = 100L,
            name = "Star Movies",
            streamUrl = "http://example.com/100.ts",
            variants = listOf(variantSd, variantHd),
            selectedVariantId = 100L
        )

        // When actively playing variant 200L, resolveLiveChannelIndex should find the grouped channel!
        val index = resolveLiveChannelIndex(
            channelList = listOf(groupedChannel),
            currentChannelIndex = -1,
            currentContentId = 200L,
            currentStreamUrl = "http://example.com/200.ts"
        )

        assertThat(index).isEqualTo(0)
    }

    @Test
    fun resolveLiveChannelIndex_matchesByResolvedPlaybackUrl() {
        val channel = Channel(
            id = 100L,
            name = "HBO HD",
            streamUrl = "http://example.com/live/100.m3u8"
        )

        val index = resolveLiveChannelIndex(
            channelList = listOf(channel),
            currentChannelIndex = -1,
            currentContentId = -1L,
            currentStreamUrl = "http://different.com/live.m3u8",
            resolvedStreamUrl = "http://example.com/live/100.m3u8"
        )

        assertThat(index).isEqualTo(0)
    }

    @Test
    fun resolveLiveChannelIndex_matchesByRemoteStreamId() {
        val channel = Channel(
            id = 100L,
            name = "Cinema HD",
            streamId = 55443L,
            streamUrl = "http://example.com/55443.ts"
        )

        val index = resolveLiveChannelIndex(
            channelList = listOf(channel),
            currentChannelIndex = -1,
            currentContentId = -1L,
            currentStreamUrl = "",
            remoteStreamId = 55443L
        )

        assertThat(index).isEqualTo(0)
    }

    @Test
    fun resolveLiveChannelIndex_matchesByTitleWhenIdsDiffer() {
        val channel = Channel(
            id = 100L,
            name = "Star Movies HD",
            canonicalName = "Star Movies",
            streamUrl = "http://example.com/new_url.ts"
        )

        val index = resolveLiveChannelIndex(
            channelList = listOf(channel),
            currentChannelIndex = -1,
            currentContentId = 999L,
            currentStreamUrl = "http://old_url.ts",
            title = "Star Movies HD"
        )

        assertThat(index).isEqualTo(0)
    }

    @Test
    fun resolveLiveChannelIndex_preservesCurrentIndexIfAlreadyMatching() {
        val channel1 = Channel(id = 100L, name = "Channel 1", streamUrl = "http://1.ts")
        val channel2 = Channel(id = 200L, name = "Channel 2", streamUrl = "http://2.ts")

        val index = resolveLiveChannelIndex(
            channelList = listOf(channel1, channel2),
            currentChannelIndex = 1,
            currentContentId = 200L,
            currentStreamUrl = "http://2.ts"
        )

        assertThat(index).isEqualTo(1)
    }

    @Test
    fun computeWrappedChannelIndex_handlesOffsetsCorrectly() {
        assertThat(computeWrappedChannelIndex(resolvedIndex = 0, channelCount = 3, offset = 1)).isEqualTo(1)
        assertThat(computeWrappedChannelIndex(resolvedIndex = 2, channelCount = 3, offset = 1)).isEqualTo(0)
        assertThat(computeWrappedChannelIndex(resolvedIndex = 0, channelCount = 3, offset = -1)).isEqualTo(2)
        assertThat(computeWrappedChannelIndex(resolvedIndex = -1, channelCount = 3, offset = 1)).isEqualTo(-1)
        assertThat(computeWrappedChannelIndex(resolvedIndex = 0, channelCount = 0, offset = 1)).isEqualTo(-1)
    }
}
