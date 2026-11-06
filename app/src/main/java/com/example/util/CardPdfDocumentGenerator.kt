package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.card.CardBatch
import com.example.data.card.GeneratedCard
import com.example.data.card.PrintPageFormat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-Resolution Multi-Card PDF Generator.
 * Produces crisp, print-ready PDF sheets for standard A4 printers and 80mm thermal rolls.
 */
object CardPdfDocumentGenerator {

    private const val A4_WIDTH_POINTS = 595   // 72 points/inch * 8.27 inches
    private const val A4_HEIGHT_POINTS = 842  // 72 points/inch * 11.69 inches
    private const val THERMAL_80MM_WIDTH = 226

    /**
     * Generates a printable PDF file containing all cards in the batch.
     */
    fun generateBatchPdf(
        context: Context,
        batch: CardBatch,
        pageFormat: PrintPageFormat = PrintPageFormat.A4_GRID_10
    ): File {
        val document = PdfDocument()
        val cards = batch.cards
        val template = batch.templateSnapshot

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.DEFAULT_BOLD
        }
        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            typeface = Typeface.DEFAULT
        }
        val cutLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }

        if (pageFormat == PrintPageFormat.THERMAL_ROLL_80MM) {
            // Thermal roll: Each card is one ticket page
            val ticketHeight = 220
            cards.forEachIndexed { index, card ->
                val pageInfo = PdfDocument.PageInfo.Builder(THERMAL_80MM_WIDTH, ticketHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                drawSingleThermalCard(canvas, card, template, THERMAL_80MM_WIDTH.toFloat(), ticketHeight.toFloat())

                document.finishPage(page)
            }
        } else {
            // Standard A4 Grid mode (10 cards: 2 cols x 5 rows)
            val cols = 2
            val rows = 5
            val cardsPerPage = cols * rows

            val marginH = 20f
            val marginV = 28f
            val gridWidth = A4_WIDTH_POINTS - (marginH * 2)
            val gridHeight = A4_HEIGHT_POINTS - (marginV * 2)

            val cardWidth = gridWidth / cols
            val cardHeight = gridHeight / rows

            val totalPages = kotlin.math.max(1, (cards.size + cardsPerPage - 1) / cardsPerPage)

            for (pageIndex in 0 until totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_POINTS, A4_HEIGHT_POINTS, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Draw Page Header & Batch Metadata
                paint.color = Color.parseColor("#0F172A")
                paint.style = Paint.Style.FILL
                textPaint.color = Color.parseColor("#0F172A")
                textPaint.textSize = 10f
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("SamMikrotik Card Studio - دفعة #${batch.batchNumber} (${batch.batchTitle})", marginH, 18f, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(batch.createdAt))
                canvas.drawText("صفحة ${pageIndex + 1} من $totalPages | $dateStr", A4_WIDTH_POINTS - marginH, 18f, textPaint)

                val startIndex = pageIndex * cardsPerPage
                val endIndex = kotlin.math.min(startIndex + cardsPerPage, cards.size)

                for (i in startIndex until endIndex) {
                    val localIndex = i - startIndex
                    val col = localIndex % cols
                    val row = localIndex / cols

                    val left = marginH + (col * cardWidth)
                    val top = marginV + (row * cardHeight)
                    val right = left + cardWidth
                    val bottom = top + cardHeight

                    val cardRect = RectF(left + 4f, top + 4f, right - 4f, bottom - 4f)
                    drawSingleA4Card(canvas, cards[i], template, cardRect)

                    // Draw cutting guide lines
                    canvas.drawRect(left, top, right, bottom, cutLinePaint)
                }

                document.finishPage(page)
            }
        }

        val cacheDir = File(context.cacheDir, "cards_pdf").apply { mkdirs() }
        val file = File(cacheDir, "cards_batch_${batch.batchNumber}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return file
    }

    private fun drawSingleA4Card(
        canvas: Canvas,
        card: GeneratedCard,
        template: com.example.data.card.CardTemplate,
        rect: RectF
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Card Cardboard Background
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        // Card Border
        paint.color = Color.parseColor("#CBD5E1") // Slate border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        // Top Header Banner
        val headerHeight = 26f
        val headerRect = RectF(rect.left, rect.top, rect.right, rect.top + headerHeight)
        paint.color = Color.parseColor("#0F172A")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(headerRect, 8f, 8f, paint)
        // Square bottom corners for header
        canvas.drawRect(RectF(rect.left, rect.top + 10f, rect.right, rect.top + headerHeight), paint)

        // Header Title (Network Name)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(card.networkName, rect.right - 10f, rect.top + 17f, titlePaint)

        // Price badge in header
        val priceBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(card.priceText, rect.left + 10f, rect.top + 17f, priceBadgePaint)

        // QR Code on Left side
        val qrSize = (rect.height() - headerHeight - 16f).toInt()
        val qrLeft = rect.left + 10f
        val qrTop = rect.top + headerHeight + 8f

        try {
            val qrBitmap = QrCodeGenerator.generateQrBitmap(
                content = card.loginUrlWithCredentials,
                sizePx = qrSize
            )
            canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
        } catch (_: Exception) {}

        // Credentials & Quota details on Right side
        val contentLeft = qrLeft + qrSize + 12f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT_BOLD
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
        }
        val credValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.MONOSPACE
        }

        var currentY = rect.top + headerHeight + 20f

        // Quota & Validity
        if (card.quotaText.isNotBlank() || card.validityText.isNotBlank()) {
            val quotaStr = listOf(card.quotaText, card.validityText).filter { it.isNotBlank() }.joinToString(" • ")
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0284C7")
                textSize = 9.5f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(quotaStr, rect.right - 10f, currentY, badgePaint)
            currentY += 16f
        }

        // Username
        canvas.drawText("اسم المستخدم (Username):", rect.right - 10f, currentY, labelPaint)
        currentY += 13f
        canvas.drawText(card.username, rect.right - 10f, currentY, credValuePaint)
        currentY += 16f

        // Password (if different)
        if (card.password.isNotBlank() && card.password != card.username) {
            canvas.drawText("كلمة المرور (Password):", rect.right - 10f, currentY, labelPaint)
            currentY += 13f
            canvas.drawText(card.password, rect.right - 10f, currentY, credValuePaint)
            currentY += 16f
        }

        // Scan QR instruction
        val instructPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 8f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("امسح الرمز بكاميرا هاتفك للدخول الفوري", rect.right - 10f, rect.bottom - 8f, instructPaint)
    }

    private fun drawSingleThermalCard(
        canvas: Canvas,
        card: GeneratedCard,
        template: com.example.data.card.CardTemplate,
        width: Float,
        height: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width, height, paint)

        val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }

        var y = 20f
        canvas.drawText(card.networkName, width / 2f, y, centerTextPaint)
        y += 14f

        val quotaStr = "${card.priceText} - ${card.quotaText}"
        canvas.drawText(quotaStr, width / 2f, y, subPaint)
        y += 18f

        // QR Code in center
        val qrSize = 90
        try {
            val qrBitmap = QrCodeGenerator.generateQrBitmap(card.loginUrlWithCredentials, sizePx = qrSize)
            canvas.drawBitmap(qrBitmap, (width - qrSize) / 2f, y, null)
        } catch (_: Exception) {}

        y += qrSize + 16f

        // Credentials
        canvas.drawText("المستخدم: ${card.username}", width / 2f, y, monoPaint)
        y += 15f
        if (card.password != card.username) {
            canvas.drawText("الرقم السري: ${card.password}", width / 2f, y, monoPaint)
            y += 15f
        }

        canvas.drawText(card.validityText, width / 2f, y, subPaint)

        // Cut divider line
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        canvas.drawLine(10f, height - 2f, width - 10f, height - 2f, dashPaint)
    }

    /**
     * Opens or shares the generated PDF file using the Android system chooser
     */
    fun sharePdfFile(context: Context, file: File, title: String = "كروت الميكروتك جاهزة للطباعة") {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun openPdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            sharePdfFile(context, file)
        }
    }
}
