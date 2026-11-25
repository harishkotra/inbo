package app.clearsms.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpValidityTest {
    @Test
    fun `uses the network sent time for a stated validity window`() {
        val expiry = OtpValidity.expiresAt("OTP 123456 is valid for 5 minutes.", 61_000L, 1_000L)
        assertEquals(301_000L, expiry)
    }

    @Test
    fun `falls back to receipt time when the network time is missing or in the future`() {
        val body = "Your OTP expires in 30 seconds."
        assertEquals(31_000L, OtpValidity.expiresAt(body, 1_000L, null))
        assertEquals(31_000L, OtpValidity.expiresAt(body, 1_000L, 2_000L))
    }

    @Test
    fun `leaves unstated and ambiguous expiry blank`() {
        assertNull(OtpValidity.expiresAt("Your OTP is 123456.", 1_000L, null))
        assertNull(OtpValidity.expiresAt("Valid for 5 minutes. Expires in 1 hour.", 1_000L, null))
    }
}
