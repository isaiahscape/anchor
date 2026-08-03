package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.ui.ExpenseViewModel
import com.example.ui.ExpenseViewModelFactory
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = AppDatabase.getDatabase(this)
        val repository = ExpenseRepository(database.expenseDao(), this)
        val viewModelFactory = ExpenseViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[ExpenseViewModel::class.java]
        
        setContent {
            val isDarkMode = viewModel.isDarkMode.collectAsStateWithLifecycle(initialValue = false).value
            
            MyApplicationTheme(darkTheme = isDarkMode) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
