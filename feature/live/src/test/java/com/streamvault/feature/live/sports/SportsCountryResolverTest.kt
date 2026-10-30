package com.streamvault.feature.live.sports

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SportsCountryResolverTest {
    @Test
    fun `resolves major cricket national teams`() {
        assertThat(SportsCountryResolver.resolveFlagUrl("India"))
            .isEqualTo("https://flagcdn.com/w80/in.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Bangladesh"))
            .isEqualTo("https://flagcdn.com/w80/bd.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Pakistan"))
            .isEqualTo("https://flagcdn.com/w80/pk.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Australia"))
            .isEqualTo("https://flagcdn.com/w80/au.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("England"))
            .isEqualTo("https://flagcdn.com/w80/gb-eng.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("South Africa"))
            .isEqualTo("https://flagcdn.com/w80/za.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("New Zealand"))
            .isEqualTo("https://flagcdn.com/w80/nz.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Sri Lanka"))
            .isEqualTo("https://flagcdn.com/w80/lk.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Afghanistan"))
            .isEqualTo("https://flagcdn.com/w80/af.png")
    }

    @Test
    fun `resolves west indies special cricket badge`() {
        assertThat(SportsCountryResolver.resolveFlagUrl("West Indies"))
            .isEqualTo("local_res://ic_sports_flag_west_indies")
        assertThat(SportsCountryResolver.resolveFlagUrl("Windies"))
            .isEqualTo("local_res://ic_sports_flag_west_indies")
        assertThat(SportsCountryResolver.resolveFlagUrl("West Indies Men"))
            .isEqualTo("local_res://ic_sports_flag_west_indies")
    }

    @Test
    fun `strips team suffixes and noise words`() {
        assertThat(SportsCountryResolver.resolveFlagUrl("Team India"))
            .isEqualTo("https://flagcdn.com/w80/in.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("India Men"))
            .isEqualTo("https://flagcdn.com/w80/in.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Pakistan Cricket"))
            .isEqualTo("https://flagcdn.com/w80/pk.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Australia XI"))
            .isEqualTo("https://flagcdn.com/w80/au.png")
    }

    @Test
    fun `resolves major football national teams`() {
        assertThat(SportsCountryResolver.resolveFlagUrl("Argentina"))
            .isEqualTo("https://flagcdn.com/w80/ar.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Brazil"))
            .isEqualTo("https://flagcdn.com/w80/br.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("France"))
            .isEqualTo("https://flagcdn.com/w80/fr.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Germany"))
            .isEqualTo("https://flagcdn.com/w80/de.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Spain"))
            .isEqualTo("https://flagcdn.com/w80/es.png")
        assertThat(SportsCountryResolver.resolveFlagUrl("Portugal"))
            .isEqualTo("https://flagcdn.com/w80/pt.png")
    }

    @Test
    fun `returns null for club teams leaving fallback initials`() {
        assertThat(SportsCountryResolver.resolveFlagUrl("Dempo SC")).isNull()
        assertThat(SportsCountryResolver.resolveFlagUrl("Shillong Lajong FC")).isNull()
        assertThat(SportsCountryResolver.resolveFlagUrl("Real Madrid")).isNull()
        assertThat(SportsCountryResolver.resolveFlagUrl("FC Barcelona")).isNull()
        assertThat(SportsCountryResolver.resolveFlagUrl("")).isNull()
        assertThat(SportsCountryResolver.resolveFlagUrl("   ")).isNull()
    }
}
