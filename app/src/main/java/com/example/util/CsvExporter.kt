package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.TransactionEntity
import com.example.data.model.AppCurrency
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun generateCsvString(
        transactions: List<TransactionEntity>,
        currency: AppCurrency
    ): String {
        val sb = StringBuilder()
        sb.append("Date,Time,Type,Category,Amount (${currency.code}),Payment Method,Note\n")

        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        for (tx in transactions) {
            val dateStr = dateSdf.format(Date(tx.dateMillis))
            val timeStr = timeSdf.format(Date(tx.dateMillis))
            val amountFormatted = CurrencyFormatter.formatNumberOnly(tx.amountCents)
            val noteEscaped = "\"${tx.note.replace("\"", "\"\"")}\""
            val categoryEscaped = "\"${tx.category.replace("\"", "\"\"")}\""

            sb.append("$dateStr,$timeStr,${tx.type},$categoryEscaped,$amountFormatted,${tx.paymentMethod},$noteEscaped\n")
        }
        return sb.toString()
    }

    fun shareCsv(context: Context, transactions: List<TransactionEntity>, currency: AppCurrency) {
        val csvContent = generateCsvString(transactions, currency)
        val fileName = "expense_tracker_export_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { out ->
            out.write(csvContent.toByteArray(Charsets.UTF_8))
        }

        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Expense Tracker Export")
            putExtra(Intent.EXTRA_TEXT, "Here is your exported transaction history from Expense Tracker.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, "Export CSV")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
