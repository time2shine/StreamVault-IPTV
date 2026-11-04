package com.streamvault.feature.playback.player

import com.streamvault.domain.model.Channel

internal fun Channel.matchesPlaybackTarget(
    targetChannelId: Long,
    currentStreamUrl: String,
    resolvedStreamUrl: String = "",
    remoteStreamId: Long? = null,
    title: String = ""
): Boolean {
    if (targetChannelId > 0L && (id == targetChannelId || allVariantRawIds().contains(targetChannelId))) {
        return true
    }
    if (currentStreamUrl.isNotBlank() && (streamUrl == currentStreamUrl ||
            variants.any { it.streamUrl == currentStreamUrl } ||
            alternativeStreams.contains(currentStreamUrl))) {
        return true
    }
    if (resolvedStreamUrl.isNotBlank() && (streamUrl == resolvedStreamUrl ||
            variants.any { it.streamUrl == resolvedStreamUrl } ||
            alternativeStreams.contains(resolvedStreamUrl))) {
        return true
    }
    if (remoteStreamId != null && remoteStreamId > 0L && (streamId == remoteStreamId ||
            variants.any { it.streamId == remoteStreamId })) {
        return true
    }
    if (title.isNotBlank() && (name.equals(title, ignoreCase = true) ||
            canonicalName.equals(title, ignoreCase = true))) {
        return true
    }
    return false
}

internal fun resolveLiveChannelIndex(
    channelList: List<Channel>,
    currentChannelIndex: Int,
    currentContentId: Long,
    currentStreamUrl: String,
    resolvedStreamUrl: String = "",
    remoteStreamId: Long? = null,
    title: String = ""
): Int {
    if (channelList.isEmpty()) return -1

    if (currentChannelIndex in channelList.indices &&
        channelList[currentChannelIndex].matchesPlaybackTarget(
            targetChannelId = currentContentId,
            currentStreamUrl = currentStreamUrl,
            resolvedStreamUrl = resolvedStreamUrl,
            remoteStreamId = remoteStreamId,
            title = title
        )
    ) {
        return currentChannelIndex
    }

    // 1. Direct ID match
    if (currentContentId > 0L) {
        val idIndex = channelList.indexOfFirst { it.id == currentContentId }
        if (idIndex != -1) return idIndex
    }

    // 2. Any variant raw ID match (critical for grouped channels)
    if (currentContentId > 0L) {
        val variantIndex = channelList.indexOfFirst { it.allVariantRawIds().contains(currentContentId) }
        if (variantIndex != -1) return variantIndex
    }

    // 3. Exact stream URL match
    if (currentStreamUrl.isNotBlank()) {
        val urlIndex = channelList.indexOfFirst { it.streamUrl == currentStreamUrl }
        if (urlIndex != -1) return urlIndex
    }

    // 4. Variant / alternative stream URL match
    if (currentStreamUrl.isNotBlank()) {
        val altUrlIndex = channelList.indexOfFirst { channel ->
            channel.variants.any { it.streamUrl == currentStreamUrl } ||
                channel.alternativeStreams.contains(currentStreamUrl)
        }
        if (altUrlIndex != -1) return altUrlIndex
    }

    // 5. Resolved playback URL match
    if (resolvedStreamUrl.isNotBlank()) {
        val resolvedIndex = channelList.indexOfFirst { channel ->
            channel.streamUrl == resolvedStreamUrl ||
                channel.variants.any { it.streamUrl == resolvedStreamUrl } ||
                channel.alternativeStreams.contains(resolvedStreamUrl)
        }
        if (resolvedIndex != -1) return resolvedIndex
    }

    // 6. Remote stream ID match
    if (remoteStreamId != null && remoteStreamId > 0L) {
        val remoteIndex = channelList.indexOfFirst { channel ->
            channel.streamId == remoteStreamId ||
                channel.variants.any { it.streamId == remoteStreamId }
        }
        if (remoteIndex != -1) return remoteIndex
    }

    // 7. Title / Canonical name fallback
    if (title.isNotBlank()) {
        val titleIndex = channelList.indexOfFirst { channel ->
            channel.name.equals(title, ignoreCase = true) ||
                channel.canonicalName.equals(title, ignoreCase = true)
        }
        if (titleIndex != -1) return titleIndex
    }

    return -1
}

internal fun computeWrappedChannelIndex(
    resolvedIndex: Int,
    channelCount: Int,
    offset: Int
): Int {
    if (resolvedIndex == -1 || channelCount <= 0) return -1
    return ((resolvedIndex + offset) % channelCount + channelCount) % channelCount
}