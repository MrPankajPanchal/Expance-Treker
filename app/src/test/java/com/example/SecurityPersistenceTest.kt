package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ExpenseRepository
import com.example.ui.viewmodel.ExpenseViewModel
import com.example.util.SecurityStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityPersistenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Clean storage before each test
        val storage = SecurityStorage(context)
        storage.clearSecurityData()
    }

    @Test
    fun testPinSaveAndVerification() {
        val storage = SecurityStorage(context)
        val saved = storage.savePin("4321")
        assertTrue("PIN should be saved successfully", saved)

        // Verify raw plaintext PIN is NOT stored
        val salt = storage.getPinSalt()
        val hash = storage.getPinHash()
        assertNotNull(salt)
        assertNotNull(hash)
        assertNotEquals("4321", salt)
        assertNotEquals("4321", hash)

        // Verification tests
        assertTrue("Correct PIN must verify successfully", storage.verifyPin("4321"))
        assertFalse("Wrong PIN must fail", storage.verifyPin("0000"))
        assertFalse("Partial PIN must fail", storage.verifyPin("432"))
        assertFalse("Longer PIN must fail", storage.verifyPin("43210"))
    }

    @Test
    fun testPinPersistsAcrossSimulatedAppRestart() {
        // Session 1: User sets PIN 1234
        val storageSession1 = SecurityStorage(context)
        storageSession1.savePin("1234")
        storageSession1.setFirstLaunchCompleted(true)
        assertTrue(storageSession1.isAppLockEnabled())
        assertTrue(storageSession1.verifyPin("1234"))

        // Session 2: App process terminates and restarts.
        // A new SecurityStorage instance is instantiated from applicationContext.
        val storageSession2 = SecurityStorage(context)
        assertTrue("App lock should still be enabled after restart", storageSession2.isAppLockEnabled())
        assertTrue("First launch completed should persist after restart", storageSession2.isFirstLaunchCompleted())

        // User enters exact same PIN created earlier
        val isValid = storageSession2.verifyPin("1234")
        assertTrue("PIN 1234 must verify successfully after app restart", isValid)

        // Wrong PIN fails
        assertFalse("Incorrect PIN must fail after restart", storageSession2.verifyPin("9999"))
    }

    @Test
    fun testPinChangePersistsAcrossRestart() {
        val storage1 = SecurityStorage(context)
        storage1.savePin("1111")
        assertTrue(storage1.verifyPin("1111"))

        // Update PIN to 2222
        storage1.savePin("2222")
        assertTrue(storage1.verifyPin("2222"))
        assertFalse(storage1.verifyPin("1111"))

        // Restart app
        val storageRestarted = SecurityStorage(context)
        assertTrue("New PIN must verify after restart", storageRestarted.verifyPin("2222"))
        assertFalse("Old PIN must not verify after restart", storageRestarted.verifyPin("1111"))
    }

    @Test
    fun testViewModelPinUnlockFlow() = runTest {
        val database = AppDatabase.getDatabase(context)
        val repository = ExpenseRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            settingDao = database.settingDao()
        )
        val storage1 = SecurityStorage(context)
        val viewModel1 = ExpenseViewModel(repository, database, storage1)

        // First launch complete with PIN 5555
        viewModel1.completeFirstLaunch(
            currencyCode = "USD",
            monthlyBudgetCents = 200000L,
            theme = "SYSTEM",
            pin = "5555",
            enableBiometric = false
        )
        assertTrue(viewModel1.isUnlocked.value)
        assertTrue(viewModel1.isAppLockEnabled.value)

        // Simulate app restart: create new ViewModel
        val storageRestarted = SecurityStorage(context)
        val viewModelRestarted = ExpenseViewModel(repository, database, storageRestarted)

        // On launch, app must be locked
        assertTrue(viewModelRestarted.isAppLockEnabled.value)
        assertFalse("App must start locked on restart when PIN is configured", viewModelRestarted.isUnlocked.value)

        // Enter wrong PIN
        val wrongResult = viewModelRestarted.verifyEnteredPin("1234")
        assertFalse(wrongResult)
        assertFalse("App must remain locked after wrong PIN", viewModelRestarted.isUnlocked.value)

        // Enter correct PIN
        val correctResult = viewModelRestarted.verifyEnteredPin("5555")
        assertTrue("Correct PIN must return true", correctResult)
        assertTrue("App must be unlocked after correct PIN", viewModelRestarted.isUnlocked.value)
    }
}
