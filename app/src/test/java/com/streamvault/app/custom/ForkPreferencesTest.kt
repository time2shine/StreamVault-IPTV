package com.streamvault.app.custom

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.streamvault.app.system.AppSystemWelcomeAdapter
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ForkPreferencesTest {

    private val context: Context = mock()
    private val preferences: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()
    private var storedSeeded = false

    @Before
    fun setUp() {
        storedSeeded = false
        whenever(context.getSharedPreferences(any(), any())).thenReturn(preferences)
        whenever(preferences.getBoolean(any(), any())).thenAnswer { storedSeeded }
        whenever(preferences.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenAnswer { invocation ->
            storedSeeded = invocation.getArgument(1)
            editor
        }
    }

    @Test
    fun `first launch seeds default M3U provider and marks as seeded`() {
        val config = AppSystemWelcomeAdapter.resolveDevProviderConfig(context)

        assertThat(config.m3uUrl).isEqualTo(ForkConfig.DEFAULT_M3U_URL)
        assertThat(config.m3uName).isEqualTo(ForkConfig.DEFAULT_M3U_NAME)
        assertThat(config.m3uVodClassificationEnabled).isTrue()
        assertThat(storedSeeded).isTrue()
    }

    @Test
    fun `subsequent check after user deletes provider does not re-seed`() {
        storedSeeded = true

        val config = AppSystemWelcomeAdapter.resolveDevProviderConfig(context)

        assertThat(config.m3uUrl).isEmpty()
        assertThat(config.m3uName).isEmpty()
    }
}
