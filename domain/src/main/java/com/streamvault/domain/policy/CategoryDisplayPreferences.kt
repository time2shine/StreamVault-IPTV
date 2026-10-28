package com.streamvault.domain.policy

import com.streamvault.domain.model.Category
import com.streamvault.domain.model.CategorySortMode
import java.util.Locale

/**
 * Resolves the display order priority for Live TV categories.
 * Strict priority ensures core categories appear in the exact order requested:
 * 1. Bangla
 * 2. Bangla News
 * 3. International News
 * 4. India
 * 5. Pakistan
 * 6. Movies
 * 7. Educational
 * 8. Music
 * 9. International
 * 10. Travel
 * 11. Sports
 * 12. Religious
 * 13. Kids
 * Other categories follow after with priority 100+ (alphabetical).
 */
fun resolveLiveCategoryDisplayPriority(categoryName: String): Int {
    val trimmed = categoryName.trim()
    val lower = trimmed.lowercase(Locale.ROOT)

    return when {
        // 1. Exact matches (highest precedence)
        lower == "bangla" -> 1
        lower == "bangla news" -> 2
        lower == "international news" -> 3
        lower == "india" || lower == "indian" -> 4
        lower == "pakistan" || lower == "pakistani" -> 5
        lower == "movies" || lower == "movie" || lower == "movies channels" || lower == "movie channels" || lower == "movies channel" || lower == "movie channel" -> 6
        lower == "educational" || lower == "education" -> 7
        lower == "music" -> 8
        lower == "international" -> 9
        lower == "travel" -> 10
        lower == "sports" || lower == "sport" -> 11
        lower == "religious" || lower == "religion" -> 12
        lower == "kids" || lower == "kid" -> 13

        // 2. Substring / fuzzy matches for IPTV playlist variations
        "bangla" in lower && "news" in lower -> 2
        "bangla" in lower -> 1
        "international" in lower && "news" in lower -> 3
        "india" in lower || "hindi" in lower -> 4
        "pakistan" in lower || "urdu" in lower -> 5
        "movie" in lower || "cinema" in lower || "film" in lower -> 6
        "edu" in lower || "science" in lower || "learn" in lower -> 7
        "music" in lower || "song" in lower || "gaan" in lower -> 8
        "international" in lower || "world" in lower -> 9
        "travel" in lower || "tour" in lower -> 10
        "sport" in lower || "cricket" in lower || "football" in lower -> 11
        "relig" in lower || "islam" in lower || "quran" in lower -> 12
        "kid" in lower || "cartoon" in lower || "anim" in lower -> 13
        else -> 100
    }
}

fun sortLiveCategoriesByPreferredOrder(categories: List<Category>): List<Category> {
    return categories.sortedWith(
        compareBy<Category> { resolveLiveCategoryDisplayPriority(it.name) }
            .thenBy { it.name.lowercase(Locale.ROOT) }
    )
}

fun applyProviderCategoryDisplayPreferences(
    categories: List<Category>,
    hiddenCategoryIds: Set<Long>,
    sortMode: CategorySortMode
): List<Category> {
    val visible = categories.filterNot { it.id in hiddenCategoryIds }
    return when (sortMode) {
        CategorySortMode.DEFAULT -> sortLiveCategoriesByPreferredOrder(visible)
        CategorySortMode.TITLE_ASC -> visible.sortedBy { it.name.lowercase(Locale.ROOT) }
        CategorySortMode.TITLE_DESC -> visible.sortedByDescending { it.name.lowercase(Locale.ROOT) }
        CategorySortMode.COUNT_DESC -> visible.sortedWith(
            compareByDescending<Category> { it.count }.thenBy { it.name.lowercase(Locale.ROOT) }
        )
        CategorySortMode.COUNT_ASC -> visible.sortedWith(
            compareBy<Category> { it.count }.thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }
}
