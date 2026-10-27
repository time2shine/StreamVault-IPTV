package com.streamvault.data.parser

import java.net.URLDecoder
import java.util.Locale

internal enum class M3uMediaKind {
    LIVE,
    VOD
}

internal enum class M3uVodEvidence {
    EXPLICIT_OVERRIDE,
    FILE_EXTENSION,
    PATH_SEGMENT,
    LIVE_STREAM_EXTENSION,
    LIVE_TEXT_HINT,
    GROUP_KEYWORD,
    NAME_KEYWORD,
    DEFAULT_LIVE
}

internal data class M3uVodClassification(
    val kind: M3uMediaKind,
    val evidence: M3uVodEvidence,
    val confidence: Float
) {
    val isVod: Boolean get() = kind == M3uMediaKind.VOD
}

/**
 * Configurable rules for providers whose naming differs from the defaults. Keywords are matched
 * as words for whitespace-delimited languages and as phrases for languages without spaces.
 */
internal data class M3uVodRules(
    val vodExtensions: Set<String> = DEFAULT_VOD_EXTENSIONS,
    val liveExtensions: Set<String> = DEFAULT_LIVE_EXTENSIONS,
    val vodPathSegments: Set<String> = DEFAULT_VOD_PATH_SEGMENTS,
    val vodKeywords: Set<String> = DEFAULT_VOD_KEYWORDS,
    val liveKeywords: Set<String> = DEFAULT_LIVE_KEYWORDS
) {
    companion object {
        val DEFAULT_VOD_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "m4v", "webm", "wmv", "flv", "mpg", "mpeg")
        val DEFAULT_LIVE_EXTENSIONS = setOf("m3u8", "ts")
        val DEFAULT_VOD_PATH_SEGMENTS = setOf("movie", "movies", "vod", "film", "films")
        val DEFAULT_VOD_KEYWORDS = setOf(
            "movie", "movies", "vod", "film", "films", "cinema",
            "película", "películas", "pelicula", "peliculas",
            "filme", "filmes", "kino", "фильм", "фильмы",
            "电影", "電影", "映画", "영화", "أفلام", "סרטים"
        )
        val DEFAULT_LIVE_KEYWORDS = setOf(
            "live", "live tv", "channel", "channels", "canal", "chaîne",
            "direct", "en vivo", "ao vivo", "直播", "ライブ", "مباشر", "שידור חי"
        )
    }
}

/** Deterministic M3U media classifier that exposes why an entry was classified. */
internal class M3uVodClassifier(
    private val rules: M3uVodRules = M3uVodRules()
) {
    companion object {
        private val NON_ALPHANUMERIC_REGEX = Regex("[^\\p{L}\\p{N}]+")

        private fun normalizeStatic(value: String): String = value
            .lowercase(Locale.ROOT)
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .trim()
    }

    // Pre-normalize keywords once at instantiation rather than inside the per-entry loop
    private val preNormalizedVodKeywords: Set<String> =
        rules.vodKeywords.map(::normalizeStatic).filter(String::isNotEmpty).toSet()
    private val preNormalizedLiveKeywords: Set<String> =
        rules.liveKeywords.map(::normalizeStatic).filter(String::isNotEmpty).toSet()

    fun classify(
        entry: M3uParser.M3uEntry,
        override: M3uMediaKind? = null
    ): M3uVodClassification {
        override?.let {
            return M3uVodClassification(it, M3uVodEvidence.EXPLICIT_OVERRIDE, 1.0f)
        }

        // Fast-path: Check explicit VOD groups first (avoids URI parsing, path splitting & regex)
        val trimmedGroup = entry.groupTitle.trim()
        if (trimmedGroup.startsWith("Movies | ", ignoreCase = true) ||
            trimmedGroup.startsWith("Movie | ", ignoreCase = true) ||
            trimmedGroup.contains("kids songs", ignoreCase = true) ||
            trimmedGroup.contains("kids song", ignoreCase = true)
        ) {
            return M3uVodClassification(M3uMediaKind.VOD, M3uVodEvidence.GROUP_KEYWORD, 1.0f)
        }

        val path = decodedPath(entry.url)
        val segments = path.split('/').filter(String::isNotBlank)
        val extension = segments.lastOrNull()?.substringAfterLast('.', missingDelimiterValue = "")

        if (extension in rules.vodExtensions) {
            return M3uVodClassification(M3uMediaKind.VOD, M3uVodEvidence.FILE_EXTENSION, 1.0f)
        }
        if (segments.any { it in rules.vodPathSegments }) {
            return M3uVodClassification(M3uMediaKind.VOD, M3uVodEvidence.PATH_SEGMENT, 0.95f)
        }
        if (extension in rules.liveExtensions) {
            return M3uVodClassification(M3uMediaKind.LIVE, M3uVodEvidence.LIVE_STREAM_EXTENSION, 0.95f)
        }

        val group = normalize(entry.groupTitle)
        val name = normalize(entry.name)
        if (matchesAny(group, preNormalizedLiveKeywords) || matchesAny(name, preNormalizedLiveKeywords)) {
            return M3uVodClassification(M3uMediaKind.LIVE, M3uVodEvidence.LIVE_TEXT_HINT, 0.8f)
        }
        if (matchesAny(group, preNormalizedVodKeywords)) {
            return M3uVodClassification(M3uMediaKind.VOD, M3uVodEvidence.GROUP_KEYWORD, 0.7f)
        }
        if (matchesAny(name, preNormalizedVodKeywords)) {
            return M3uVodClassification(M3uMediaKind.VOD, M3uVodEvidence.NAME_KEYWORD, 0.55f)
        }
        return M3uVodClassification(M3uMediaKind.LIVE, M3uVodEvidence.DEFAULT_LIVE, 1.0f)
    }

    private fun decodedPath(url: String): String {
        val simple = url.substringBefore('#').substringBefore('?')
        if (simple.contains('%')) {
            val decoded = runCatching { URLDecoder.decode(simple, "UTF-8") }.getOrNull()
            if (decoded != null) return decoded.lowercase(Locale.ROOT)
        }
        return simple.lowercase(Locale.ROOT)
    }

    private fun normalize(value: String): String = normalizeStatic(value)

    private fun matchesAny(value: String, preNormalizedKeywords: Set<String>): Boolean {
        if (value.isEmpty()) return false
        val paddedValue = " $value "
        return preNormalizedKeywords.any { normalizedKeyword ->
            paddedValue.contains(" $normalizedKeyword ") ||
                (normalizedKeyword.none(Char::isWhitespace) &&
                    normalizedKeyword.any { it.code > 127 } &&
                    value.contains(normalizedKeyword))
        }
    }
}
