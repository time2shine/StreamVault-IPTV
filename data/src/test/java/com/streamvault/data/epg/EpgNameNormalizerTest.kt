package com.streamvault.data.epg

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EpgNameNormalizerTest {

    @Test
    fun `normalize_basicName_lowercaseAndStripped`() {
        assertThat(EpgNameNormalizer.normalize("BBC One HD")).isEqualTo("bbconehd")
    }

    @Test
    fun `normalize_accents_removed`() {
        assertThat(EpgNameNormalizer.normalize("Télé Loisirs")).isEqualTo("teleloisirs")
    }

    @Test
    fun `normalize_specialChars_removed`() {
        assertThat(EpgNameNormalizer.normalize("CNN - US (Int'l)")).isEqualTo("cnnusintl")
    }

    @Test
    fun `normalize_empty_returnsEmpty`() {
        assertThat(EpgNameNormalizer.normalize("")).isEmpty()
        assertThat(EpgNameNormalizer.normalize("   ")).isEmpty()
    }

    @Test
    fun `normalize_numbersPreserved`() {
        assertThat(EpgNameNormalizer.normalize("Sport 1")).isEqualTo("sport1")
    }

    @Test
    fun `normalize_mixedCase_lowered`() {
        assertThat(EpgNameNormalizer.normalize("DiScOvErY")).isEqualTo("discovery")
    }

    @Test
    fun `normalize_allSpecialChars_returnsEmpty`() {
        assertThat(EpgNameNormalizer.normalize("---")).isEmpty()
    }

    @Test
    fun `normalize_germanUmlauts_stripped`() {
        assertThat(EpgNameNormalizer.normalize("ZDF Küche")).isEqualTo("zdfkuche")
    }

    @Test
    fun `normalize_frenchCedilla_stripped`() {
        assertThat(EpgNameNormalizer.normalize("Garçon")).isEqualTo("garcon")
    }

    @Test
    fun `cleanChannelName_stripsLiveWrapperAndCategoryPrefixes`() {
        assertThat(EpgNameNormalizer.cleanChannelName("---Live(Star Sports 1 HD)---")).isEqualTo("Star Sports 1 HD")
        assertThat(EpgNameNormalizer.cleanChannelName("--Live(Star Sports 2 Hindi)--")).isEqualTo("Star Sports 2 Hindi")
        assertThat(EpgNameNormalizer.cleanChannelName("CR | Star Sports 1 Hindi")).isEqualTo("Star Sports 1 Hindi")
        assertThat(EpgNameNormalizer.cleanChannelName("FB | BeIN Sports 1")).isEqualTo("BeIN Sports 1")
    }
}
