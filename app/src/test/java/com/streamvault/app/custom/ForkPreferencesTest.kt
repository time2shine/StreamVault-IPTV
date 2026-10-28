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
    private var storedFullLoaded = false
    private var storedLastSync = 0L

    @Before
    fun setUp() {
        storedSeeded = false
        storedFullLoaded = false
        storedLastSync = 0L
        whenever(context.getSharedPreferences(any(), any())).thenReturn(preferences)
        whenever(preferences.getBoolean(any(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            if (key == "full_library_loaded") storedFullLoaded else storedSeeded
        }
        whenever(preferences.getLong(any(), any())).thenAnswer { storedLastSync }
        whenever(preferences.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenAnswer { invocation ->
            val key = invocation.getArgument<String>(0)
            if (key == "full_library_loaded") {
                storedFullLoaded = invocation.getArgument(1)
            } else {
                storedSeeded = invocation.getArgument(1)
            }
            editor
        }
        whenever(editor.putLong(any(), any())).thenAnswer { invocation ->
            storedLastSync = invocation.getArgument(1)
            editor
        }
    }

    @Test
    fun `first launch seeds bootstrap Xtream provider and marks as seeded`() {
        val config = AppSystemWelcomeAdapter.resolveDevProviderConfig(context)

        assertThat(config.xtreamServer).isEqualTo(ForkConfig.DEFAULT_XTREAM_URL)
        assertThat(config.xtreamUsername).isEqualTo(ForkConfig.DEFAULT_XTREAM_USERNAME)
        assertThat(config.xtreamPassword).isEqualTo(ForkConfig.DEFAULT_XTREAM_PASSWORD)
        assertThat(config.xtreamName).isEqualTo(ForkConfig.DEFAULT_XTREAM_NAME)
        assertThat(storedSeeded).isTrue()
    }

    @Test
    fun `subsequent check after user deletes provider does not re-seed`() {
        storedSeeded = true

        val config = AppSystemWelcomeAdapter.resolveDevProviderConfig(context)

        assertThat(config.xtreamServer).isEmpty()
        assertThat(config.xtreamUsername).isEmpty()
        assertThat(config.xtreamPassword).isEmpty()
        assertThat(config.xtreamName).isEmpty()
    }

    @Test
    fun `full library loaded and daily sync timestamps persist correctly`() {
        assertThat(ForkPreferences.isFullLibraryLoaded(context)).isFalse()
        ForkPreferences.setFullLibraryLoaded(context, true)
        assertThat(ForkPreferences.isFullLibraryLoaded(context)).isTrue()

        assertThat(ForkPreferences.getLastDailySyncMillis(context)).isEqualTo(0L)
        val timestamp = 1700000000000L
        ForkPreferences.setLastDailySyncMillis(context, timestamp)
        assertThat(ForkPreferences.getLastDailySyncMillis(context)).isEqualTo(timestamp)
    }
}
