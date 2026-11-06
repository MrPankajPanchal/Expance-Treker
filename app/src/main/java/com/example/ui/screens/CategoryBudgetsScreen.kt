package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AppCurrency
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.CategoryIcons
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBudgetsScreen(
    categories: List<CategoryEntity>,
    allTransactions: List<TransactionEntity>,
    currency: AppCurrency,
    startDayOfMonth: Int,
    onUpdateCategoryBudget: (categoryId: Long, budgetCents: Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expenseCategories = remember(categories) {
        categories.filter { it.type == "EXPENSE" }
    }

    val (startMillis, endMillis) = remember(startDayOfMonth) {
        DateTimeUtils.getStartAndEndOfMonth(startDayOfMonth = startDayOfMonth)
    }

    val thisMonthCategorySpent = remember(allTransactions, startMillis, endMillis) {
        allTransactions
            .filter { it.type == "EXPENSE" && it.dateMillis in startMillis..endMillis }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amountCents } }
    }

    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var budgetInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Category Budgets", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Set individual monthly spending limits for each category to keep expenses under control.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(expenseCategories, key = { it.id }) { cat ->
                val spentCents = thisMonthCategorySpent[cat.name] ?: 0L
                val budgetCents = cat.monthlyBudgetCents
                val progress = if (budgetCents > 0L) (spentCents.toFloat() / budgetCents.toFloat()).coerceIn(0f, 1f) else 0f
                val pct = if (budgetCents > 0L) ((spentCents.toDouble() / budgetCents.toDouble()) * 100).toInt() else 0
                val remaining = (budgetCents - spentCents).coerceAtLeast(0L)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingCategory = cat
                            budgetInput = if (cat.monthlyBudgetCents > 0L) CurrencyFormatter.formatNumberOnly(cat.monthlyBudgetCents) else ""
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryAvatar(
                                iconName = cat.iconName,
                                colorHex = cat.colorHex,
                                size = 42.dp,
                                iconSize = 20.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (budgetCents > 0L) {
                                        "${CurrencyFormatter.formatCents(spentCents, currency)} of ${CurrencyFormatter.formatCents(budgetCents, currency)}"
                                    } else "No budget set",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                editingCategory = cat
                                budgetInput = if (cat.monthlyBudgetCents > 0L) CurrencyFormatter.formatNumberOnly(cat.monthlyBudgetCents) else ""
                            }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit budget", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }

                        if (budgetCents > 0L) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = when {
                                    pct >= 100 -> ExpenseRed
                                    pct >= 80 -> WarningAmber
                                    else -> CategoryIcons.parseColor(cat.colorHex)
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$pct% spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Remaining: ${CurrencyFormatter.formatCents(remaining, currency)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (remaining > 0L) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Budget Dialog
    if (editingCategory != null) {
        val cat = editingCategory!!
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Budget for ${cat.name}") },
            text = {
                Column {
                    Text(
                        text = "Enter monthly budget for this category (enter 0 to remove):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { c -> c.isDigit() } },
                        leadingIcon = {
                            Text(currency.symbol, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cents = CurrencyFormatter.parseToCents(budgetInput)
                        onUpdateCategoryBudget(cat.id, cents)
                        editingCategory = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
