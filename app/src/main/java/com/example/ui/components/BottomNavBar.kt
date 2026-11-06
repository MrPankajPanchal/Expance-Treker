package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NavScreen(val title: String) {
    HOME("Home"),
    TRANSACTIONS("Transactions"),
    REPORTS("Reports"),
    SETTINGS("Settings")
}

@Composable
fun ExpenseBottomNavBar(
    currentScreen: NavScreen,
    onNavigate: (NavScreen) -> Unit,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home
                NavBarItem(
                    label = "Home",
                    selected = currentScreen == NavScreen.HOME,
                    selectedIcon = Icons.Default.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    testTag = "nav_home",
                    onClick = { onNavigate(NavScreen.HOME) },
                    modifier = Modifier.weight(1f)
                )

                // Transactions
                NavBarItem(
                    label = "Transactions",
                    selected = currentScreen == NavScreen.TRANSACTIONS,
                    selectedIcon = Icons.Default.ReceiptLong,
                    unselectedIcon = Icons.Outlined.ReceiptLong,
                    testTag = "nav_transactions",
                    onClick = { onNavigate(NavScreen.TRANSACTIONS) },
                    modifier = Modifier.weight(1f)
                )

                // Spacer for center plus button
                Spacer(modifier = Modifier.size(56.dp))

                // Reports
                NavBarItem(
                    label = "Reports",
                    selected = currentScreen == NavScreen.REPORTS,
                    selectedIcon = Icons.Default.BarChart,
                    unselectedIcon = Icons.Outlined.BarChart,
                    testTag = "nav_reports",
                    onClick = { onNavigate(NavScreen.REPORTS) },
                    modifier = Modifier.weight(1f)
                )

                // Settings
                NavBarItem(
                    label = "Settings",
                    selected = currentScreen == NavScreen.SETTINGS,
                    selectedIcon = Icons.Default.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    testTag = "nav_settings",
                    onClick = { onNavigate(NavScreen.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Prominent Center (+) Button
            FloatingActionButton(
                onClick = onPlusClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .size(54.dp)
                    .testTag("fab_add_transaction")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 28.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}
