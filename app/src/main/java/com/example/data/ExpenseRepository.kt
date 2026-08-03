package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExpenseRepository(private val expenseDao: ExpenseDao, context: Context) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
    
    private val _monthlyBudget = MutableStateFlow(sharedPrefs.getFloat("monthly_budget", 1000f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _profileName = MutableStateFlow(sharedPrefs.getString("profile_name", "") ?: "")
    val profileName: StateFlow<String> = _profileName.asStateFlow()

    private val _profileImageUri = MutableStateFlow(sharedPrefs.getString("profile_image_uri", null))
    val profileImageUri: StateFlow<String?> = _profileImageUri.asStateFlow()

    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(sharedPrefs.getString("selected_currency", "USD") ?: "USD")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    fun updateBudget(newBudget: Double) {
        sharedPrefs.edit().putFloat("monthly_budget", newBudget.toFloat()).apply()
        _monthlyBudget.value = newBudget
    }

    fun updateProfileName(name: String) {
        sharedPrefs.edit().putString("profile_name", name).apply()
        _profileName.value = name
    }

    fun updateProfileImageUri(uri: String?) {
        sharedPrefs.edit().putString("profile_image_uri", uri).apply()
        _profileImageUri.value = uri
    }

    fun updateDarkMode(isDark: Boolean) {
        sharedPrefs.edit().putBoolean("is_dark_mode", isDark).apply()
        _isDarkMode.value = isDark
    }

    fun updateCurrency(code: String) {
        sharedPrefs.edit().putString("selected_currency", code).apply()
        _selectedCurrency.value = code
    }

    fun getExpensesBetweenDates(startDate: Long, endDate: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesBetweenDates(startDate, endDate)
    }

    suspend fun insert(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun update(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteById(id: Int) {
        expenseDao.deleteExpenseById(id)
    }
}
