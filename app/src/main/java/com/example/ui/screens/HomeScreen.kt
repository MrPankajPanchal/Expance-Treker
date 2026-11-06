package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AppCurrency
import com.example.ui.components.BalanceCard
import com.example.ui.components.CategoryAvatar
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    totalBalanceCents: Long,
    totalIncomeCents: Long,
    totalExpenseCents: Long,
    recentTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currency: AppCurrency,
    hideBalance: Boolean,
    onToggleHideBalance: () -> Unit,
    monthlyBudgetCents: Long,
    thisMonthSpentCents: Long,
    onSeeAllTransactions: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onAddTransactionClick: () -> Unit,
    isAppLockEnabled: Boolean,
    onLockApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember { DateTimeUtils.getGreeting() }
    val todayDateFormatted = remember {
        SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())
    }

    val categoryMap = remember(categories) {
        categories.associateBy { it.name }
    }

    // Budget Calculations
    val remainingBudgetCents = (monthlyBudgetCents - thisMonthSpentCents).coerceAtLeast(0L)
    val budgetProgress = if (monthlyBudgetCents > 0L) {
        (thisMonthSpentCents.toFloat() / monthlyBudgetCents.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val budgetPercent = (budgetProgress * 100).toInt()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 90.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Greeting Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = todayDateFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isAppLockEnabled) {
                    IconButton(
                        onClick = onLockApp,
                        modifier = Modifier
                            .testTag("lock_app_button")
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock App",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Large Balance Card
        item {
            BalanceCard(
                totalBalanceCents = totalBalanceCents,
                totalIncomeCents = totalIncomeCents,
                totalExpenseCents = totalExpenseCents,
                currency = currency,
                hideBalance = hideBalance,
                onToggleHideBalance = onToggleHideBalance
            )
        }

        // "This Month" Budget Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("this_month_budget_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "This Month",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$budgetPercent% used",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = when {
                                budgetPercent >= 100 -> ExpenseRed
                                budgetPercent >= 80 -> WarningAmber
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Linear Progress Indicator
                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = when {
                            budgetPercent >= 100 -> ExpenseRed
                            budgetPercent >= 80 -> WarningAmber
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCents(
                                    amountCents = thisMonthSpentCents,
                                    currency = currency,
                                    hideValue = hideBalance
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Monthly Budget",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCents(
                                    amountCents = monthlyBudgetCents,
                                    currency = currency,
                                    hideValue = hideBalance
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCents(
                                    amountCents = remainingBudgetCents,
                                    currency = currency,
                                    hideValue = hideBalance
                                ),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (remainingBudgetCents > 0L) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    // Warning Alert Badge if over 80% or 100%
                    if (budgetPercent >= 80) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (budgetPercent >= 100) ExpenseRed.copy(alpha = 0.12f)
                                    else WarningAmber.copy(alpha = 0.12f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (budgetPercent >= 100) ExpenseRed else WarningAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (budgetPercent >= 100) "You have reached 100% of your monthly budget!"
                                else "You've used $budgetPercent% of your monthly budget.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (budgetPercent >= 100) ExpenseRed else WarningAmber
                            )
                        }
                    }
                }
            }
        }

        // "Recent Transactions" Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (recentTransactions.isNotEmpty()) {
                    TextButton(
                        onClick = onSeeAllTransactions,
                        modifier = Modifier.testTag("see_all_transactions_button")
                    ) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Transactions List or Empty State
        if (recentTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_transactions_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Add your first income or expense to start tracking your money.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onAddTransactionClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_state_add_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Add Transaction", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(recentTransactions, key = { it.id }) { tx ->
                val cat = categoryMap[tx.category]
                val iconName = cat?.iconName ?: if (tx.type == "INCOME") "payments" else "receipt_long"
                val colorHex = cat?.colorHex ?: if (tx.type == "INCOME") "#10B981" else "#EF4444"

                TransactionListItem(
                    transaction = tx,
                    iconName = iconName,
                    colorHex = colorHex,
                    currency = currency,
                    hideBalance = hideBalance,
                    onClick = { onTransactionClick(tx) }
                )
            }
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: TransactionEntity,
    iconName: String,
    colorHex: String,
    currency: AppCurrency,
    hideBalance: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == "INCOME"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("transaction_item_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryAvatar(
                iconName = iconName,
                colorHex = colorHex,
                size = 46.dp,
                iconSize = 22.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.paymentMethod,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = " • ${transaction.note}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatCents(
                        amountCents = if (isIncome) transaction.amountCents else -transaction.amountCents,
                        currency = currency,
                        showSign = true,
                        hideValue = hideBalance
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DateTimeUtils.formatTime(transaction.dateMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
