package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingEntity
import com.example.data.local.CategoryDao
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryTotal
import com.example.data.local.SettingDao
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val settingDao: SettingDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allSettings: Flow<List<AppSettingEntity>> = settingDao.getAllSettings()

    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> {
        return transactionDao.getRecentTransactions(limit)
    }

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startMillis, endMillis)
    }

    fun getTransactionsByMonth(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByMonth(startMillis, endMillis)
    }

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> {
        return transactionDao.searchTransactions(query)
    }

    fun getTotalIncome(startMillis: Long, endMillis: Long): Flow<Long> {
        return transactionDao.getTotalIncome(startMillis, endMillis)
    }

    fun getTotalExpenses(startMillis: Long, endMillis: Long): Flow<Long> {
        return transactionDao.getTotalExpenses(startMillis, endMillis)
    }

    fun getCategoryTotals(type: String, startMillis: Long, endMillis: Long): Flow<List<CategoryTotal>> {
        return transactionDao.getCategoryTotals(type, startMillis, endMillis)
    }

    fun getTransactionById(id: Long): Flow<TransactionEntity?> {
        return transactionDao.getTransactionById(id)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>) {
        transactionDao.insertTransactions(transactions)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> {
        return categoryDao.getCategoriesByType(type)
    }

    suspend fun insertCategory(category: CategoryEntity): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun updateCategoryBudget(categoryId: Long, budgetCents: Long) {
        categoryDao.updateCategoryBudget(categoryId, budgetCents)
    }

    fun getSetting(key: String): Flow<AppSettingEntity?> {
        return settingDao.getSetting(key)
    }

    suspend fun getSettingValue(key: String): String? {
        return settingDao.getSettingValue(key)
    }

    suspend fun setSetting(key: String, value: String) {
        settingDao.setSetting(AppSettingEntity(key, value))
    }

    suspend fun clearAllData(database: AppDatabase) {
        transactionDao.deleteAllTransactions()
        settingDao.clearSettings()
        // Re-populate default categories
        AppDatabase.populateInitialData(database)
    }
}
