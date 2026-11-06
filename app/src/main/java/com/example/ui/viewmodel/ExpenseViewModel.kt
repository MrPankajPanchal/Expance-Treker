package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.AppCurrency
import com.example.data.model.Currencies
import com.example.data.repository.ExpenseRepository
import com.example.util.CsvExporter
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.NotificationHelper
import com.example.util.SecurityHelper
import com.example.util.SecurityStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val database: AppDatabase,
    private val securityStorage: SecurityStorage
) : ViewModel() {

    // Central observable transactions flow - the single source of truth from Room SQLite
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.getRecentTransactions(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Settings
    private val settingsMap: StateFlow<Map<String, String>> = repository.allSettings
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currency: StateFlow<AppCurrency> = settingsMap.map { map ->
        val code = map["currency_code"] ?: "INR"
        Currencies.fromCode(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Currencies.defaultCurrency)

    val monthlyBudgetCents: StateFlow<Long> = settingsMap.map { map ->
        map["monthly_budget_cents"]?.toLongOrNull() ?: 1000000L // 10,000 in minor units
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1000000L)

    val themePreference: StateFlow<String> = settingsMap.map { map ->
        map["theme_preference"] ?: "SYSTEM"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val startDayOfMonth: StateFlow<Int> = settingsMap.map { map ->
        map["start_day_of_month"]?.toIntOrNull() ?: 1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val isHideBalance: StateFlow<Boolean> = settingsMap.map { map ->
        map["hide_balance"]?.toBoolean() ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isFirstLaunchCompleted = MutableStateFlow(securityStorage.isFirstLaunchCompleted())
    val isFirstLaunchCompleted: StateFlow<Boolean> = _isFirstLaunchCompleted.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(securityStorage.isAppLockEnabled())
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(securityStorage.isBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    val autoLockTimeout: StateFlow<String> = settingsMap.map { map ->
        map["auto_lock_timeout"] ?: "1" // "IMMEDIATE", "1", "5", "15"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "1")

    // In-memory unlock state for the current session.
    // If App Lock is not enabled, the session is unlocked immediately.
    private val _isUnlocked = MutableStateFlow(!securityStorage.isAppLockEnabled())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    init {
        // Synchronize and migrate any existing credentials from Room database into SecurityStorage
        viewModelScope.launch {
            settingsMap.collect { map ->
                map["first_launch_completed"]?.toBoolean()?.let {
                    _isFirstLaunchCompleted.value = it
                }
                map["app_lock_enabled"]?.toBoolean()?.let {
                    _isAppLockEnabled.value = it
                }
                map["biometric_enabled"]?.toBoolean()?.let {
                    _isBiometricEnabled.value = it
                }
                val salt = map["app_lock_pin_salt"]
                val hash = map["app_lock_pin_hash"]
                val enabled = map["app_lock_enabled"]?.toBoolean() ?: false
                if (!salt.isNullOrEmpty() && !hash.isNullOrEmpty()) {
                    securityStorage.importLegacyCredentials(salt, hash, enabled)
                }
            }
        }
    }

    private var lastBackgroundTimestamp: Long = 0L

    fun unlockApp() {
        _isUnlocked.value = true
    }

    fun lockApp() {
        _isUnlocked.value = false
    }

    /**
     * Verifies the user's entered PIN against persistent secure storage.
     * Uses Android Keystore AES-GCM decryption and cryptographic constant-time comparison.
     */
    fun verifyEnteredPin(enteredPin: String): Boolean {
        val valid = securityStorage.verifyPin(enteredPin)
        if (valid) {
            _isUnlocked.value = true
        }
        return valid
    }

    fun setAppPin(pin: String) {
        val saved = securityStorage.savePin(pin)
        if (saved) {
            securityStorage.setAppLockEnabled(true)
            _isAppLockEnabled.value = true
        }
        viewModelScope.launch(Dispatchers.IO) {
            val salt = securityStorage.getPinSalt() ?: ""
            val hash = securityStorage.getPinHash() ?: ""
            repository.setSetting("app_lock_pin_salt", salt)
            repository.setSetting("app_lock_pin_hash", hash)
            repository.setSetting("app_lock_enabled", "true")
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityStorage.setBiometricEnabled(enabled)
        _isBiometricEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("biometric_enabled", enabled.toString())
        }
    }

    fun setAutoLockTimeout(timeout: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("auto_lock_timeout", timeout)
        }
    }

    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        if (!isAppLockEnabled.value || !_isUnlocked.value) return
        val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
        val timeout = autoLockTimeout.value
        val shouldLock = when (timeout) {
            "IMMEDIATE" -> true
            "1" -> elapsed >= 1 * 60 * 1000L
            "5" -> elapsed >= 5 * 60 * 1000L
            "15" -> elapsed >= 15 * 60 * 1000L
            else -> elapsed >= 1 * 60 * 1000L
        }
        if (shouldLock && lastBackgroundTimestamp > 0L) {
            _isUnlocked.value = false
        }
    }

    fun toggleHideBalance() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = isHideBalance.value
            repository.setSetting("hide_balance", (!current).toString())
        }
    }

    fun setCurrency(currencyCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("currency_code", currencyCode)
        }
    }

    fun setMonthlyBudget(budgetCents: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("monthly_budget_cents", budgetCents.toString())
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("theme_preference", theme)
        }
    }

    fun setStartDayOfMonth(day: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("start_day_of_month", day.coerceIn(1, 28).toString())
        }
    }

    fun setAppLock(enabled: Boolean, pin: String? = null) {
        if (!enabled) {
            securityStorage.setAppLockEnabled(false)
            _isAppLockEnabled.value = false
            _isUnlocked.value = true
            viewModelScope.launch(Dispatchers.IO) {
                repository.setSetting("app_lock_enabled", "false")
                repository.setSetting("biometric_enabled", "false")
            }
        } else if (pin != null) {
            setAppPin(pin)
        } else {
            securityStorage.setAppLockEnabled(true)
            _isAppLockEnabled.value = true
            viewModelScope.launch(Dispatchers.IO) {
                repository.setSetting("app_lock_enabled", "true")
            }
        }
    }

    fun completeFirstLaunch(
        currencyCode: String,
        monthlyBudgetCents: Long,
        theme: String,
        pin: String?,
        enableBiometric: Boolean
    ) {
        // Synchronously save authentication and first launch status to persistent storage
        if (!pin.isNullOrBlank()) {
            securityStorage.savePin(pin)
            securityStorage.setAppLockEnabled(true)
            securityStorage.setBiometricEnabled(enableBiometric)
            _isAppLockEnabled.value = true
            _isBiometricEnabled.value = enableBiometric
        } else {
            securityStorage.setAppLockEnabled(false)
            _isAppLockEnabled.value = false
        }
        securityStorage.setFirstLaunchCompleted(true)
        _isFirstLaunchCompleted.value = true
        _isUnlocked.value = true

        // Persist to Room SQLite database in background
        viewModelScope.launch(Dispatchers.IO) {
            repository.setSetting("currency_code", currencyCode)
            repository.setSetting("monthly_budget_cents", monthlyBudgetCents.toString())
            repository.setSetting("theme_preference", theme)

            if (!pin.isNullOrBlank()) {
                val salt = securityStorage.getPinSalt() ?: ""
                val hash = securityStorage.getPinHash() ?: ""
                repository.setSetting("app_lock_pin_salt", salt)
                repository.setSetting("app_lock_pin_hash", hash)
                repository.setSetting("app_lock_enabled", "true")
            } else {
                repository.setSetting("app_lock_enabled", "false")
            }

            repository.setSetting("biometric_enabled", enableBiometric.toString())
            repository.setSetting("first_launch_completed", "true")
        }
    }

    /**
     * CRITICAL PERSISTENCE METHOD:
     * Validates input, inserts/updates Room SQLite database on IO dispatcher,
     * awaits completion, checks budget warnings, and returns Result<Long>.
     */
    suspend fun saveTransaction(
        id: Long?,
        type: String,
        amountCents: Long,
        category: String,
        dateMillis: Long,
        paymentMethod: String,
        note: String,
        createdAt: Long? = null,
        context: Context? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (amountCents <= 0L) {
                return@withContext Result.failure(IllegalArgumentException("Amount must be greater than zero."))
            }

            val dateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateMillis))
            val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(dateMillis))
            val now = System.currentTimeMillis()

            val entity = TransactionEntity(
                id = id ?: 0L,
                type = type,
                amountCents = amountCents,
                category = category,
                date = dateFormatted,
                time = timeFormatted,
                timestamp = dateMillis,
                dateMillis = dateMillis,
                paymentMethod = paymentMethod,
                note = note.trim(),
                createdAt = createdAt ?: now,
                updatedAt = now
            )

            val persistedId: Long
            if (id != null && id > 0L) {
                repository.updateTransaction(entity)
                persistedId = id
            } else {
                persistedId = repository.insertTransaction(entity)
            }

            if (type == "EXPENSE" && context != null) {
                checkBudgetAlerts(context)
            }

            Result.success(persistedId)
        } catch (e: Exception) {
            Log.e("ExpenseTracker", "Failed to persist transaction to SQLite database", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            repository.deleteTransactionById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ExpenseTracker", "Failed to delete transaction $id", e)
            Result.failure(e)
        }
    }

    // Category Management
    fun addCustomCategory(name: String, type: String, iconName: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cat = CategoryEntity(
                name = name.trim(),
                type = type,
                iconName = iconName,
                colorHex = colorHex,
                isCustom = true
            )
            repository.insertCategory(cat)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    fun updateCategoryBudget(categoryId: Long, budgetCents: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategoryBudget(categoryId, budgetCents)
        }
    }

    private suspend fun checkBudgetAlerts(context: Context) {
        val budget = monthlyBudgetCents.value
        if (budget <= 0L) return

        val (start, end) = DateTimeUtils.getStartAndEndOfMonth(startDayOfMonth = startDayOfMonth.value)
        val allTx = allTransactions.value
        val spentMonth = allTx.filter { it.type == "EXPENSE" && it.dateMillis in start..end }
            .sumOf { it.amountCents }

        val pct = ((spentMonth.toDouble() / budget.toDouble()) * 100).toInt()
        val remaining = (budget - spentMonth).coerceAtLeast(0L)
        val formattedRemaining = CurrencyFormatter.formatCents(remaining, currency.value)

        val alert50Key = "alert_50_${Calendar.getInstance().get(Calendar.MONTH)}"
        val alert80Key = "alert_80_${Calendar.getInstance().get(Calendar.MONTH)}"
        val alert100Key = "alert_100_${Calendar.getInstance().get(Calendar.MONTH)}"

        if (pct >= 100 && repository.getSettingValue(alert100Key) == null) {
            NotificationHelper.showBudgetAlert(context, 100, formattedRemaining)
            repository.setSetting(alert100Key, "true")
        } else if (pct >= 80 && repository.getSettingValue(alert80Key) == null) {
            NotificationHelper.showBudgetAlert(context, 80, formattedRemaining)
            repository.setSetting(alert80Key, "true")
        } else if (pct >= 50 && repository.getSettingValue(alert50Key) == null) {
            NotificationHelper.showBudgetAlert(context, 50, formattedRemaining)
            repository.setSetting(alert50Key, "true")
        }
    }

    fun exportCsv(context: Context) {
        val txs = allTransactions.value
        CsvExporter.shareCsv(context, txs, currency.value)
    }

    fun createBackupJson(): String {
        val txs = allTransactions.value
        val cats = allCategories.value

        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        val txArray = JSONArray()
        for (t in txs) {
            val obj = JSONObject()
            obj.put("type", t.type)
            obj.put("amountCents", t.amountCents)
            obj.put("category", t.category)
            obj.put("date", t.date)
            obj.put("time", t.time)
            obj.put("timestamp", t.timestamp)
            obj.put("dateMillis", t.dateMillis)
            obj.put("paymentMethod", t.paymentMethod)
            obj.put("note", t.note)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val catArray = JSONArray()
        for (c in cats) {
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("type", c.type)
            obj.put("iconName", c.iconName)
            obj.put("colorHex", c.colorHex)
            obj.put("isCustom", c.isCustom)
            obj.put("monthlyBudgetCents", c.monthlyBudgetCents)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        return root.toString(2)
    }

    fun restoreBackupJson(jsonString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val root = JSONObject(jsonString)
                val txArray = root.optJSONArray("transactions") ?: JSONArray()
                val restoredTxs = mutableListOf<TransactionEntity>()
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val dateMillis = obj.optLong("dateMillis", obj.optLong("timestamp", System.currentTimeMillis()))
                    val dateStr = obj.optString("date", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateMillis)))
                    val timeStr = obj.optString("time", SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(dateMillis)))

                    restoredTxs.add(
                        TransactionEntity(
                            type = obj.getString("type"),
                            amountCents = obj.getLong("amountCents"),
                            category = obj.getString("category"),
                            date = dateStr,
                            time = timeStr,
                            timestamp = dateMillis,
                            dateMillis = dateMillis,
                            paymentMethod = obj.getString("paymentMethod"),
                            note = obj.optString("note", "")
                        )
                    )
                }

                if (restoredTxs.isNotEmpty()) {
                    repository.insertTransactions(restoredTxs)
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                Log.e("ExpenseTracker", "Error restoring backup", e)
                withContext(Dispatchers.Main) { onError(e.message ?: "Failed to parse backup") }
            }
        }
    }

    fun resetAppSecurityAndData() {
        securityStorage.clearSecurityData()
        _isUnlocked.value = false
        _isAppLockEnabled.value = false
        _isBiometricEnabled.value = false
        _isFirstLaunchCompleted.value = false
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData(database)
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData(database)
        }
    }
}
