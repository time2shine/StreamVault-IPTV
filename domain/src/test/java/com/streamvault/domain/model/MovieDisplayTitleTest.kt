package com.streamvault.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieDisplayTitleTest {

    private fun movieWithName(name: String): Movie = Movie(
        id = 1L,
        name = name,
        streamUrl = "http://example.com/1.mp4",
        providerId = 1L
    )

    @Test
    fun `detects new emoji and cleans title with trailing new tag`() {
        val movie = movieWithName("Love you (2026) new 🆕")
        assertTrue(movie.hasNewBadge)
        assertEquals("Love you (2026)", movie.displayTitle)
    }

    @Test
    fun `detects new emoji and cleans title without trailing new word`() {
        val movie = movieWithName("Kiss me (2022) 🆕")
        assertTrue(movie.hasNewBadge)
        assertEquals("Kiss me (2022)", movie.displayTitle)
    }

    @Test
    fun `detects leading new emoji and cleans title`() {
        val movie = movieWithName("🆕 Dune: Part Two")
        assertTrue(movie.hasNewBadge)
        assertEquals("Dune: Part Two", movie.displayTitle)
    }

    @Test
    fun `cleans bracketed new tag`() {
        val movie = movieWithName("Avatar [NEW 🆕]")
        assertTrue(movie.hasNewBadge)
        assertEquals("Avatar", movie.displayTitle)
    }

    @Test
    fun `preserves word New inside title when emoji is present`() {
        val movie = movieWithName("Star Wars: A New Hope 🆕")
        assertTrue(movie.hasNewBadge)
        assertEquals("Star Wars: A New Hope", movie.displayTitle)
    }

    @Test
    fun `normal title without emoji has no new badge and remains unchanged`() {
        val movie = movieWithName("Inception (2010)")
        assertFalse(movie.hasNewBadge)
        assertEquals("Inception (2010)", movie.displayTitle)
    }
}
