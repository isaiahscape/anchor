package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.Expense
import com.example.util.ExportUtils
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ExpenseViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "main"
    
    val context = LocalContext.current
    val expenses by viewModel.uiState.collectAsStateWithLifecycle()
    val budget by viewModel.budgetState.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.profileImageUri.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var showProfileHub by remember { mutableStateOf(false) }
    var showCurrencySelector by remember { mutableStateOf(false) }

    val tabs = remember { listOf("home", "expenses", "reports", "analytics", "planner") }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val currencyFormatter = remember(selectedCurrency) {
        val format = NumberFormat.getCurrencyInstance()
        try {
            val curr = Currency.getInstance(selectedCurrency)
            format.currency = curr
            // Force using only the symbol by stripping the country/ISO code prefix
            val symbols = (format as java.text.DecimalFormat).decimalFormatSymbols
            val fullSymbol = curr.getSymbol(Locale.US) // Try US locale first for narrow symbols
            symbols.currencySymbol = fullSymbol
                .replace(selectedCurrency, "") // Remove "USD"
                .replace(selectedCurrency.take(2), "") // Remove "US"
                .trim()
                .ifEmpty { "$" }
            format.decimalFormatSymbols = symbols
        } catch (_: Exception) {
            format.currency = Currency.getInstance("USD")
        }
        format
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            if (currentRoute != "settings") {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding().padding(top = 8.dp, bottom = 8.dp),
                    title = { 
                        val title = if (currentRoute == "main") {
                            when (tabs[pagerState.currentPage]) {
                                "home" -> "Dashboard"
                                "expenses" -> "Expenses"
                                "analytics" -> "Analysis"
                                "planner" -> "Budget Planner"
                                else -> "Reports"
                            }
                        } else "Settings"
                        Text(
                            text = title,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.headlineMedium
                        ) 
                    },
                    actions = {
                        val activeTab = if (currentRoute == "main") tabs[pagerState.currentPage] else ""
                        if (activeTab != "home" && activeTab != "analytics" && activeTab != "planner" && currentRoute == "main") {
                            MonthYearSelectorIcon(
                                selectedMonth = selectedMonth,
                                onMonthSelected = { y, m -> viewModel.setMonth(y, m) }
                            )
                        }
                        if (activeTab == "expenses") {
                            IconButton(onClick = { ExportUtils.shareCsvAsText(context, expenses) }) {
                                Icon(Icons.Default.Share, contentDescription = "Export to CSV")
                            }
                        }
                        IconButton(onClick = { showCurrencySelector = true }) {
                            Icon(Icons.Default.Payments, contentDescription = "Select Currency")
                        }
                        IconButton(
                            onClick = { showProfileHub = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(32.dp)
                            ) {
                                if (profileImageUri.isNullOrEmpty()) {
                                    Icon(
                                        Icons.Default.Person, 
                                        contentDescription = "Profile", 
                                        modifier = Modifier.padding(4.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    coil.compose.AsyncImage(
                                        model = profileImageUri,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            val activeTab = if (currentRoute == "main") tabs[pagerState.currentPage] else ""
            if (activeTab == "expenses" || activeTab == "home") {
                LargeFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(bottom = 80.dp) // Lift FAB above floating navbar
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(32.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "main",
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
            ) {
                composable("main") {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (tabs[page]) {
                            "home" -> HomeScreen(
                                expenses = expenses,
                                budget = budget,
                                selectedMonth = selectedMonth,
                                currencyFormatter = currencyFormatter,
                                onUpdateBudget = { viewModel.updateBudget(it) },
                                onMonthSelected = { y, m -> viewModel.setMonth(y, m) },
                                onEditExpense = { expense -> expenseToEdit = expense }
                            )
                            "expenses" -> ExpensesListScreen(
                                expenses = expenses,
                                currencyFormatter = currencyFormatter,
                                onDelete = { viewModel.deleteExpense(it.id) },
                                onEdit = { expense ->
                                    expenseToEdit = expense
                                }
                            )
                            "reports" -> ReportsScreen(
                                expenses = expenses,
                                currencyFormatter = currencyFormatter
                            )
                            "analytics" -> AnalyticsScreen(
                                expenses = expenses,
                                currencyFormatter = currencyFormatter
                            )
                            "planner" -> {
                                val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
                                BudgetPlannerScreen(
                                    expenses = expenses,
                                    categoryBudgets = categoryBudgets,
                                    onUpdateCategoryBudget = { cat, amt -> viewModel.updateCategoryBudget(cat, amt) },
                                    currencyFormatter = currencyFormatter
                                )
                            }
                        }
                    }
                }
                composable("settings") {
                    SettingsScreen(
                        isDarkMode = isDarkMode,
                        onUpdateDarkMode = { viewModel.updateDarkMode(it) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Floating Navigation Bar
            if (currentRoute == "main") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        shadowElevation = 8.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            tabs.forEachIndexed { index, route ->
                                val icon = when (route) {
                                    "home" -> Icons.Default.Home
                                    "expenses" -> Icons.Default.List
                                    "reports" -> Icons.Default.Analytics
                                    "analytics" -> Icons.Default.BarChart
                                    else -> Icons.Default.AccountBalanceWallet
                                }
                                val isSelected = pagerState.currentPage == index
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = route,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showAddDialog || expenseToEdit != null) {
            ExpenseDialog(
                expense = expenseToEdit,
                currencyCode = selectedCurrency,
                onDismiss = { 
                    showAddDialog = false
                    expenseToEdit = null
                },
                onSave = { amount, category, description ->
                    if (expenseToEdit != null) {
                        viewModel.updateExpense(expenseToEdit!!.copy(amount = amount, category = category, description = description))
                    } else {
                        viewModel.addExpense(amount, category, description, System.currentTimeMillis())
                    }
                    showAddDialog = false
                    expenseToEdit = null
                }
            )
        }
        
        if (showProfileHub) {
            ProfileHubBottomSheet(
                profileName = profileName,
                profileImageUri = profileImageUri,
                onUpdateProfileName = { viewModel.updateProfileName(it) },
                onUpdateProfileImageUri = { viewModel.updateProfileImageUri(it) },
                onNavigateToSettings = { navController.navigate("settings") },
                onDismiss = { showProfileHub = false }
            )
        }

        if (showCurrencySelector) {
            CurrencySelectorBottomSheet(
                selectedCurrency = selectedCurrency,
                onCurrencySelected = { 
                    viewModel.updateCurrency(it)
                    showCurrencySelector = false
                },
                onDismiss = { showCurrencySelector = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectorBottomSheet(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    
    val allCurrencies = remember {
        Currency.getAvailableCurrencies()
            .sortedBy { it.currencyCode }
            .map { it to it.getDisplayName(Locale.getDefault()) }
    }
    
    val filteredCurrencies = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allCurrencies
        } else {
            allCurrencies.filter { (curr, name) ->
                curr.currencyCode.contains(searchQuery, ignoreCase = true) ||
                        name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f) // Limit height since it's a long list
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Select Currency",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                placeholder = { Text("Search by code or name") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            
            LazyColumn {
                items(filteredCurrencies) { (curr, name) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        supportingContent = { Text("${curr.currencyCode} (${curr.getSymbol(Locale.getDefault())})") },
                        leadingContent = {
                            RadioButton(
                                selected = selectedCurrency == curr.currencyCode,
                                onClick = null
                            )
                        },
                        modifier = Modifier.clickable { onCurrencySelected(curr.currencyCode) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileHubBottomSheet(
    profileName: String,
    profileImageUri: String?,
    onUpdateProfileName: (String) -> Unit,
    onUpdateProfileImageUri: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onUpdateProfileImageUri(uri.toString())
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image with "EDIT" overlay
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { photoPickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri.isNullOrEmpty()) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    coil.compose.AsyncImage(
                        model = profileImageUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "EDIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = profileName,
                onValueChange = onUpdateProfileName,
                placeholder = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.7f),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold, 
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )
            Spacer(Modifier.height(4.dp))
            Text("Your identity in Anchor", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(32.dp))
            
            // Settings Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                onClick = { 
                    onDismiss()
                    onNavigateToSettings()
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("App Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Theme, Security & More", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun MonthYearSelectorIcon(
    selectedMonth: Long,
    onMonthSelected: (Int, Int) -> Unit
) {
    val context = LocalContext.current

    IconButton(onClick = { 
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedMonth }
        android.app.DatePickerDialog(
            context,
            { _, year, month, _ ->
                onMonthSelected(year, month)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }) {
        Icon(Icons.Default.CalendarToday, contentDescription = "Select Month")
    }
}

@Composable
fun HomeScreen(
    expenses: List<Expense>,
    budget: Double,
    selectedMonth: Long,
    currencyFormatter: NumberFormat,
    onUpdateBudget: (Double) -> Unit,
    onMonthSelected: (Int, Int) -> Unit,
    onEditExpense: (Expense) -> Unit
) {
    val totalExpense = expenses.sumOf { it.amount }
    val remainingBalance = budget - totalExpense
    
    var showBudgetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        // Budget & Balance Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text("Remaining Balance", style = MaterialTheme.typography.titleMedium)
                    IconButton(
                        onClick = { showBudgetDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Budget", modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currencyFormatter.format(remainingBalance),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Budget", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Text(currencyFormatter.format(budget), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Expense", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Text(currencyFormatter.format(totalExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                val progress = if (budget > 0) (totalExpense / budget).toFloat().coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (remainingBalance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            MonthYearSelectorIcon(
                selectedMonth = selectedMonth,
                onMonthSelected = onMonthSelected
            )
        }
        
        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No recent expenses.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                expenses.take(5).forEach { expense ->
                    ExpenseCard(expense = expense, currencyFormatter = currencyFormatter, onDelete = {}, onEdit = { onEditExpense(expense) })
                }
            }
        }
        Spacer(Modifier.height(100.dp)) // Bottom padding for floating navbar
    }
    
    if (showBudgetDialog) {
        var budgetInput by remember { mutableStateOf(budget.toString()) }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Budget") },
                    singleLine = true,
                    prefix = { Text(currencyFormatter.currency?.symbol ?: "$") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newBudget = budgetInput.toDoubleOrNull()
                    if (newBudget != null) {
                        onUpdateBudget(newBudget)
                        showBudgetDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExpensesListScreen(
    expenses: List<Expense>,
    currencyFormatter: NumberFormat,
    onDelete: (Expense) -> Unit,
    onEdit: (Expense) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = CategoryUtils.categories
    
    val filteredExpenses = expenses.filter { expense ->
        val matchesSearch = expense.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || expense.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Column(modifier = Modifier.fillMaxSize()) {
        
        // Search and Filter Section
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search expenses...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All") }
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
        }
        
        if (filteredExpenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (expenses.isEmpty()) "No expenses this month. Add one!" else "No matching expenses found.", 
                    style = MaterialTheme.typography.bodyLarge, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseCard(expense = expense, currencyFormatter = currencyFormatter, onDelete = { onDelete(expense) }, onEdit = { onEdit(expense) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCard(expense: Expense, currencyFormatter: NumberFormat, onDelete: () -> Unit, onEdit: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    true
                } else false
            }
        ),
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 16.dp))
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            onClick = onEdit
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = CategoryUtils.getCategoryColor(expense.category).copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            CategoryUtils.getCategoryIcon(expense.category),
                            contentDescription = null,
                            tint = CategoryUtils.getCategoryColor(expense.category),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = expense.description.ifEmpty { "Expense" }, 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = expense.category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = dateFormat.format(Date(expense.timestamp)), 
                                style = MaterialTheme.typography.bodyMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = currencyFormatter.format(expense.amount),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun ReportsScreen(
    expenses: List<Expense>,
    currencyFormatter: NumberFormat
) {
    val categoryTotals = expenses
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
        
    val totalSpent = expenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        
        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("No data for reports.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val sweepAngles = categoryTotals.map { 
                        if (totalSpent > 0) (it.second / totalSpent).toFloat() * 360f else 0f 
                    }
                    val colors = categoryTotals.map { CategoryUtils.getCategoryColor(it.first) }
                    
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(200.dp)) {
                        var startAngle = -90f
                        for (i in sweepAngles.indices) {
                            val sweep = sweepAngles[i]
                            drawArc(
                                color = colors[i],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 48f)
                            )
                            startAngle += sweep
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Spent", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormatter.format(totalSpent), 
                            style = MaterialTheme.typography.titleLarge, 
                            fontWeight = FontWeight.Black, 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("Spending by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(categoryTotals) { (category, total) ->
                    val percentage = if (totalSpent > 0) (total / totalSpent).toFloat() else 0f
                    CategoryReportItem(category, total, percentage, currencyFormatter)
                }
            }
        }
    }
}

@Composable
fun CategoryReportItem(category: String, total: Double, percentage: Float, currencyFormatter: NumberFormat) {
    val catColor = CategoryUtils.getCategoryColor(category)
    val catIcon = CategoryUtils.getCategoryIcon(category)
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = catColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        catIcon,
                        contentDescription = null,
                        tint = catColor,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(currencyFormatter.format(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = catColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AnalyticsScreen(
    expenses: List<Expense>,
    currencyFormatter: NumberFormat
) {
    val categoryTotals = expenses
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
        
    val totalSpent = expenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Spending Trends",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Simple Bar Chart
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    categoryTotals.forEach { (category, total) ->
                        val barHeight = if (totalSpent > 0) (total / totalSpent).toFloat() else 0f
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight().weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.6f)
                                    .fillMaxHeight(barHeight.coerceAtLeast(0.05f))
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(CategoryUtils.getCategoryColor(category))
                            )
                            Spacer(Modifier.height(8.dp))
                            Icon(
                                CategoryUtils.getCategoryIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Category Distribution",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Pie Chart with Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(150.dp).weight(1f),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    categoryTotals.forEach { (category, total) ->
                        val sweep = if (totalSpent > 0) (total / totalSpent).toFloat() * 360f else 0f
                        drawArc(
                            color = CategoryUtils.getCategoryColor(category),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true
                        )
                        startAngle += sweep
                    }
                }
            }

            Spacer(Modifier.width(24.dp))

            Column(modifier = Modifier.weight(1.2f)) {
                categoryTotals.take(4).forEach { (category, _) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(CategoryUtils.getCategoryColor(category))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun BudgetPlannerScreen(
    expenses: List<Expense>,
    categoryBudgets: Map<String, Double>,
    onUpdateCategoryBudget: (String, Double) -> Unit,
    currencyFormatter: NumberFormat
) {
    val categories = CategoryUtils.categories
    val categoryTotals = expenses
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    var editingCategory by remember { mutableStateOf<String?>(null) }
    var budgetInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Category Budgets",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(categories) { category ->
                val budget = categoryBudgets[category] ?: 0.0
                val spent = categoryTotals[category] ?: 0.0
                val progress = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1.2f) else 0f
                val color = CategoryUtils.getCategoryColor(category)

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = {
                        editingCategory = category
                        budgetInput = budget.toString()
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = color.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        CategoryUtils.getCategoryIcon(category),
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    currencyFormatter.format(spent),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "of ${currencyFormatter.format(budget)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LinearProgressIndicator(
                            progress = { progress.coerceAtMost(1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (progress > 1f) MaterialTheme.colorScheme.error else color,
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                        
                        if (progress > 1f) {
                            Text(
                                "Over budget by ${currencyFormatter.format(spent - budget)}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingCategory != null) {
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Set Budget for $editingCategory") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Budget Amount") },
                    singleLine = true,
                    prefix = { Text(currencyFormatter.currency?.symbol ?: "$") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amount = budgetInput.toDoubleOrNull() ?: 0.0
                    onUpdateCategoryBudget(editingCategory!!, amount)
                    editingCategory = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDialog(
    expense: Expense?,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit
) {
    var amountStr by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(expense?.category ?: "") }
    var description by remember { mutableStateOf(expense?.description ?: "") }
    
    val categories = CategoryUtils.categories
    var expanded by remember { mutableStateOf(false) }
    
    val currencySymbol = remember(currencyCode) {
        try {
            val curr = Currency.getInstance(currencyCode)
            val fullSymbol = curr.getSymbol(Locale.US)
            fullSymbol
                .replace(currencyCode, "") // Remove "USD"
                .replace(currencyCode.take(2), "") // Remove "US"
                .trim()
                .ifEmpty { "$" }
        } catch (_: Exception) {
            "$"
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense != null) "Edit Expense" else "Add Expense", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount") },
                    prefix = { Text(currencySymbol) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    category = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null && category.isNotEmpty()) {
                        onSave(amount, category, description)
                    }
                },
                enabled = amountStr.isNotBlank() && category.isNotBlank(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
