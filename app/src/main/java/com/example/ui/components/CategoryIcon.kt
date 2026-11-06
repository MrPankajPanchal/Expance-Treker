package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object CategoryIcons {

    val availableIcons = listOf(
        "restaurant" to Icons.Default.Restaurant,
        "fastfood" to Icons.Default.Fastfood,
        "shopping_bag" to Icons.Default.ShoppingBag,
        "directions_bus" to Icons.Default.DirectionsBus,
        "flight" to Icons.Default.Flight,
        "receipt_long" to Icons.Default.ReceiptLong,
        "school" to Icons.Default.School,
        "medical_services" to Icons.Default.MedicalServices,
        "local_hospital" to Icons.Default.LocalHospital,
        "movie" to Icons.Default.Movie,
        "sports_esports" to Icons.Default.SportsEsports,
        "local_grocery_store" to Icons.Default.LocalGroceryStore,
        "home" to Icons.Default.Home,
        "apartment" to Icons.Default.Apartment,
        "payments" to Icons.Default.Payments,
        "storefront" to Icons.Default.Storefront,
        "business_center" to Icons.Default.BusinessCenter,
        "laptop" to Icons.Default.Laptop,
        "work" to Icons.Default.Work,
        "card_giftcard" to Icons.Default.CardGiftcard,
        "trending_up" to Icons.Default.TrendingUp,
        "savings" to Icons.Default.Savings,
        "account_balance_wallet" to Icons.Default.AccountBalanceWallet,
        "attach_money" to Icons.Default.AttachMoney,
        "category" to Icons.Default.Category
    )

    val availableColors = listOf(
        "#F59E0B", "#EC4899", "#3B82F6", "#EF4444", "#8B5CF6",
        "#10B981", "#F97316", "#14B8A6", "#6366F1", "#06B6D4",
        "#84CC16", "#D946EF", "#64748B"
    )

    fun getIcon(name: String): ImageVector {
        return availableIcons.find { it.first.equals(name, ignoreCase = true) }?.second
            ?: Icons.Default.Category
    }

    fun parseColor(hex: String, default: Color = Color(0xFF64748B)): Color {
        return try {
            val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
            Color(android.graphics.Color.parseColor(cleanHex))
        } catch (e: Exception) {
            default
        }
    }
}

@Composable
fun CategoryAvatar(
    iconName: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    val bgColor = CategoryIcons.parseColor(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .background(bgColor.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryIcons.getIcon(iconName),
            contentDescription = null,
            tint = bgColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
