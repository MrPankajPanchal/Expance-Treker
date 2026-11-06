package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val category: String,
    val totalCents: Long
)

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsByMonth(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR paymentMethod LIKE '%' || :query || '%' ORDER BY dateMillis DESC, id DESC")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'INCOME' AND dateMillis >= :startMillis AND dateMillis <= :endMillis")
    fun getTotalIncome(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM transactions WHERE type = 'EXPENSE' AND dateMillis >= :startMillis AND dateMillis <= :endMillis")
    fun getTotalExpenses(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("SELECT category, SUM(amountCents) as totalCents FROM transactions WHERE type = :type AND dateMillis >= :startMillis AND dateMillis <= :endMillis GROUP BY category ORDER BY totalCents DESC")
    fun getCategoryTotals(type: String, startMillis: Long, endMillis: Long): Flow<List<CategoryTotal>>
}
