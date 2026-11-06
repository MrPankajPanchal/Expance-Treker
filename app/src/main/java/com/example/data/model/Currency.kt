package com.example.data.model

data class AppCurrency(
    val code: String,
    val symbol: String,
    val name: String
)

object Currencies {
    val list = listOf(
        AppCurrency("INR", "₹", "Indian Rupee (INR)"),
        AppCurrency("USD", "$", "US Dollar (USD)"),
        AppCurrency("EUR", "€", "Euro (EUR)"),
        AppCurrency("GBP", "£", "British Pound (GBP)"),
        AppCurrency("JPY", "¥", "Japanese Yen (JPY)"),
        AppCurrency("AUD", "A$", "Australian Dollar (AUD)"),
        AppCurrency("CAD", "C$", "Canadian Dollar (CAD)"),
        AppCurrency("SGD", "S$", "Singapore Dollar (SGD)"),
        AppCurrency("AED", "د.إ", "UAE Dirham (AED)")
    )

    val defaultCurrency = list.first()

    fun fromCode(code: String): AppCurrency {
        return list.find { it.code.equals(code, ignoreCase = true) } ?: defaultCurrency
    }
}
