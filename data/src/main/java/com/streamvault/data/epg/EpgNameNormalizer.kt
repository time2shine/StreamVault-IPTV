package com.streamvault.data.epg

import java.text.Normalizer
import java.util.Locale

/**
 * Normalizes channel display names for EPG matching.
 * Designed for comparing channel names across different EPG sources.
 *
 * This is intentionally simpler than [ChannelNormalizer.getLogicalGroupId]
 * — it does not strip quality tags or country prefixes because those may be
 * meaningful when matching EPG channels to provider channels.
 */
object EpgNameNormalizer {

    private val nonAlphanumericRegex = Regex("[^a-z0-9]")
    private val liveWrapperRegex = Regex("""^[\s\-–—]*live\s*\((.*?)\)[\s\-–—]*$""", RegexOption.IGNORE_CASE)
    private val categoryPrefixRegex = Regex("""^[A-Z0-9\s]{1,10}\|\s*""", RegexOption.IGNORE_CASE)

    /**
     * Strips enclosing Live wrapper or IPTV category prefixes from channel display names.
     * Examples:
     * - "---Live(Star Sports 1 HD)---" → "Star Sports 1 HD"
     * - "CR | Star Sports 1 Hindi" → "Star Sports 1 Hindi"
     */
    fun cleanChannelName(name: String): String {
        if (name.isBlank()) return ""
        var cleaned = name.trim()
        val liveMatch = liveWrapperRegex.find(cleaned)
        if (liveMatch != null) {
            cleaned = liveMatch.groupValues[1].trim()
        }
        val prefixMatch = categoryPrefixRegex.find(cleaned)
        if (prefixMatch != null) {
            cleaned = cleaned.substring(prefixMatch.range.last + 1).trim()
        }
        return cleaned
    }

    /**
     * Produces a normalized key from a channel display name.
     * - lowercase
     * - strip accents
     * - remove all non-alphanumeric characters
     *
     * Example: "BBC One HD" → "bbconehd"
     */
    fun normalize(name: String): String {
        if (name.isBlank()) return ""
        val lower = name.lowercase(Locale.ROOT)
        val stripped = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return stripped.replace(nonAlphanumericRegex, "")
    }
}
