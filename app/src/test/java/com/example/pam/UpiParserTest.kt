package com.example.pam

import com.example.pam.util.UpiParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiParserTest {

    @Test
    fun testParseFullUpiUri() {
        val uri = "upi://pay?pa=merchant@okhdfcbank&pn=Fresh%20Mart&am=250.50&cu=INR&tn=Weekly%20Groceries&tr=TXN123456"
        val parsed = UpiParser.parse(uri)

        assertNotNull(parsed)
        assertEquals("merchant@okhdfcbank", parsed?.upiId)
        assertEquals("Fresh Mart", parsed?.payeeName)
        assertEquals(250.50, parsed?.amount)
        assertEquals("Weekly Groceries", parsed?.note)
        assertEquals("TXN123456", parsed?.refId)
    }

    @Test
    fun testParseMinimalUpiUri() {
        val uri = "upi://pay?pa=someone@upi"
        val parsed = UpiParser.parse(uri)

        assertNotNull(parsed)
        assertEquals("someone@upi", parsed?.upiId)
        assertNull(parsed?.payeeName)
        assertNull(parsed?.amount)
    }

    @Test
    fun testParsePlainUpiId() {
        val upiId = "john.doe@okaxis"
        val parsed = UpiParser.parse(upiId)

        assertNotNull(parsed)
        assertEquals("john.doe@okaxis", parsed?.upiId)
    }

    @Test
    fun testParseInvalidString() {
        val invalid = "https://google.com"
        val parsed = UpiParser.parse(invalid)

        assertNull(parsed)
    }

    @Test
    fun testBuildUpiUri() {
        val generated = UpiParser.buildUpiUriString(
            upiId = "store@bank",
            payeeName = "My Store",
            amount = 100.0,
            note = "Bill Payment"
        )

        // Must contain literal '@' in 'pa' as required by RFC 3986 & NPCI
        assertTrue(generated.contains("pa=store@bank"))
        assertTrue(generated.contains("pn=My+Store") || generated.contains("pn=My%20Store"))
        assertTrue(generated.contains("am=100.00"))
        assertTrue(generated.contains("cu=INR"))
        // Must NOT inject synthetic 'tr' parameter
        org.junit.Assert.assertFalse(generated.contains("tr=CRB"))
    }

    @Test
    fun testGetPaymentUriFromScannedMerchantQr() {
        val rawScanned = "upi://pay?pa=merchant@okhdfcbank&pn=Fresh%20Mart&mc=5499&mode=02&orgid=180001&sign=MEQCIBx+12/34=="
        val paymentUri = UpiParser.getPaymentUri(
            rawScannedUri = rawScanned,
            upiId = "merchant@okhdfcbank",
            payeeName = "Fresh Mart",
            amount = 350.0,
            note = null
        )

        assertTrue(paymentUri.contains("pa=merchant@okhdfcbank"))
        assertTrue(paymentUri.contains("mc=5499"))
        assertTrue(paymentUri.contains("mode=02"))
        assertTrue(paymentUri.contains("orgid=180001"))
        assertTrue(paymentUri.contains("sign=MEQCIBx+12/34=="))
        assertTrue(paymentUri.contains("am=350.00"))
        assertTrue(paymentUri.contains("cu=INR"))
        org.junit.Assert.assertFalse(paymentUri.contains("tr=CRB"))
        org.junit.Assert.assertFalse(paymentUri.contains("tn=Crebit"))
    }

    @Test
    fun testGetPaymentUriNormalizesPercent40InPa() {
        val rawScanned = "upi://pay?pa=merchant%40okhdfcbank&pn=FreshMart&mc=5499"
        val paymentUri = UpiParser.getPaymentUri(
            rawScannedUri = rawScanned,
            upiId = "merchant@okhdfcbank",
            payeeName = "FreshMart",
            amount = 150.0,
            note = null
        )

        assertTrue(paymentUri.contains("pa=merchant@okhdfcbank"))
        org.junit.Assert.assertFalse(paymentUri.contains("pa=merchant%40okhdfcbank"))
        assertTrue(paymentUri.contains("am=150.00"))
    }

    @Test
    fun testGetPaymentUriUpdatesExistingAmount() {
        val rawScanned = "upi://pay?pa=store@upi&pn=Store&am=50.00&cu=INR"
        val paymentUri = UpiParser.getPaymentUri(
            rawScannedUri = rawScanned,
            upiId = "store@upi",
            payeeName = "Store",
            amount = 120.0,
            note = null
        )

        assertTrue(paymentUri.contains("am=120.00"))
        org.junit.Assert.assertFalse(paymentUri.contains("am=50.00"))
    }
}
