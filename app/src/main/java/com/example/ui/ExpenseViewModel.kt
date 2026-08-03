package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis)
    val selectedMonth: StateFlow<Long> = _selectedMonth.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<List<Expense>> = _selectedMonth.flatMapLatest { startOfMonth ->
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startOfMonth
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }
        repository.getExpensesBetweenDates(startOfMonth, calendar.timeInMillis)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val budgetState: StateFlow<Double> = repository.monthlyBudget
    val profileName: StateFlow<String> = repository.profileName
    val profileImageUri: StateFlow<String?> = repository.profileImageUri
    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
    val selectedCurrency: StateFlow<String> = repository.selectedCurrency
    val categoryBudgets: StateFlow<Map<String, Double>> = repository.categoryBudgets

    fun updateBudget(newBudget: Double) {
        repository.updateBudget(newBudget)
    }

    fun updateCategoryBudget(category: String, amount: Double) {
        repository.updateCategoryBudget(category, amount)
    }

    fun updateProfileName(name: String) {
        repository.updateProfileName(name)
    }

    fun updateProfileImageUri(uri: String?) {
        repository.updateProfileImageUri(uri)
    }

    fun updateDarkMode(isDark: Boolean) {
        repository.updateDarkMode(isDark)
    }

    fun updateCurrency(code: String) {
        repository.updateCurrency(code)
    }

    fun setMonth(year: Int, month: Int) {
        _selectedMonth.value = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun nextMonth() {
        _selectedMonth.value = Calendar.getInstance().apply {
            timeInMillis = _selectedMonth.value
            add(Calendar.MONTH, 1)
        }.timeInMillis
    }

    fun previousMonth() {
        _selectedMonth.value = Calendar.getInstance().apply {
            timeInMillis = _selectedMonth.value
            add(Calendar.MONTH, -1)
        }.timeInMillis
    }

    fun addExpense(amount: Double, category: String, description: String, timestamp: Long) {
        viewModelScope.launch {
            repository.insert(Expense(amount = amount, category = category, description = description, timestamp = timestamp))
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.update(expense)
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
