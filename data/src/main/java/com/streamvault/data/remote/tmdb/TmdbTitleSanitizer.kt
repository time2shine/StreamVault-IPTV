package com.streamvault.data.remote.tmdb

data class TmdbCleanTitle(
    val cleanTitle: String,
    val year: Int? = null
)

object TmdbTitleSanitizer {

    // Emojis, pictographs, symbols (e.g. 🆕 U+1F195, 🔥 U+1F525, 🎬 U+1F3AC, etc.)
    private val EMOJI_AND_SYMBOLS_REGEX = Regex(
        """[\p{So}\p{Sk}\uFE0F\u200D]+"""
    )

    // Standard pattern: Title (Year) [trailing tags like "new 🆕", "[4K]", etc.]
    // Uses non-greedy .*? for the title part so it stops at the (Year).
    private val PARENTHESIZED_YEAR_REGEX = Regex(
        """^(.*?)\s*[\(\[]\s*((?:19|20)\d{2})\s*[\)\]](.*)$""",
        RegexOption.IGNORE_CASE
    )

    // Standalone 4-digit year pattern if parentheses were not used (e.g. "Movie 2022")
    private val STANDALONE_YEAR_REGEX = Regex(
        """\b((?:19|20)\d{2})\b"""
    )

    // Country/language prefix tags like "EN |", "US |", "[EN]", "[FR]", or promotional prefixes "NEW |"
    private val PREFIX_TAGS_REGEX = Regex(
        """^(?:\[[a-zA-Z]{2,3}\]|(?:[a-zA-Z]{2,3}\s*\|)|\b(?:NEW|HOT)\b\s*[:|-])\s*""",
        RegexOption.IGNORE_CASE
    )

    // Quality and codec tokens often found in brackets or at end
    private val QUALITY_AND_TAGS_REGEX = Regex(
        """\b(?:4K|UHD|FHD|HD|SD|1080p|720p|480p|HDR|HDR10|HDR10\+|DV|Dolby Vision|HEVC|H\.?264|H\.?265|x264|x265|Remux|BluRay|BDRip|WEB-DL|WEBRip|Rip|Multi[- ]?Audio|Dual[- ]?Audio|Dubbed|Subbed)\b""",
        RegexOption.IGNORE_CASE
    )

    // Standalone promotional words "NEW" or "HOT" that appear at the beginning or end of string
    private val PROMO_WORDS_EDGE_REGEX = Regex(
        """(?:^\s*\b(?:NEW|HOT)\b\s*|\s*\b(?:NEW|HOT)\b\s*$)""",
        RegexOption.IGNORE_CASE
    )

    fun sanitize(rawTitle: String?): TmdbCleanTitle {
        if (rawTitle.isNullOrBlank()) return TmdbCleanTitle("", null)

        val trimmed = rawTitle.trim()

        // 1. Check for standard "Title (Year) [optional trailing]" pattern
        val parenMatch = PARENTHESIZED_YEAR_REGEX.find(trimmed)
        if (parenMatch != null) {
            val titlePart = parenMatch.groupValues[1]
            val year = parenMatch.groupValues[2].toIntOrNull()

            val cleanTitle = cleanTitlePart(titlePart)
            if (cleanTitle.isNotBlank()) {
                return TmdbCleanTitle(cleanTitle, year)
            }
        }

        // 2. Fallback when there is no (Year):
        var working = trimmed.replace(EMOJI_AND_SYMBOLS_REGEX, " ")
        working = working.replace(PREFIX_TAGS_REGEX, " ")

        // Extract standalone year if present
        val standaloneYearMatch = STANDALONE_YEAR_REGEX.find(working)
        val extractedYear = standaloneYearMatch?.groupValues?.get(1)?.toIntOrNull()

        if (standaloneYearMatch != null) {
            // Take text before the standalone year as title
            val beforeYear = working.substring(0, standaloneYearMatch.range.first)
            val clean = cleanTitlePart(beforeYear)
            if (clean.isNotBlank()) {
                return TmdbCleanTitle(clean, extractedYear)
            }
        }

        // 3. Last fallback: strip quality tokens, promo words, extra whitespace
        working = working.replace(QUALITY_AND_TAGS_REGEX, " ")
        working = working.replace(PROMO_WORDS_EDGE_REGEX, " ")
        val finalClean = cleanTitlePart(working)
        return TmdbCleanTitle(finalClean, extractedYear)
    }

    private fun cleanTitlePart(text: String): String {
        var result = text.replace(EMOJI_AND_SYMBOLS_REGEX, " ")
        result = result.replace(PREFIX_TAGS_REGEX, " ")
        result = result.replace(PROMO_WORDS_EDGE_REGEX, " ")
        // Replace dots and underscores with spaces if used as separators
        result = result.replace(Regex("""[._]"""), " ")
        // Collapse multiple spaces into one and trim
        return result.replace(Regex("""\s+"""), " ").trim()
    }
}
