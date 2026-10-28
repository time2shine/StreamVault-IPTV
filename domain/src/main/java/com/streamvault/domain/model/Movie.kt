package com.streamvault.domain.model

private val NEW_EMOJI_PATTERN = Regex("""(?i)(?:\s*\[\s*new\s*\]|\s*\(\s*new\s*\)|(?:\s*\bnew\b\s*)?[🆕\uD83C\uDD95\uFE0F]+(?:\s*\bnew\b\s*)?)""")
private val EMPTY_BRACKETS_PATTERN = Regex("""[\[\(]\s*[\]\)]""")
private val MULTI_SPACE_PATTERN = Regex("""\s+""")

data class Movie(
    val id: Long,
    val name: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val streamUrl: String = "",
    val containerExtension: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val duration: String? = null,
    val durationSeconds: Int = 0,
    val rating: Float = 0f,
    val year: String? = null,
    val tmdbId: Long? = null,
    val youtubeTrailer: String? = null,
    val isFavorite: Boolean = false,
    val providerId: Long = 0,
    val watchProgress: Long = 0L,
    val lastWatchedAt: Long = 0L,
    val isAdult: Boolean = false,
    val isUserProtected: Boolean = false,
    val streamId: Long = 0L,
    val addedAt: Long = 0L,
    val logicalGroupId: String? = null,
    val selectedVariantId: Long? = null,
    val variants: List<VodMovieVariant> = emptyList(),
    val duplicateConfidence: VodDuplicateConfidence = VodDuplicateConfidence.NONE,
    val variantLabel: String? = null
) {
    val hasNewBadge: Boolean = name.contains("🆕") || name.contains("\uD83C\uDD95") ||
        name.contains("[NEW]", ignoreCase = true) || name.contains("(NEW)", ignoreCase = true)

    val displayTitle: String = if (!hasNewBadge) {
        name
    } else {
        val stripped = name.replace(NEW_EMOJI_PATTERN, " ")
        val withoutEmptyBrackets = stripped.replace(EMPTY_BRACKETS_PATTERN, " ")
        withoutEmptyBrackets.replace(MULTI_SPACE_PATTERN, " ").trim()
    }

    init {
        require(durationSeconds >= 0) { "durationSeconds must be non-negative" }
        require(rating in 0f..10f) { "rating must be between 0 and 10" }
        require(watchProgress >= 0) { "watchProgress must be non-negative" }
        require(lastWatchedAt >= 0) { "lastWatchedAt must be non-negative" }
    }
}

