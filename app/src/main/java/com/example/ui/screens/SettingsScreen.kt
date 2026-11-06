package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCurrency
import com.example.data.model.Currencies
import com.example.ui.theme.ExpenseRed
import com.example.util.CurrencyFormatter
import com.example.util.SecurityHelper

@Composable
fun SettingsScreen(
    currency: AppCurrency,
    themePreference: String,
    monthlyBudgetCents: Long,
    startDayOfMonth: Int,
    isAppLockEnabled: Boolean,
    isBiometricEnabled: Boolean = false,
    autoLockTimeout: String = "1",
    onSelectCurrency: (String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onSetMonthlyBudget: (Long) -> Unit,
    onSetStartDayOfMonth: (Int) -> Unit,
    onSetAppLock: (Boolean, String?) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit = {},
    onSetAutoLockTimeout: (String) -> Unit = {},
    onOpenCategoryBudgets: () -> Unit,
    onExportCsv: () -> Unit,
    onCreateBackup: () -> String,
    onRestoreBackup: (String) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showStartDayDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_content"),
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
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Section: General
        item {
            SettingsSectionCard(title = "General") {
                SettingsItem(
                    icon = Icons.Default.Paid,
                    title = "Currency",
                    subtitle = "${currency.symbol} — ${currency.name}",
                    testTag = "setting_currency",
                    onClick = { showCurrencyDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.ColorLens,
                    title = "Theme",
                    subtitle = when (themePreference) {
                        "LIGHT" -> "Light Mode"
                        "DARK" -> "Dark Mode"
                        else -> "System Default"
                    },
                    testTag = "setting_theme",
                    onClick = { showThemeDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.DateRange,
                    title = "Start of Month",
                    subtitle = "Day $startDayOfMonth of each month",
                    testTag = "setting_start_day",
                    onClick = { showStartDayDialog = true }
                )
            }
        }

        // Section: Budget
        item {
            SettingsSectionCard(title = "Budget") {
                SettingsItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Monthly Budget",
                    subtitle = CurrencyFormatter.formatCents(monthlyBudgetCents, currency),
                    testTag = "setting_monthly_budget",
                    onClick = { showBudgetDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.PieChart,
                    title = "Category Budgets",
                    subtitle = "Manage individual limits per category",
                    testTag = "setting_category_budgets",
                    onClick = onOpenCategoryBudgets
                )
            }
        }

        // Section: Security
        item {
            SettingsSectionCard(title = "Security & Privacy") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("App Lock", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                if (isAppLockEnabled) "Protected by 4-digit PIN" else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinDialog = true
                            } else {
                                onSetAppLock(false, null)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White),
                        modifier = Modifier.testTag("app_lock_switch")
                    )
                }

                if (isAppLockEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Change PIN",
                        subtitle = "Update your 4-digit security code",
                        testTag = "setting_change_pin",
                        onClick = { showPinDialog = true }
                    )

                    if (SecurityHelper.isBiometricAvailable(context)) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text("Biometric Unlock", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                    Text(
                                        if (isBiometricEnabled) "Enabled" else "Disabled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { onSetBiometricEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White),
                                modifier = Modifier.testTag("biometric_switch")
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    SettingsItem(
                        icon = Icons.Default.DateRange,
                        title = "Auto-Lock Timeout",
                        subtitle = when (autoLockTimeout) {
                            "IMMEDIATE" -> "Lock Immediately"
                            "1" -> "After 1 Minute"
                            "5" -> "After 5 Minutes"
                            "15" -> "After 15 Minutes"
                            else -> "After 1 Minute"
                        },
                        testTag = "setting_auto_lock_timeout",
                        onClick = { showTimeoutDialog = true }
                    )
                }
            }
        }

        // Section: Data Management
        item {
            SettingsSectionCard(title = "Data Management") {
                SettingsItem(
                    icon = Icons.Default.Share,
                    title = "Export CSV",
                    subtitle = "Export transactions with dates, types & amounts",
                    testTag = "setting_export_csv",
                    onClick = onExportCsv
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.FileUpload,
                    title = "Backup Data",
                    subtitle = "Generate offline JSON backup string",
                    testTag = "setting_backup_data",
                    onClick = { showBackupDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.FileDownload,
                    title = "Restore Data",
                    subtitle = "Import transactions and categories from backup",
                    testTag = "setting_restore_data",
                    onClick = { showRestoreDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Delete All Data",
                    subtitle = "Reset all transactions and custom categories",
                    tint = ExpenseRed,
                    testTag = "setting_delete_all",
                    onClick = { showDeleteAllDialog = true }
                )
            }
        }

        // Section: About
        item {
            SettingsSectionCard(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Privacy Policy",
                    subtitle = "100% Offline. Zero telemetry, zero tracking.",
                    testTag = "setting_privacy_policy",
                    onClick = { showPrivacyPolicyDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text("App Version", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                    Text(
                        text = "v1.0.0 (Offline Edition)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Currency Picker Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(Currencies.list) { curr ->
                        val isSelected = currency.code == curr.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    onSelectCurrency(curr.code)
                                    showCurrencyDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${curr.symbol}  ${curr.name}",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
            }
        )
    }

    // Theme Picker Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SYSTEM" to "System Default", "LIGHT" to "Light Mode", "DARK" to "Dark Mode").forEach { (code, label) ->
                        val isSelected = themePreference == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    onSelectTheme(code)
                                    showThemeDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Monthly Budget Dialog
    if (showBudgetDialog) {
        var input by remember { mutableStateOf(CurrencyFormatter.formatNumberOnly(monthlyBudgetCents)) }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget") },
            text = {
                Column {
                    Text("Enter total monthly spending limit:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it.filter { c -> c.isDigit() } },
                        leadingIcon = { Text(currency.symbol, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cents = CurrencyFormatter.parseToCents(input)
                    if (cents > 0L) {
                        onSetMonthlyBudget(cents)
                        showBudgetDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Start Day Dialog
    if (showStartDayDialog) {
        var dayInput by remember { mutableStateOf(startDayOfMonth.toString()) }
        AlertDialog(
            onDismissRequest = { showStartDayDialog = false },
            title = { Text("Start Day of Month") },
            text = {
                Column {
                    Text("Enter which day of the month your budget/month cycle begins (1 to 28):", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = dayInput,
                        onValueChange = { dayInput = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val d = dayInput.toIntOrNull() ?: 1
                    onSetStartDayOfMonth(d.coerceIn(1, 28))
                    showStartDayDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDayDialog = false }) { Text("Cancel") }
            }
        )
    }

    // PIN Setup / Change Dialog
    if (showPinDialog) {
        var pin1 by remember { mutableStateOf("") }
        var pin2 by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter a 4-digit PIN to lock your expenses:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { if (it.length <= 4) pin1 = it.filter { c -> c.isDigit() } },
                        label = { Text("New PIN (4 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { if (it.length <= 4) pin2 = it.filter { c -> c.isDigit() } },
                        label = { Text("Confirm PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Text(error!!, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (pin1.length != 4) {
                        error = "PIN must be exactly 4 digits."
                    } else if (pin1 != pin2) {
                        error = "PINs do not match."
                    } else {
                        onSetAppLock(true, pin1)
                        showPinDialog = false
                        Toast.makeText(context, "PIN successfully updated", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Set PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Auto-Lock Timeout Dialog
    if (showTimeoutDialog) {
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("Auto-Lock Timeout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val timeouts = listOf(
                        "IMMEDIATE" to "Lock Immediately",
                        "1" to "After 1 Minute",
                        "5" to "After 5 Minutes",
                        "15" to "After 15 Minutes"
                    )
                    timeouts.forEach { (code, label) ->
                        val isSelected = autoLockTimeout == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    onSetAutoLockTimeout(code)
                                    showTimeoutDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTimeoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Backup Data Dialog
    if (showBackupDialog) {
        val backupJson = remember { onCreateBackup() }
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Local Backup Data") },
            text = {
                Column {
                    Text(
                        "Here is your full offline database backup formatted as JSON. You can copy it to your notes or clipboard for safe keeping:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backupJson,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Expense Tracker Backup", backupJson))
                    Toast.makeText(context, "Backup copied to clipboard", Toast.LENGTH_SHORT).show()
                    showBackupDialog = false
                }) {
                    Text("Copy Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Done") }
            }
        )
    }

    // Restore Data Dialog
    if (showRestoreDialog) {
        var restoreInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore From Backup") },
            text = {
                Column {
                    Text(
                        "Paste a previously exported JSON backup to restore your transactions and categories:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restoreInput,
                        onValueChange = { restoreInput = it },
                        placeholder = { Text("Paste JSON backup here...") },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreInput.isNotBlank()) {
                            onRestoreBackup(restoreInput.trim())
                            showRestoreDialog = false
                        }
                    },
                    enabled = restoreInput.isNotBlank()
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete All Data Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Data?") },
            text = {
                Text(
                    "Are you sure you want to permanently delete all transactions and reset data? This action cannot be undone.",
                    color = ExpenseRed
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showDeleteAllDialog = false
                        Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    modifier = Modifier.testTag("confirm_delete_all_button")
                ) {
                    Text("Delete Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Column(modifier = Modifier.height(260.dp).padding(vertical = 4.dp)) {
                    Text(
                        text = "100% Offline & Private\n\n" +
                                "Expense Tracker is built on privacy-by-design principles:\n\n" +
                                "• All your financial records, income, expenses, and budgets are saved exclusively in your device's local Room database.\n" +
                                "• We do not operate any tracking servers, cloud databases, or third-party analytics.\n" +
                                "• We never request access to your banking passwords, SMS messages, contacts, microphone, or camera.\n" +
                                "• When you export data (CSV or Backup), it is created locally and shared only by your explicit action.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) { Text("Got it") }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
