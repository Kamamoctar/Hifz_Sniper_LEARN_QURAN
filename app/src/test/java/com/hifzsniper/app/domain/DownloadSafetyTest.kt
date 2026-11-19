package com.hifzsniper.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DownloadSafetyTest {
    @Test fun onlyPlainHttpsUrlsWithoutCredentialsOrFragmentsAreAccepted() {
        assertTrue(DownloadSafety.isSecureRemoteUrl("https://audio.example.test/001001.mp3"))
        assertFalse(DownloadSafety.isSecureRemoteUrl("http://audio.example.test/001001.mp3"))
        assertFalse(DownloadSafety.isSecureRemoteUrl("https://user:secret@example.test/audio.mp3"))
        assertFalse(DownloadSafety.isSecureRemoteUrl("https://example.test/audio.mp3#fragment"))
    }

    @Test fun identifiersCannotEscapeTheAudioDirectory() {
        assertEquals("secret.mp3", DownloadSafety.safeFileName("../../secret"))
        assertFalse(DownloadSafety.safeFileName("../../secret").contains('/'))
        assertFalse(DownloadSafety.safeFileName("../../secret").contains('\\'))
    }

    @Test fun canonicalContainmentRejectsParentTraversal() {
        val parent = File(System.getProperty("java.io.tmpdir"), "hifz-audio")
        assertTrue(DownloadSafety.isInside(parent, File(parent, "safe.mp3")))
        assertFalse(DownloadSafety.isInside(parent, File(parent, "../outside.mp3")))
    }
}
