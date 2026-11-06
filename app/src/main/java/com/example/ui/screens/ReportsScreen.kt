package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AppCurrency
import com.example.data.model.TimePeriod
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.CategoryExpenseItem
import com.example.ui.components.CategoryIcons
import com.example.ui.components.DonutPieChart
import com.example.ui.components.IncomeVsExpenseComparison
import com.example.ui.components.MonthlySpendingBarChart
import com.example.ui.components.SpendingBarData
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ReportsScreen(
    allTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currency: AppCurrency,
    hideBalance: Boolean,
    startDayOfMonth: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(TimePeriod.THIS_MONTH) }

    // Custom date range state
    var customStartMillis by remember {
        mutableLongStateOf(
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        )
    }
    var customEndMillis by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }

    val (startTimeMillis, endTimeMillis) = remember(selectedPeriod, startDayOfMonth, customStartMillis, customEndMillis) {
        when (selectedPeriod) {
            TimePeriod.THIS_WEEK -> DateTimeUtils.getStartAndEndOfWeek()
            TimePeriod.THIS_MONTH -> DateTimeUtils.getStartAndEndOfMonth(startDayOfMonth = startDayOfMonth)
            TimePeriod.LAST_MONTH -> DateTimeUtils.getStartAndEndOfLastMonth(startDayOfMonth = startDayOfMonth)
            TimePeriod.CUSTOM -> Pair(customStartMillis, customEndMillis)
        }
    }

    // Filter transactions for period
    val periodTransactions = remember(allTransactions, startTimeMillis, endTimeMillis) {
        allTransactions.filter { it.dateMillis in startTimeMillis..endTimeMillis }
    }

    val categoryMap = remember(categories) {
        categories.associateBy { it.name }
    }

    // Metrics
    val totalIncomeCents = remember(periodTransactions) {
        periodTransactions.filter { it.type == "INCOME" }.sumOf { it.amountCents }
    }
    val totalExpenseCents = remember(periodTransactions) {
        periodTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amountCents }
    }
    val netSavingsCents = totalIncomeCents - totalExpenseCents
    val savingsRate = if (totalIncomeCents > 0L) {
        ((netSavingsCents.toDouble() / totalIncomeCents.toDouble()) * 100).toInt()
    } else 0

    // Expense by Category items
    val categoryExpenseItems = remember(periodTransactions, totalExpenseCents, categories) {
        if (totalExpenseCents <= 0L) emptyList()
        else {
            val expenseTx = periodTransactions.filter { it.type == "EXPENSE" }
            val grouped = expenseTx.groupBy { it.category }
            grouped.map { (catName, txList) ->
                val sum = txList.sumOf { it.amountCents }
                val pct = ((sum.toDouble() / totalExpenseCents.toDouble()) * 100).toFloat()
                val cat = categoryMap[catName]
                val colorHex = cat?.colorHex ?: "#64748B"
                val iconName = cat?.iconName ?: "receipt_long"
                CategoryExpenseItem(
                    categoryName = catName,
                    iconName = iconName,
                    color = CategoryIcons.parseColor(colorHex),
                    amountCents = sum,
                    percentage = pct
                )
            }.sortedByDescending { it.amountCents }
        }
    }

    // Spending Bar Data (Daily or Weekly buckets)
    val spendingBarData = remember(periodTransactions, startTimeMillis, endTimeMillis) {
        val daysCount = ((endTimeMillis - startTimeMillis) / (24 * 60 * 60 * 1000)).coerceAtLeast(1)
        val expenseTx = periodTransactions.filter { it.type == "EXPENSE" }

        if (daysCount <= 14) {
            // Daily breakdown
            val sdf = SimpleDateFormat("EEE d", Locale.getDefault())
            val buckets = mutableMapOf<String, Long>()
            val cal = Calendar.getInstance().apply { timeInMillis = startTimeMillis }
            while (cal.timeInMillis <= endTimeMillis) {
                buckets[sdf.format(cal.time)] = 0L
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            for (tx in expenseTx) {
                val key = sdf.format(Date(tx.dateMillis))
                if (buckets.containsKey(key)) {
                    buckets[key] = (buckets[key] ?: 0L) + tx.amountCents
                }
            }
            buckets.entries.toList().takeLast(7).map { SpendingBarData(it.key, it.value) }
        } else {
            // 4-Week breakdown
            val step = (endTimeMillis - startTimeMillis) / 4
            (0 until 4).map { i ->
                val bStart = startTimeMillis + (i * step)
                val bEnd = bStart + step
                val sum = expenseTx.filter { it.dateMillis in bStart..bEnd }.sumOf { it.amountCents }
                SpendingBarData("W${i + 1}", sum)
            }
        }
    }

    // Offline Spending Insights
    val insights = remember(categoryExpenseItems, totalExpenseCents, totalIncomeCents, allTransactions, startDayOfMonth) {
        val list = mutableListOf<String>()

        if (categoryExpenseItems.isNotEmpty()) {
            val topCategory = categoryExpenseItems.first()
            list.add("You spent the most on ${topCategory.categoryName} (${topCategory.percentage.toInt()}% of total expenses).")

            if (categoryExpenseItems.size > 1) {
                val second = categoryExpenseItems[1]
                list.add("${second.categoryName} represents ${second.percentage.toInt()}% of your expenses.")
            }
        }

        // Compare with last month spending if viewing This Month
        val (lastStart, lastEnd) = DateTimeUtils.getStartAndEndOfLastMonth(startDayOfMonth)
        val lastMonthExpenses = allTransactions.filter { it.type == "EXPENSE" && it.dateMillis in lastStart..lastEnd }
            .sumOf { it.amountCents }

        if (lastMonthExpenses > 0L && totalExpenseCents > 0L) {
            val diff = totalExpenseCents - lastMonthExpenses
            val diffPct = (abs(diff.toDouble()) / lastMonthExpenses.toDouble() * 100).toInt()
            if (diff < 0) {
                list.add("Your spending is $diffPct% lower than last month.")
            } else if (diff > 0) {
                list.add("Your spending is $diffPct% higher than last month.")
            }
        }

        if (totalIncomeCents > 0L && netSavingsCents > 0L) {
            val pct = ((netSavingsCents.toDouble() / totalIncomeCents.toDouble()) * 100).toInt()
            list.add("You saved $pct% of your total income this period.")
        }

        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen_content"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 90.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Text(
                text = "Financial Reports",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Time Period Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimePeriod.values().forEach { period ->
                    val isSelected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (period) {
                                TimePeriod.THIS_WEEK -> "Week"
                                TimePeriod.THIS_MONTH -> "Month"
                                TimePeriod.LAST_MONTH -> "Last Mo"
                                TimePeriod.CUSTOM -> "Custom"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Custom Date Range Selector (if selected)
        if (selectedPeriod == TimePeriod.CUSTOM) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customStartMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                    customStartMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(DateTimeUtils.formatDate(customStartMillis), style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customEndMillis }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                                    customEndMillis = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(DateTimeUtils.formatDate(customEndMillis), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Summary Card: Income, Expenses, Net Savings
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("reports_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Income",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCents(totalIncomeCents, currency, hideValue = hideBalance),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = IncomeGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Expenses",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCents(totalExpenseCents, currency, hideValue = hideBalance),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRed
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Net Savings",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCents(netSavingsCents, currency, hideValue = hideBalance),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (netSavingsCents >= 0L) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    IncomeVsExpenseComparison(
                        incomeCents = totalIncomeCents,
                        expenseCents = totalExpenseCents,
                        currency = currency
                    )
                }
            }
        }

        // Spending Insights Card
        if (insights.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Spending Insights",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        insights.forEach { insight ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = insight,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expense by Category Donut Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("category_donut_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Expense by Category",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DonutPieChart(
                        items = categoryExpenseItems,
                        totalExpenseCents = totalExpenseCents,
                        currency = currency
                    )
                }
            }
        }

        // Monthly Spending Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Spending Trend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    MonthlySpendingBarChart(
                        data = spendingBarData,
                        currency = currency
                    )
                }
            }
        }

        // Category Breakdown Detailed List
        if (categoryExpenseItems.isNotEmpty()) {
            item {
                Text(
                    text = "Category Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(categoryExpenseItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryAvatar(
                                iconName = item.iconName,
                                colorHex = String.format("#%06X", (0xFFFFFF and item.color.value.toInt())),
                                size = 40.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.categoryName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.percentage.toInt()}% of total expenses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatCents(item.amountCents, currency, hideValue = hideBalance),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { item.percentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = item.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
