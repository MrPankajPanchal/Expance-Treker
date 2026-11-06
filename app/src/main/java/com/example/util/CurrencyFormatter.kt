package com.example.util

import com.example.data.model.AppCurrency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    fun formatCents(
        amountCents: Long,
        currency: AppCurrency,
        showSign: Boolean = false,
        hideValue: Boolean = false
    ): String {
        if (hideValue) {
            return "${currency.symbol} ••••••"
        }

        val isNegative = amountCents < 0
        val absCents = abs(amountCents)
        val major = absCents / 100
        val minor = absCents % 100

        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val formatter = DecimalFormat("#,##0", symbols)
        val formattedMajor = formatter.format(major)

        val formattedAmount = if (minor > 0) {
            "$formattedMajor.${minor.toString().padStart(2, '0')}"
        } else {
            formattedMajor
        }

        val prefix = when {
            isNegative -> "-"
            showSign && amountCents > 0 -> "+"
            else -> ""
        }

        return "$prefix${currency.symbol}$formattedAmount"
    }

    fun parseToCents(input: String): Long {
        val sanitized = input.trim().replace(",", "")
        if (sanitized.isEmpty()) return 0L
        return try {
            val parts = sanitized.split(".")
            val major = parts[0].toLongOrNull() ?: 0L
            val minor = if (parts.size > 1) {
                val decString = parts[1].take(2).padEnd(2, '0')
                decString.toLongOrNull() ?: 0L
            } else {
                0L
            }
            (major * 100) + minor
        } catch (e: Exception) {
            0L
        }
    }

    fun formatNumberOnly(amountCents: Long): String {
        val absCents = abs(amountCents)
        val major = absCents / 100
        val minor = absCents % 100
        return if (minor > 0) {
            "$major.${minor.toString().padStart(2, '0')}"
        } else {
            "$major"
        }
    }
}
