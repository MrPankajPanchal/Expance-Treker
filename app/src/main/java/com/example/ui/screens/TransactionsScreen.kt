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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AppCurrency
import com.example.data.model.PaymentMethods
import com.example.util.DateTimeUtils

enum class TransactionSort(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_AMOUNT("Highest Amount"),
    LOWEST_AMOUNT("Lowest Amount")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    allTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currency: AppCurrency,
    hideBalance: Boolean,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<String?>("ALL") } // "ALL", "EXPENSE", "INCOME"
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedPaymentMethodFilter by remember { mutableStateOf<String?>(null) }
    var selectedSort by remember { mutableStateOf(TransactionSort.NEWEST) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val categoryMap = remember(categories) {
        categories.associateBy { it.name }
    }

    // Filter & Sort Logic
    val filteredTransactions = remember(
        allTransactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryFilter,
        selectedPaymentMethodFilter,
        selectedSort
    ) {
        allTransactions
            .filter { tx ->
                val matchesType = when (selectedTypeFilter) {
                    "EXPENSE" -> tx.type == "EXPENSE"
                    "INCOME" -> tx.type == "INCOME"
                    else -> true
                }
                val matchesCategory = selectedCategoryFilter == null || tx.category == selectedCategoryFilter
                val matchesPayment = selectedPaymentMethodFilter == null || tx.paymentMethod == selectedPaymentMethodFilter
                val matchesQuery = if (searchQuery.isBlank()) true else {
                    val q = searchQuery.trim().lowercase()
                    tx.category.lowercase().contains(q) ||
                            tx.note.lowercase().contains(q) ||
                            tx.paymentMethod.lowercase().contains(q) ||
                            (tx.amountCents / 100).toString().contains(q)
                }
                matchesType && matchesCategory && matchesPayment && matchesQuery
            }
            .let { list ->
                when (selectedSort) {
                    TransactionSort.NEWEST -> list.sortedWith(compareByDescending<TransactionEntity> { it.dateMillis }.thenByDescending { it.id })
                    TransactionSort.OLDEST -> list.sortedWith(compareBy<TransactionEntity> { it.dateMillis }.thenBy { it.id })
                    TransactionSort.HIGHEST_AMOUNT -> list.sortedByDescending { it.amountCents }
                    TransactionSort.LOWEST_AMOUNT -> list.sortedBy { it.amountCents }
                }
            }
    }

    // Grouping by date
    val groupedByDate = remember(filteredTransactions) {
        filteredTransactions.groupBy { DateTimeUtils.getDateGroupHeader(it.dateMillis) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen_content")
    ) {
        // Search & Filter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions, notes...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transactions_search_field")
                )

                // Sort Dropdown Button
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .testTag("sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        TransactionSort.values().forEach { sortOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = sortOption.label,
                                        fontWeight = if (selectedSort == sortOption) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedSort = sortOption
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Filter Sheet Toggle Button
                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (selectedCategoryFilter != null || selectedPaymentMethodFilter != null)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .testTag("filter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (selectedCategoryFilter != null || selectedPaymentMethodFilter != null)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Type Filter Chips (All, Expenses, Income)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val types = listOf("ALL" to "All", "EXPENSE" to "Expenses", "INCOME" to "Income")
                items(types) { (code, label) ->
                    val isSelected = selectedTypeFilter == code
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTypeFilter = code },
                        label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Active category chip
                if (selectedCategoryFilter != null) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("Category: $selectedCategoryFilter ✕") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Active payment chip
                if (selectedPaymentMethodFilter != null) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { selectedPaymentMethodFilter = null },
                            label = { Text("Payment: $selectedPaymentMethodFilter ✕") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Transactions grouped by Date
        if (groupedByDate.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No matching transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try adjusting your search or filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(onClick = {
                        searchQuery = ""
                        selectedTypeFilter = "ALL"
                        selectedCategoryFilter = null
                        selectedPaymentMethodFilter = null
                    }) {
                        Text("Reset Filters", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedByDate.forEach { (dateHeader, txList) ->
                    item {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }

                    items(txList, key = { it.id }) { tx ->
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
    }

    // Modal BottomSheet for Category and Payment Filters
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Transactions",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = {
                        selectedCategoryFilter = null
                        selectedPaymentMethodFilter = null
                        showFilterSheet = false
                    }) {
                        Text("Reset All")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Filter by Category",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat.name,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat.name) null else cat.name
                            },
                            label = { Text(cat.name) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Filter by Payment Method",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PaymentMethods.list) { method ->
                        FilterChip(
                            selected = selectedPaymentMethodFilter == method,
                            onClick = {
                                selectedPaymentMethodFilter = if (selectedPaymentMethodFilter == method) null else method
                            },
                            label = { Text(method) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
