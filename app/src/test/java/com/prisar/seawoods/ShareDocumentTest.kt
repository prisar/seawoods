package com.prisar.seawoods

import android.app.Activity
import android.content.Intent
import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShareDocumentTest {

    @Test
    fun shareDocument_withContentUri_createsCorrectIntent() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val contentUri = Uri.parse("content://com.prisar.seawoods.fileprovider/test/document.jpg")

        shareDocument(activity, contentUri, "image/jpeg")

        val shadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity

        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_CHOOSER, startedIntent.action)
        val shareIntent = startedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertNotNull(shareIntent)
        assertEquals(Intent.ACTION_SEND, shareIntent?.action)
        assertEquals("image/jpeg", shareIntent?.type)
        assertEquals(contentUri, shareIntent?.getParcelableExtra(Intent.EXTRA_STREAM))
        assertEquals("Scanned Document", shareIntent?.getStringExtra(Intent.EXTRA_SUBJECT))
        assertEquals("Please find the scanned document attached.", shareIntent?.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun shareDocument_withPdfMimeType_usesCorrectType() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val contentUri = Uri.parse("content://com.prisar.seawoods.fileprovider/test/document.pdf")

        shareDocument(activity, contentUri, "application/pdf")

        val shadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shareIntent = startedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)

        assertEquals("application/pdf", shareIntent?.type)
    }

    @Test
    fun shareDocument_withDefaultMimeType_usesImageJpeg() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val contentUri = Uri.parse("content://com.prisar.seawoods.fileprovider/test/document.jpg")

        shareDocument(activity, contentUri)

        val shadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shareIntent = startedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)

        assertEquals("image/jpeg", shareIntent?.type)
    }

    @Test
    fun shareDocument_includesReadPermissionFlag() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val contentUri = Uri.parse("content://com.prisar.seawoods.fileprovider/test/document.jpg")

        shareDocument(activity, contentUri)

        val shadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shareIntent = startedIntent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)

        val hasReadPermission = (shareIntent?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        assertTrue(hasReadPermission)
    }
}
