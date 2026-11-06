package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AppCurrency
import com.example.data.model.PaymentMethods
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.CategoryIcons
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(
    currency: AppCurrency,
    categories: List<CategoryEntity>,
    editingTransaction: TransactionEntity? = null,
    initialType: String = "EXPENSE",
    onSaveTransaction: suspend (
        id: Long?,
        type: String,
        amountCents: Long,
        category: String,
        dateMillis: Long,
        paymentMethod: String,
        note: String
    ) -> Result<Long>,
    onDeleteTransaction: (suspend (Long) -> Result<Unit>)? = null,
    onAddCustomCategory: (name: String, type: String, iconName: String, colorHex: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedType by remember {
        mutableStateOf(editingTransaction?.type ?: initialType)
    }

    var amountInput by remember {
        mutableStateOf(
            if (editingTransaction != null) CurrencyFormatter.formatNumberOnly(editingTransaction.amountCents)
            else ""
        )
    }

    val availableCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    var selectedCategory by remember {
        mutableStateOf(
            editingTransaction?.category ?: availableCategories.firstOrNull()?.name ?: "Other"
        )
    }

    var transactionDateMillis by remember {
        mutableLongStateOf(editingTransaction?.dateMillis ?: System.currentTimeMillis())
    }

    var selectedPaymentMethod by remember {
        mutableStateOf(editingTransaction?.paymentMethod ?: PaymentMethods.default)
    }

    var noteInput by remember {
        mutableStateOf(editingTransaction?.note ?: "")
    }

    // Saving and Error States
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Date and Time pickers
    val calendar = remember(transactionDateMillis) {
        Calendar.getInstance().apply { timeInMillis = transactionDateMillis }
    }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val updatedCal = Calendar.getInstance().apply {
                    timeInMillis = transactionDateMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                transactionDateMillis = updatedCal.timeInMillis
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val updatedCal = Calendar.getInstance().apply {
                    timeInMillis = transactionDateMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                transactionDateMillis = updatedCal.timeInMillis
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
    }

    val isExpense = selectedType == "EXPENSE"
    val accentColor = if (isExpense) ExpenseRed else IncomeGreen
    val validAmount = CurrencyFormatter.parseToCents(amountInput) > 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editingTransaction != null) "Edit Transaction"
                        else if (isExpense) "Add Expense" else "Add Income",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isSaving,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (editingTransaction != null && onDeleteTransaction != null) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            enabled = !isSaving && !isDeleting,
                            modifier = Modifier.testTag("delete_transaction_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ExpenseRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (isSaving) return@Button // Prevent duplicate rapid taps
                            val cents = CurrencyFormatter.parseToCents(amountInput)
                            if (cents <= 0L) {
                                errorMessage = "Please enter a valid amount."
                                return@Button
                            }

                            isSaving = true
                            errorMessage = null

                            coroutineScope.launch {
                                val result = onSaveTransaction(
                                    editingTransaction?.id,
                                    selectedType,
                                    cents,
                                    selectedCategory,
                                    transactionDateMillis,
                                    selectedPaymentMethod,
                                    noteInput
                                )

                                if (result.isSuccess) {
                                    val successMsg = if (editingTransaction != null) "Transaction updated successfully"
                                    else if (isExpense) "Expense saved successfully" else "Income saved successfully"
                                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                } else {
                                    isSaving = false
                                    errorMessage = "Couldn’t save transaction. Please try again."
                                }
                            }
                        },
                        enabled = validAmount && !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_transaction_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Saving to Database...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = if (editingTransaction != null) "Save Changes"
                                else if (isExpense) "Save Expense" else "Save Income",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Error banner if save failed (preserves all form inputs)
            AnimatedVisibility(visible = errorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ExpenseRed.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = ExpenseRed
                    )
                }
            }

            // Transaction Type Toggle (Expense / Income)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expense tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isExpense) ExpenseRed else Color.Transparent)
                        .clickable(enabled = !isSaving) {
                            selectedType = "EXPENSE"
                            val expCats = categories.filter { it.type == "EXPENSE" }
                            if (expCats.none { it.name == selectedCategory }) {
                                selectedCategory = expCats.firstOrNull()?.name ?: "Other"
                            }
                        }
                        .padding(vertical = 12.dp)
                        .testTag("type_expense_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Income tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isExpense) IncomeGreen else Color.Transparent)
                        .clickable(enabled = !isSaving) {
                            selectedType = "INCOME"
                            val incCats = categories.filter { it.type == "INCOME" }
                            if (incCats.none { it.name == selectedCategory }) {
                                selectedCategory = incCats.firstOrNull()?.name ?: "Salary"
                            }
                        }
                        .padding(vertical = 12.dp)
                        .testTag("type_income_tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (!isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Large Amount Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AMOUNT",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() || it == '.' }
                                if (clean.count { it == '.' } <= 1) {
                                    amountInput = clean
                                    errorMessage = null
                                }
                            },
                            placeholder = {
                                Text(
                                    "0",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            },
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            enabled = !isSaving,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .width(220.dp)
                                .testTag("amount_input_field")
                        )
                    }
                }
            }

            // Category Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { showAddCategoryDialog = true },
                        enabled = !isSaving,
                        modifier = Modifier.testTag("add_custom_category_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableCategories.forEach { cat ->
                        val isSelected = selectedCategory == cat.name
                        val catColor = CategoryIcons.parseColor(cat.colorHex)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) catColor.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) catColor else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = !isSaving) { selectedCategory = cat.name }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            CategoryAvatar(
                                iconName = cat.iconName,
                                colorHex = cat.colorHex,
                                size = 28.dp,
                                iconSize = 16.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Date and Time Pickers
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Date & Time",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                            .clickable(enabled = !isSaving) { datePickerDialog.show() }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("date_picker_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = DateTimeUtils.formatDate(transactionDateMillis),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Time
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                            .clickable(enabled = !isSaving) { timePickerDialog.show() }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("time_picker_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = DateTimeUtils.formatTime(transactionDateMillis),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Payment Method
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethods.list.forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (!isSaving) selectedPaymentMethod = method },
                            label = { Text(method, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Optional Note
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Note (Optional)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("e.g. Lunch with team, Groceries") },
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_input_field"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirmDialog = false },
            title = { Text("Delete this transaction?") },
            text = { Text("This transaction will be permanently deleted from the database and all dashboard/report calculations will update immediately.") },
            confirmButton = {
                Button(
                    onClick = {
                        val txId = editingTransaction?.id ?: return@Button
                        isDeleting = true
                        coroutineScope.launch {
                            val res = onDeleteTransaction?.invoke(txId)
                            if (res?.isSuccess == true) {
                                Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                                showDeleteConfirmDialog = false
                                onNavigateBack()
                            } else {
                                isDeleting = false
                                Toast.makeText(context, "Failed to delete transaction. Try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    enabled = !isDeleting,
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Delete", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Custom Category Dialog
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var selectedIconName by remember { mutableStateOf("category") }
        var selectedColorHex by remember { mutableStateOf("#F59E0B") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Create Custom Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Select Icon", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcons.availableIcons.take(12).forEach { (iconKey, iconVec) ->
                            val isIconSel = selectedIconName == iconKey
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isIconSel) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedIconName = iconKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVec,
                                    contentDescription = iconKey,
                                    tint = if (isIconSel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Text("Select Color", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryIcons.availableColors.forEach { colorHex ->
                            val isColorSel = selectedColorHex == colorHex
                            val color = CategoryIcons.parseColor(colorHex)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isColorSel) 2.5.dp else 0.dp,
                                        color = if (isColorSel) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = colorHex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onAddCustomCategory(newCatName.trim(), selectedType, selectedIconName, selectedColorHex)
                            selectedCategory = newCatName.trim()
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCatName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
