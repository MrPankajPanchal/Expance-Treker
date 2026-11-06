package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.local.TransactionEntity
import com.example.data.repository.ExpenseRepository
import com.example.ui.components.ExpenseBottomNavBar
import com.example.ui.components.NavScreen
import com.example.ui.components.PinLockScreen
import com.example.ui.screens.AddTransactionScreen
import com.example.ui.screens.CategoryBudgetsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseTrackerTheme
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.ui.viewmodel.ExpenseViewModelFactory
import com.example.util.DateTimeUtils
import com.example.util.SecurityHelper
import com.example.util.SecurityStorage
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var viewModel: ExpenseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ExpenseRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            settingDao = database.settingDao()
        )
        val securityStorage = SecurityStorage(applicationContext)
        val factory = ExpenseViewModelFactory(repository, database, securityStorage)
        viewModel = ViewModelProvider(this, factory)[ExpenseViewModel::class.java]

        setContent {
            val themePref by viewModel.themePreference.collectAsStateWithLifecycle()

            ExpenseTrackerTheme(themePreference = themePref) {
                ExpenseTrackerApp(
                    activity = this,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppForegrounded()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(
    activity: FragmentActivity,
    viewModel: ExpenseViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State from ViewModel (Derived from Room DB as single source of truth)
    val isFirstLaunchCompleted by viewModel.isFirstLaunchCompleted.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val autoLockTimeout by viewModel.autoLockTimeout.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val monthlyBudgetCents by viewModel.monthlyBudgetCents.collectAsStateWithLifecycle()
    val startDayOfMonth by viewModel.startDayOfMonth.collectAsStateWithLifecycle()
    val isHideBalance by viewModel.isHideBalance.collectAsStateWithLifecycle()
    val themePref by viewModel.themePreference.collectAsStateWithLifecycle()

    // Navigation and Modal State
    var currentScreen by remember { mutableStateOf(NavScreen.HOME) }
    var isAddTransactionOpen by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var addTransactionType by remember { mutableStateOf("EXPENSE") }
    var showQuickAddChoiceSheet by remember { mutableStateOf(false) }
    var isCategoryBudgetsOpen by remember { mutableStateOf(false) }

    // Month calculations derived strictly from persistent database records
    val (monthStart, monthEnd) = remember(startDayOfMonth) {
        DateTimeUtils.getStartAndEndOfMonth(startDayOfMonth = startDayOfMonth)
    }
    val thisMonthSpentCents = remember(allTransactions, monthStart, monthEnd) {
        allTransactions.filter { it.type == "EXPENSE" && it.dateMillis in monthStart..monthEnd }
            .sumOf { it.amountCents }
    }
    val totalIncomeCents = remember(allTransactions) {
        allTransactions.filter { it.type == "INCOME" }.sumOf { it.amountCents }
    }
    val totalExpenseCents = remember(allTransactions) {
        allTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amountCents }
    }
    val totalBalanceCents = totalIncomeCents - totalExpenseCents

    // Intercept Back Press
    BackHandler(enabled = isCategoryBudgetsOpen || isAddTransactionOpen || currentScreen != NavScreen.HOME) {
        when {
            isCategoryBudgetsOpen -> isCategoryBudgetsOpen = false
            isAddTransactionOpen -> {
                isAddTransactionOpen = false
                editingTransaction = null
            }
            currentScreen != NavScreen.HOME -> currentScreen = NavScreen.HOME
        }
    }

    // 1. First Launch Flow (Welcome Screen -> Security Setup -> Preferences)
    if (!isFirstLaunchCompleted) {
        OnboardingScreen(
            onComplete = { selectedCurrency, budgetCents, selectedTheme, pin, enableBio ->
                viewModel.completeFirstLaunch(
                    currencyCode = selectedCurrency,
                    monthlyBudgetCents = budgetCents,
                    theme = selectedTheme,
                    pin = pin,
                    enableBiometric = enableBio
                )
            }
        )
        return
    }

    // 2. App Lock / Authentication Screen
    if (isAppLockEnabled && !isUnlocked) {
        val isBioSupported = remember { SecurityHelper.isBiometricAvailable(activity) }

        // Automatically prompt biometrics on launch if enabled and supported
        LaunchedEffect(Unit) {
            if (isBiometricEnabled && isBioSupported) {
                SecurityHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = { viewModel.unlockApp() },
                    onError = { /* Let user enter PIN fallback */ }
                )
            }
        }

        PinLockScreen(
            onVerifyPin = { pin -> viewModel.verifyEnteredPin(pin) },
            onUnlocked = { viewModel.unlockApp() },
            isBiometricEnabled = isBiometricEnabled && isBioSupported,
            onTriggerBiometrics = {
                SecurityHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = { viewModel.unlockApp() },
                    onError = { msg -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show() }
                )
            },
            onResetAppData = {
                viewModel.resetAppSecurityAndData()
                Toast.makeText(context, "App security and data reset successfully", Toast.LENGTH_LONG).show()
            }
        )
        return
    }

    // 3. Category Budgets Screen
    if (isCategoryBudgetsOpen) {
        CategoryBudgetsScreen(
            categories = categories,
            allTransactions = allTransactions,
            currency = currency,
            startDayOfMonth = startDayOfMonth,
            onUpdateCategoryBudget = { id, budget ->
                viewModel.updateCategoryBudget(id, budget)
                coroutineScope.launch { snackbarHostState.showSnackbar("Category budget updated") }
            },
            onNavigateBack = { isCategoryBudgetsOpen = false }
        )
        return
    }

    // 4. Add or Edit Transaction Screen (Validated Database Persistence)
    if (isAddTransactionOpen) {
        AddTransactionScreen(
            currency = currency,
            categories = categories,
            editingTransaction = editingTransaction,
            initialType = addTransactionType,
            onSaveTransaction = { id, type, amountCents, category, dateMillis, paymentMethod, note ->
                viewModel.saveTransaction(
                    id = id,
                    type = type,
                    amountCents = amountCents,
                    category = category,
                    dateMillis = dateMillis,
                    paymentMethod = paymentMethod,
                    note = note,
                    createdAt = editingTransaction?.createdAt,
                    context = context
                )
            },
            onDeleteTransaction = { id ->
                viewModel.deleteTransaction(id)
            },
            onAddCustomCategory = { name, type, iconName, colorHex ->
                viewModel.addCustomCategory(name, type, iconName, colorHex)
                coroutineScope.launch { snackbarHostState.showSnackbar("Category '$name' created") }
            },
            onNavigateBack = {
                isAddTransactionOpen = false
                editingTransaction = null
            }
        )
        return
    }

    // 5. Main Dashboard Scaffold with Bottom Navigation
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            ExpenseBottomNavBar(
                currentScreen = currentScreen,
                onNavigate = { screen -> currentScreen = screen },
                onPlusClick = { showQuickAddChoiceSheet = true }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "main_screen_transition"
        ) { screen ->
            when (screen) {
                NavScreen.HOME -> {
                    HomeScreen(
                        totalBalanceCents = totalBalanceCents,
                        totalIncomeCents = totalIncomeCents,
                        totalExpenseCents = totalExpenseCents,
                        recentTransactions = recentTransactions,
                        categories = categories,
                        currency = currency,
                        hideBalance = isHideBalance,
                        onToggleHideBalance = { viewModel.toggleHideBalance() },
                        monthlyBudgetCents = monthlyBudgetCents,
                        thisMonthSpentCents = thisMonthSpentCents,
                        onSeeAllTransactions = { currentScreen = NavScreen.TRANSACTIONS },
                        onTransactionClick = { tx ->
                            editingTransaction = tx
                            isAddTransactionOpen = true
                        },
                        onAddTransactionClick = {
                            editingTransaction = null
                            addTransactionType = "EXPENSE"
                            isAddTransactionOpen = true
                        },
                        isAppLockEnabled = isAppLockEnabled,
                        onLockApp = { viewModel.lockApp() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                NavScreen.TRANSACTIONS -> {
                    TransactionsScreen(
                        allTransactions = allTransactions,
                        categories = categories,
                        currency = currency,
                        hideBalance = isHideBalance,
                        onTransactionClick = { tx ->
                            editingTransaction = tx
                            isAddTransactionOpen = true
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                NavScreen.REPORTS -> {
                    ReportsScreen(
                        allTransactions = allTransactions,
                        categories = categories,
                        currency = currency,
                        hideBalance = isHideBalance,
                        startDayOfMonth = startDayOfMonth,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                NavScreen.SETTINGS -> {
                    SettingsScreen(
                        currency = currency,
                        themePreference = themePref,
                        monthlyBudgetCents = monthlyBudgetCents,
                        startDayOfMonth = startDayOfMonth,
                        isAppLockEnabled = isAppLockEnabled,
                        isBiometricEnabled = isBiometricEnabled,
                        autoLockTimeout = autoLockTimeout,
                        onSelectCurrency = { viewModel.setCurrency(it) },
                        onSelectTheme = { viewModel.setTheme(it) },
                        onSetMonthlyBudget = { viewModel.setMonthlyBudget(it) },
                        onSetStartDayOfMonth = { viewModel.setStartDayOfMonth(it) },
                        onSetAppLock = { enabled, pin -> viewModel.setAppLock(enabled, pin) },
                        onSetBiometricEnabled = { viewModel.setBiometricEnabled(it) },
                        onSetAutoLockTimeout = { viewModel.setAutoLockTimeout(it) },
                        onOpenCategoryBudgets = { isCategoryBudgetsOpen = true },
                        onExportCsv = { viewModel.exportCsv(context) },
                        onCreateBackup = { viewModel.createBackupJson() },
                        onRestoreBackup = { json ->
                            viewModel.restoreBackupJson(
                                jsonString = json,
                                onSuccess = {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Backup restored successfully!") }
                                },
                                onError = { errorMsg ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Restore error: $errorMsg") }
                                }
                            )
                        },
                        onClearAllData = { viewModel.clearAllData() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // Center (+) Quick Add Choice Sheet
    if (showQuickAddChoiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickAddChoiceSheet = false },
            sheetState = rememberModalBottomSheetState(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add New Record",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Add Expense
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(ExpenseRed.copy(alpha = 0.12f))
                            .clickable {
                                showQuickAddChoiceSheet = false
                                editingTransaction = null
                                addTransactionType = "EXPENSE"
                                isAddTransactionOpen = true
                            }
                            .padding(vertical = 20.dp, horizontal = 16.dp)
                            .testTag("quick_add_expense_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(ExpenseRed.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Add Expense",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ExpenseRed
                            )
                        }
                    }

                    // Add Income
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(IncomeGreen.copy(alpha = 0.12f))
                            .clickable {
                                showQuickAddChoiceSheet = false
                                editingTransaction = null
                                addTransactionType = "INCOME"
                                isAddTransactionOpen = true
                            }
                            .padding(vertical = 20.dp, horizontal = 16.dp)
                            .testTag("quick_add_income_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(IncomeGreen.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Add Income",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = IncomeGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
