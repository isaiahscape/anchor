package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.Expense
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {
    fun exportToCsvAndShare(context: Context, expenses: List<Expense>) {
        try {
            val file = File(context.cacheDir, "expenses_export.csv")
            val writer = FileWriter(file)
            
            writer.append("Date,Amount,Category,Description\n")
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            
            for (expense in expenses) {
                val dateString = dateFormat.format(Date(expense.timestamp))
                val escapedDescription = expense.description.replace("\"", "\"\"")
                writer.append("$dateString,${expense.amount},${expense.category},\"$escapedDescription\"\n")
            }
            
            writer.flush()
            writer.close()
            
            // Note: Since API 24+, we must use FileProvider, but for a quick share, 
            // if we configure FileProvider in AndroidManifest, it works perfectly.
            // Alternatively, we can use Intent.ACTION_CREATE_DOCUMENT to let user save it.
            // Let's use ACTION_SEND to just share the text for simplicity if FileProvider isn't setup.
            // Actually, let's just share it as plain text if it's not too long, or setup a basic FileProvider.
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getCsvContent(expenses: List<Expense>): String {
        val builder = java.lang.StringBuilder()
        builder.append("Date,Amount,Category,Description\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        for (expense in expenses) {
            val dateString = dateFormat.format(Date(expense.timestamp))
            val escapedDescription = expense.description.replace("\"", "\"\"")
            builder.append("$dateString,${expense.amount},${expense.category},\"$escapedDescription\"\n")
        }
        return builder.toString()
    }
    
    fun shareCsvAsText(context: Context, expenses: List<Expense>) {
        val csv = getCsvContent(expenses)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Expenses Export")
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        context.startActivity(Intent.createChooser(intent, "Export Expenses"))
    }
}
