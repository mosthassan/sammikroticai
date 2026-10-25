package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Unified dispatch and sharing manager for financial reports (PDF & CSV).
 * Wraps Android native Intent.ACTION_SEND and Intent.ACTION_VIEW with FileProvider.
 */
object ReportExportManager {

    /**
     * Dispatches the generated PDF file via Android Share Chooser.
     */
    fun sharePdf(context: Context, file: File, title: String = "مشاركة تقرير PDF") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة ملف PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Dispatches the generated CSV file via Android Share Chooser.
     */
    fun shareCsv(context: Context, file: File, title: String = "مشاركة ملف CSV") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر مشاركة ملف CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the generated PDF file in an external PDF viewer.
     */
    fun openPdf(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "عرض مستند PDF عبر"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح ملف PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
