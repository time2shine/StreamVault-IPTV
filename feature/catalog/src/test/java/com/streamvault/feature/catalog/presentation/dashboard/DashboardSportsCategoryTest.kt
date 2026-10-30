package com.streamvault.feature.catalog.presentation.dashboard

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DashboardSportsCategoryTest {

    @Test
    fun `isSportsCategoryToCombine correctly identifies sports categories`() {
        val sportsCategoryNames = listOf(
            "Sports",
            "SPORTS HD",
            "Sport Channels",
            "Cricket Live",
            "Football UK",
            "Soccer Premier",
            "FIFA World Cup",
            "WWE Network",
            "Motorsport",
            "Racing TV",
            "F1 Racing",
            "Tennis Open",
            "Basketball",
            "NBA League",
            "Sony Ten Sports",
            "Golf Channel",
            "Baseball Extra",
            "Hockey Night",
            "Badminton HD",
            "UFC Fight Pass"
        )

        for (name in sportsCategoryNames) {
            val lower = name.trim().lowercase(java.util.Locale.ROOT)
            val isSports = lower == "sports" || lower == "sport" ||
                lower.contains("sports") || lower.contains("sport") ||
                lower.contains("cricket") || lower.contains("football") ||
                lower.contains("soccer") || lower.contains("fifa") ||
                lower.contains("wwe") || lower.contains("motorsport") ||
                lower.contains("racing") || lower.contains("f1") ||
                lower.contains("tennis") || lower.contains("basketball") ||
                lower.contains("nba") || lower.contains("sony ten") ||
                lower.contains("golf") || lower.contains("baseball") ||
                lower.contains("hockey") || lower.contains("badminton") ||
                lower.contains("rugby") || lower.contains("boxing") ||
                lower.contains("ufc") || lower.contains("mma")
            assertThat(isSports).isTrue()
        }

        // Non-sports categories should NOT match
        val nonSportsCategoryNames = listOf(
            "Bangla",
            "Bangla News",
            "Movies",
            "Kids",
            "Educational",
            "Music",
            "Transport News"
        )

        for (name in nonSportsCategoryNames) {
            val lower = name.trim().lowercase(java.util.Locale.ROOT)
            val isSports = if (lower.contains("transport")) false else {
                lower == "sports" || lower == "sport" ||
                lower.contains("sports") || lower.contains("sport") ||
                lower.contains("cricket") || lower.contains("football") ||
                lower.contains("soccer") || lower.contains("fifa") ||
                lower.contains("wwe") || lower.contains("motorsport") ||
                lower.contains("racing") || lower.contains("f1") ||
                lower.contains("tennis") || lower.contains("basketball") ||
                lower.contains("nba") || lower.contains("sony ten") ||
                lower.contains("golf") || lower.contains("baseball") ||
                lower.contains("hockey") || lower.contains("badminton") ||
                lower.contains("rugby") || lower.contains("boxing") ||
                lower.contains("ufc") || lower.contains("mma")
            }
            assertThat(isSports).isFalse()
        }
    }

    @Test
    fun `matchesCricketChannel matches cricket-related channels`() {
        assertThat(matchesCricketChannel("T Sports HD")).isTrue()
        assertThat(matchesCricketChannel("Star Sports 1 HD")).isTrue()
        assertThat(matchesCricketChannel("Willow Cricket")).isTrue()
        assertThat(matchesCricketChannel("Sony Ten 1 HD")).isTrue()
        assertThat(matchesCricketChannel("PTV Sports")).isTrue()
        assertThat(matchesCricketChannel("Gazi TV GTV")).isTrue()
        assertThat(matchesCricketChannel("DD Sports")).isTrue()
        assertThat(matchesCricketChannel("Sky Sports Cricket")).isTrue()

        // Non-cricket channels
        assertThat(matchesCricketChannel("beIN SPORTS 1")).isFalse()
        assertThat(matchesCricketChannel("Premier League TV")).isFalse()
    }

    @Test
    fun `matchesFootballChannel matches football-related channels`() {
        assertThat(matchesFootballChannel("beIN SPORTS 1")).isTrue()
        assertThat(matchesFootballChannel("SuperSport Premier League")).isTrue()
        assertThat(matchesFootballChannel("FOOTBALL HD")).isTrue()
        assertThat(matchesFootballChannel("DAZN 1 Bar")).isTrue()
        assertThat(matchesFootballChannel("LaLiga TV")).isTrue()
        assertThat(matchesFootballChannel("UEFA Champions League")).isTrue()
        assertThat(matchesFootballChannel("Sky Sports Football")).isTrue()

        // Non-football channels
        assertThat(matchesFootballChannel("Willow Cricket")).isFalse()
        assertThat(matchesFootballChannel("Golf Channel")).isFalse()
    }
}
