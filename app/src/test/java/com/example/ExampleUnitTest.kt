package com.example

import com.example.data.local.TransactionEntity
import com.example.data.model.Currencies
import com.example.util.CsvExporter
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.SecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCurrencyParsing() {
        assertEquals(25000L, CurrencyFormatter.parseToCents("250"))
        assertEquals(25050L, CurrencyFormatter.parseToCents("250.50"))
        assertEquals(25050L, CurrencyFormatter.parseToCents("250.5"))
        assertEquals(123456L, CurrencyFormatter.parseToCents("1,234.56"))
        assertEquals(0L, CurrencyFormatter.parseToCents(""))
        assertEquals(0L, CurrencyFormatter.parseToCents("abc"))
    }

    @Test
    fun testCurrencyFormatting() {
        val inr = Currencies.fromCode("INR")
        assertEquals("₹24,580", CurrencyFormatter.formatCents(2458000L, inr))
        assertEquals("₹250.50", CurrencyFormatter.formatCents(25050L, inr))
        assertEquals("-₹10,420", CurrencyFormatter.formatCents(-1042000L, inr))
        assertEquals("+₹35,000", CurrencyFormatter.formatCents(3500000L, inr, showSign = true))
        assertEquals("₹ ••••••", CurrencyFormatter.formatCents(2458000L, inr, hideValue = true))
    }

    @Test
    fun testGreeting() {
        val greeting = DateTimeUtils.getGreeting()
        assertNotNull(greeting)
        assertTrue(greeting.startsWith("Good "))
    }

    @Test
    fun testCsvExport() {
        val tx = TransactionEntity(
            id = 1L,
            type = "EXPENSE",
            amountCents = 25000L,
            category = "Food",
            date = "2026-10-16",
            time = "12:30 PM",
            timestamp = 1729000000000L,
            dateMillis = 1729000000000L,
            paymentMethod = "Cash",
            note = "Lunch",
            createdAt = 1729000000000L,
            updatedAt = 1729000000000L
        )
        assertEquals(25000L, tx.amount)
        val csv = CsvExporter.generateCsvString(listOf(tx), Currencies.defaultCurrency)
        assertTrue(csv.contains("Food"))
        assertTrue(csv.contains("EXPENSE"))
        assertTrue(csv.contains("Cash"))
        assertTrue(csv.contains("Lunch"))
    }

    @Test
    fun testSecurityHelperPinHashingAndVerification() {
        val salt = SecurityHelper.generateSalt()
        assertEquals(32, salt.length)

        val pin = "1234"
        val hash = SecurityHelper.hashPin(pin, salt)
        assertNotNull(hash)
        assertEquals(64, hash.length) // SHA-256 hex string is 64 chars

        // Correct PIN
        assertTrue(SecurityHelper.verifyPin("1234", hash, salt))

        // Incorrect PINs
        assertFalse(SecurityHelper.verifyPin("0000", hash, salt))
        assertFalse(SecurityHelper.verifyPin("1235", hash, salt))
        assertFalse(SecurityHelper.verifyPin("", hash, salt))
    }
}
