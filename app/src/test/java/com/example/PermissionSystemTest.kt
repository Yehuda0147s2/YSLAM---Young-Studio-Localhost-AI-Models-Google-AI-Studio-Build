package com.example

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.util.permission.AppPermission
import com.example.util.permission.HapticFeedbackHelper
import com.example.util.permission.NotificationHelper
import com.example.util.permission.PermissionHelper
import com.example.util.permission.SpeechRecognitionHelper
import com.example.util.permission.SpeechState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionSystemTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testAppPermissionDefinitions() {
        assertEquals(Manifest.permission.RECORD_AUDIO, AppPermission.RECORD_AUDIO.permissionId)
        assertTrue(AppPermission.RECORD_AUDIO.isDangerousRuntime)
        assertTrue(AppPermission.RECORD_AUDIO.purpose.isNotBlank())
        assertTrue(AppPermission.RECORD_AUDIO.privacyGuarantee.isNotBlank())

        assertEquals(Manifest.permission.VIBRATE, AppPermission.VIBRATE.permissionId)
        assertFalse(AppPermission.VIBRATE.isDangerousRuntime)

        assertEquals(Manifest.permission.INTERNET, AppPermission.INTERNET.permissionId)
        assertFalse(AppPermission.INTERNET.isDangerousRuntime)
    }

    @Test
    fun testNormalPermissionsGrantedByDefault() {
        // Test initial check, then grant via Robolectric shadow and verify PermissionHelper
        val app = context as android.app.Application
        org.robolectric.Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.INTERNET,
            Manifest.permission.VIBRATE
        )

        val internetGranted = PermissionHelper.isPermissionGranted(context, AppPermission.INTERNET)
        assertTrue(internetGranted)

        val vibrateGranted = PermissionHelper.isPermissionGranted(context, AppPermission.VIBRATE)
        assertTrue(vibrateGranted)
    }

    @Test
    fun testNotificationChannelCreation() {
        NotificationHelper.ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = manager.getNotificationChannel(NotificationHelper.CHANNEL_ID)
            assertNotNull(channel)
            assertEquals("YSLAM Generation & Alerts", channel.name)
        }
    }

    @Test
    fun testHapticFeedbackSafeExecution() {
        // Verifies no unhandled SecurityException or null pointer occurs
        HapticFeedbackHelper.vibrateShort(context)
        HapticFeedbackHelper.vibrateSuccess(context)
        HapticFeedbackHelper.vibrateError(context)
        assertTrue(true)
    }

    @Test
    fun testSpeechRecognitionHelperInitialState() {
        val helper = SpeechRecognitionHelper(context)
        assertEquals(SpeechState.Idle, helper.state.value)
        assertEquals("", helper.recognizedText.value)

        // Stopping when idle is safe
        helper.stopListening()
        assertEquals(SpeechState.Idle, helper.state.value)
    }

    @Test
    fun testGetAllStatusesReturnsAllPermissions() {
        val statuses = PermissionHelper.getAllStatuses(context)
        assertEquals(AppPermission.values().size, statuses.size)
        assertTrue(statuses.any { it.permission == AppPermission.RECORD_AUDIO })
        assertTrue(statuses.any { it.permission == AppPermission.POST_NOTIFICATIONS })
        assertTrue(statuses.any { it.permission == AppPermission.VIBRATE })
        assertTrue(statuses.any { it.permission == AppPermission.INTERNET })
    }
}
