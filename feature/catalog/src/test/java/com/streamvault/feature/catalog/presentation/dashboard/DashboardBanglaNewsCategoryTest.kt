package com.streamvault.feature.catalog.presentation.dashboard

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DashboardBanglaNewsCategoryTest {

    @Test
    fun `resolveBanglaNewsFlag correctly resolves clean channel names`() {
        val bangladeshChannels = listOf(
            "ATN News" to "atnnews.bd",
            "Channel 1" to "Channel 1",
            "Channel 24" to "channel24.bd",
            "DBC News HD" to "DBCNEWSHD.bd",
            "Ekattor TV" to "ekattor.tv",
            "Ekhon TV" to "EKHON.TV",
            "Independent Television" to "INDEPENDENT TV.bd",
            "Jago News 24" to "JagoNews24",
            "Jamuna TV" to "jamuna.tv",
            "News 21 Bangla" to "NEWS 21 BANGLA",
            "News24 Television" to "NEWS24.bd",
            "Somoy News" to "somoyNEWS.bd",
            "BTV News" to "btvnews.bd",
            "Sangsad Bangladesh" to "sangsad.bd",
            "Star News" to "Star News",
            "TBN 24 USA" to "TBN24.us"
        )

        for ((name, epg) in bangladeshChannels) {
            val flag = resolveBanglaNewsFlag(name, epg)
            assertThat(flag).isEqualTo("bd")
        }

        val kolkataChannels = listOf(
            "ABP Ananda" to "ABPAnanda.in",
            "AK Bangla 24x7" to "akbangla.in",
            "Ananda Barta" to "Ananda Barta",
            "Calcutta News" to "CalcuttaNews",
            "High News" to "highnews.in",
            "Kolkata TV" to "KolkataTv.in",
            "KTV Bangla" to "TribeTV.in",
            "News18 Bangla" to "News18BanglaNews",
            "R Plus News" to "R Plus News",
            "Republic Bangla" to "RepublicBangla.in",
            "TV9 Bangla" to "TV9Bangla.in",
            "Zee 24 Ghanta" to "zee24ghanta.in",
            "Zillar Barta" to "ZillarBarta"
        )

        for ((name, epg) in kolkataChannels) {
            val flag = resolveBanglaNewsFlag(name, epg)
            assertThat(flag).isEqualTo("in")
        }
    }

    @Test
    fun `resolveBanglaNewsFlag supports legacy prefix format for backward compatibility`() {
        assertThat(resolveBanglaNewsFlag("BD | Channel 24")).isEqualTo("bd")
        assertThat(resolveBanglaNewsFlag("BD|Somoy TV")).isEqualTo("bd")
        assertThat(resolveBanglaNewsFlag("IN | ABP Ananda")).isEqualTo("in")
        assertThat(resolveBanglaNewsFlag("IN|Zee 24 Ghanta")).isEqualTo("in")
    }

    @Test
    fun `formatBanglaNewsDisplayName cleanly handles both prefixed and clean names`() {
        assertThat(formatBanglaNewsDisplayName("BD | Channel 24")).isEqualTo("Channel 24")
        assertThat(formatBanglaNewsDisplayName("IN | ABP Ananda")).isEqualTo("ABP Ananda")
        assertThat(formatBanglaNewsDisplayName("Jamuna TV")).isEqualTo("Jamuna TV")
        assertThat(formatBanglaNewsDisplayName("Zee 24 Ghanta")).isEqualTo("Zee 24 Ghanta")
    }
}
