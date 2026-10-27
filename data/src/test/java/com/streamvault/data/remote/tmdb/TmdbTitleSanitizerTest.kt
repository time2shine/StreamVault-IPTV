package com.streamvault.data.remote.tmdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbTitleSanitizerTest {

    @Test
    fun `sanitizes user format with year and trailing new emoji`() {
        val result = TmdbTitleSanitizer.sanitize("Love you (2026) new 🆕")
        assertEquals("Love you", result.cleanTitle)
        assertEquals(2026, result.year)
    }

    @Test
    fun `sanitizes user format with year only`() {
        val result = TmdbTitleSanitizer.sanitize("Kiss me (2022)")
        assertEquals("Kiss me", result.cleanTitle)
        assertEquals(2022, result.year)
    }

    @Test
    fun `sanitizes movie with subtitle and year and new emoji`() {
        val result = TmdbTitleSanitizer.sanitize("Avatar: The Way of Water (2022) new 🆕")
        assertEquals("Avatar: The Way of Water", result.cleanTitle)
        assertEquals(2022, result.year)
    }

    @Test
    fun `strips leading emojis and trailing tags`() {
        val result = TmdbTitleSanitizer.sanitize("🔥 NEW Inception (2010) [4K HDR] [Multi-Audio] 🎬")
        assertEquals("Inception", result.cleanTitle)
        assertEquals(2010, result.year)
    }

    @Test
    fun `preserves word New inside movie title`() {
        val result = TmdbTitleSanitizer.sanitize("Star Wars: Episode IV - A New Hope (1977)")
        assertEquals("Star Wars: Episode IV - A New Hope", result.cleanTitle)
        assertEquals(1977, result.year)
    }

    @Test
    fun `sanitizes movie with dot separators`() {
        val result = TmdbTitleSanitizer.sanitize("Spider.Man.No.Way.Home (2021) 1080p")
        assertEquals("Spider Man No Way Home", result.cleanTitle)
        assertEquals(2021, result.year)
    }

    @Test
    fun `sanitizes movie with standalone year without parentheses`() {
        val result = TmdbTitleSanitizer.sanitize("Gladiator 2000 REMUX")
        assertEquals("Gladiator", result.cleanTitle)
        assertEquals(2000, result.year)
    }

    @Test
    fun `handles blank and null input gracefully`() {
        assertEquals("", TmdbTitleSanitizer.sanitize("").cleanTitle)
        assertNull(TmdbTitleSanitizer.sanitize("").year)
        assertEquals("", TmdbTitleSanitizer.sanitize(null).cleanTitle)
        assertNull(TmdbTitleSanitizer.sanitize(null).year)
    }
}
