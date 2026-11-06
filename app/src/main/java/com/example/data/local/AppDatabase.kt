package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AppSettingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun settingDao(): SettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance

                // Ensure initial default categories exist
                scope.launch(Dispatchers.IO) {
                    populateInitialData(instance)
                }

                instance
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            if (categoryDao.countCategories() == 0) {
                val defaultCategories = listOf(
                    // Expense categories
                    CategoryEntity(name = "Food", type = "EXPENSE", iconName = "restaurant", colorHex = "#F59E0B"),
                    CategoryEntity(name = "Shopping", type = "EXPENSE", iconName = "shopping_bag", colorHex = "#EC4899"),
                    CategoryEntity(name = "Travel", type = "EXPENSE", iconName = "directions_bus", colorHex = "#3B82F6"),
                    CategoryEntity(name = "Bills", type = "EXPENSE", iconName = "receipt_long", colorHex = "#EF4444"),
                    CategoryEntity(name = "Education", type = "EXPENSE", iconName = "school", colorHex = "#8B5CF6"),
                    CategoryEntity(name = "Health", type = "EXPENSE", iconName = "medical_services", colorHex = "#10B981"),
                    CategoryEntity(name = "Entertainment", type = "EXPENSE", iconName = "movie", colorHex = "#F97316"),
                    CategoryEntity(name = "Groceries", type = "EXPENSE", iconName = "local_grocery_store", colorHex = "#14B8A6"),
                    CategoryEntity(name = "Rent", type = "EXPENSE", iconName = "home", colorHex = "#6366F1"),
                    CategoryEntity(name = "Other", type = "EXPENSE", iconName = "category", colorHex = "#64748B"),

                    // Income categories
                    CategoryEntity(name = "Salary", type = "INCOME", iconName = "payments", colorHex = "#10B981"),
                    CategoryEntity(name = "Business", type = "INCOME", iconName = "storefront", colorHex = "#0EA5E9"),
                    CategoryEntity(name = "Freelance", type = "INCOME", iconName = "laptop", colorHex = "#8B5CF6"),
                    CategoryEntity(name = "Gift", type = "INCOME", iconName = "card_giftcard", colorHex = "#F43F5E"),
                    CategoryEntity(name = "Investment", type = "INCOME", iconName = "trending_up", colorHex = "#059669"),
                    CategoryEntity(name = "Pocket Money", type = "INCOME", iconName = "account_balance_wallet", colorHex = "#D97706"),
                    CategoryEntity(name = "Other", type = "INCOME", iconName = "attach_money", colorHex = "#64748B")
                )
                categoryDao.insertCategories(defaultCategories)
            }
        }
    }
}
