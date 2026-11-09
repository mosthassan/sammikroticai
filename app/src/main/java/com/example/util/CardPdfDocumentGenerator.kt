package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.card.CardBatch
import com.example.data.card.CardTemplate
import com.example.data.card.CardThemePreset
import com.example.data.card.CodeBoxStyle
import com.example.data.card.GeneratedCard
import com.example.data.card.PrintPageFormat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-Resolution Multi-Card PDF Generator matching SamMikrotik Web Engine.
 * Supports A4 24-card grid (3x8), 18-card grid (3x6), 10-card grid (2x5),
 * and 80mm thermal rolls with rich theme-faithful rendering and cutting marks.
 */
object CardPdfDocumentGenerator {

    private const val A4_WIDTH_POINTS = 595   // 72 points/inch * 8.27 inches
    private const val A4_HEIGHT_POINTS = 842  // 72 points/inch * 11.69 inches
    private const val THERMAL_80MM_WIDTH = 226

    fun generateBatchPdf(
        context: Context,
        batch: CardBatch,
        pageFormat: PrintPageFormat = PrintPageFormat.A4_GRID_24
    ): File {
        val document = PdfDocument()
        val cards = batch.cards
        val template = batch.templateSnapshot

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.DEFAULT_BOLD
        }
        val cutLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
            pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
        }

        val preloadedBgBitmap: Bitmap? = if (!template.customBackgroundImagePath.isNullOrBlank()) {
            val bgFile = File(template.customBackgroundImagePath)
            if (bgFile.exists() && bgFile.length() > 0) {
                try {
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    android.graphics.BitmapFactory.decodeFile(bgFile.absolutePath, options)
                } catch (_: Exception) {
                    null
                }
            } else null
        } else null

        try {
            if (pageFormat == PrintPageFormat.THERMAL_ROLL_80MM) {
                val ticketHeight = 220
                cards.forEachIndexed { index, card ->
                    val pageInfo = PdfDocument.PageInfo.Builder(THERMAL_80MM_WIDTH, ticketHeight, index + 1).create()
                    val page = document.startPage(pageInfo)
                    drawThermalCard(page.canvas, card, template, THERMAL_80MM_WIDTH.toFloat(), ticketHeight.toFloat())
                    document.finishPage(page)
                }
            } else {
                val cols = pageFormat.cols
                val rows = pageFormat.rows
                val cardsPerPage = cols * rows

                val marginH = 16f
                val marginV = 22f
                val gridWidth = A4_WIDTH_POINTS - (marginH * 2)
                val gridHeight = A4_HEIGHT_POINTS - (marginV * 2)

                val cardWidth = gridWidth / cols
                val cardHeight = gridHeight / rows

                val totalPages = kotlin.math.max(1, (cards.size + cardsPerPage - 1) / cardsPerPage)

                for (pageIndex in 0 until totalPages) {
                    val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_POINTS, A4_HEIGHT_POINTS, pageIndex + 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas

                    // Header info matching sammikroticnew
                    textPaint.color = Color.parseColor("#0F172A")
                    textPaint.textSize = 8f
                    textPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText("${template.networkName} • هاتف الدعم: ${template.supportPhone} • صفحة ${pageIndex + 1} من $totalPages", marginH, 14f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    val formatTitle = "ورقة A4 (297 × 210 مم) • ${cols} أعمدة × ${rows} صفوف (${cardsPerPage} كرت/ورقة) • خطوط القص ✂️"
                    canvas.drawText(formatTitle, A4_WIDTH_POINTS - marginH, 14f, textPaint)

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

                        val cardRect = RectF(left + 2f, top + 2f, right - 2f, bottom - 2f)
                        drawA4Card(canvas, cards[i], template, cardRect, pageFormat, preloadedBgBitmap)

                        if (template.showCutLines) {
                            canvas.drawRect(left, top, right, bottom, cutLinePaint)
                        }
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
        } finally {
            preloadedBgBitmap?.recycle()
        }
    }

    private fun drawA4Card(
        canvas: Canvas,
        card: GeneratedCard,
        template: CardTemplate,
        rect: RectF,
        format: PrintPageFormat,
        preloadedBgBitmap: Bitmap? = null
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val isCleanWhite = template.themePreset == CardThemePreset.CLEAN_WHITE

        // 1. Background Fill: Custom Gallery Image OR Theme Solid/Gradient
        var hasCustomBg = false
        if (preloadedBgBitmap != null) {
            canvas.drawBitmap(preloadedBgBitmap, null, rect, null)
            hasCustomBg = true
            if (template.overlayAlpha > 0f) {
                paint.color = Color.BLACK
                paint.alpha = (template.overlayAlpha * 255).toInt().coerceIn(0, 255)
                canvas.drawRoundRect(rect, 6f, 6f, paint)
                paint.alpha = 255
            }
        }

        if (!hasCustomBg) {
            if (isCleanWhite) {
                paint.color = Color.WHITE
                paint.shader = null
                canvas.drawRoundRect(rect, 6f, 6f, paint)

                paint.color = Color.parseColor("#CBD5E1")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(rect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL
            } else {
                val bgShader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    template.backgroundColorHex.toInt(),
                    template.gradientEndColorHex.toInt(),
                    Shader.TileMode.CLAMP
                )
                paint.shader = bgShader
                paint.style = Paint.Style.FILL
                canvas.drawRoundRect(rect, 6f, 6f, paint)
                paint.shader = null

                // Subtle border
                paint.color = template.primaryColorHex.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.2f
                canvas.drawRoundRect(rect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL
            }
        }

        val w = rect.width()
        val h = rect.height()
        val isCompact = format == PrintPageFormat.A4_GRID_24 || format == PrintPageFormat.A4_GRID_42

        // 2. Top Header Bar
        val headerHeight = if (isCompact) 18f else 22f
        val headerRect = RectF(rect.left, rect.top, rect.right, rect.top + headerHeight)

        if (isCleanWhite) {
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(headerRect, 6f, 6f, paint)
            canvas.drawRect(RectF(rect.left, rect.top + 6f, rect.right, rect.top + headerHeight), paint)
        } else {
            paint.color = Color.BLACK
            paint.alpha = 80
            canvas.drawRoundRect(headerRect, 6f, 6f, paint)
            canvas.drawRect(RectF(rect.left, rect.top + 6f, rect.right, rect.top + headerHeight), paint)
            paint.alpha = 255
        }

        // Network Name (Right side of header)
        if (template.showNetworkName) {
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isCleanWhite) Color.parseColor("#0F172A") else Color.WHITE
                textSize = if (isCompact) 8f else 10f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(card.networkName, rect.right - 6f, rect.top + headerHeight - 6f, titlePaint)
        }

        // Price Badge (Left side of header)
        if (template.showPrice) {
            val baseBadgeWidth = if (isCompact) 40f else 48f
            val badgeWidth = (baseBadgeWidth * template.priceLayout.widthScale.coerceIn(0.4f, 2.0f))
            val badgeRect = RectF(rect.left + 5f, rect.top + 2f, rect.left + 5f + badgeWidth, rect.top + headerHeight - 2f)
            paint.color = template.badgeBgColorHex.toInt()
            canvas.drawRoundRect(badgeRect, 4f, 4f, paint)

            val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = template.badgeTextColorHex.toInt()
                textSize = if (isCompact) 7.5f else 9f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(card.priceText, badgeRect.centerX(), badgeRect.centerY() + 3f, pricePaint)
        }

        // 3. Middle Body: QR Code on Left, Credentials on Right
        val qrSize = (h - headerHeight - 16f).toInt().coerceIn(24, 60)
        val qrLeft = rect.left + 6f
        val qrTop = rect.top + headerHeight + 4f

        if (template.showQrCode) {
            try {
                // Draw QR container background
                paint.color = Color.WHITE
                canvas.drawRoundRect(RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), 4f, 4f, paint)

                val qrBitmap = QrCodeGenerator.generateQrBitmap(
                    content = card.loginUrlWithCredentials,
                    sizePx = qrSize
                )
                canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
            } catch (_: Exception) {}
        }

        // Info Column on Right
        val infoRight = rect.right - 6f
        var currentY = rect.top + headerHeight + 11f

        // Quota & Validity
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isCleanWhite) Color.parseColor("#0284C7") else template.primaryColorHex.toInt()
            textSize = if (isCompact) 6.5f else 8f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.RIGHT
        }
        val metaStr = listOf(card.quotaText, card.validityText).filter { it.isNotBlank() }.joinToString(" • ")
        if (metaStr.isNotBlank()) {
            canvas.drawText(metaStr, infoRight, currentY, metaPaint)
            currentY += if (isCompact) 9f else 11f
        }

        // Code Box (User & Password) with adjustable widthScale
        val baseBoxWidth = if (template.showQrCode) (w - qrSize - 20f) else (w - 14f)
        val boxWidth = (baseBoxWidth * template.codeBoxLayout.widthScale.coerceIn(0.35f, 1.0f))
        val boxHeight = if (isCompact) 18f else 22f
        val boxRect = RectF(infoRight - boxWidth, currentY, infoRight, currentY + boxHeight)

        // Draw Code Box background according to CodeBoxStyle
        when (template.codeBoxStyle) {
            CodeBoxStyle.PILL_BADGE -> {
                paint.color = if (isCleanWhite) Color.parseColor("#F1F5F9") else 0x3300F0FF.toInt()
                canvas.drawRoundRect(boxRect, boxHeight / 2, boxHeight / 2, paint)
                paint.color = template.primaryColorHex.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(boxRect, boxHeight / 2, boxHeight / 2, paint)
                paint.style = Paint.Style.FILL
            }
            CodeBoxStyle.SCRATCH_FOIL -> {
                paint.color = Color.parseColor("#D97706") // Gold foil look
                canvas.drawRoundRect(boxRect, 4f, 4f, paint)
                paint.color = Color.parseColor("#FDE68A")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.8f
                canvas.drawRoundRect(boxRect, 4f, 4f, paint)
                paint.style = Paint.Style.FILL
            }
            CodeBoxStyle.TICKET_DASHED -> {
                paint.color = if (isCleanWhite) Color.WHITE else 0x22FFFFFF.toInt()
                canvas.drawRoundRect(boxRect, 3f, 3f, paint)
                paint.color = template.primaryColorHex.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                paint.pathEffect = DashPathEffect(floatArrayOf(3f, 2f), 0f)
                canvas.drawRoundRect(boxRect, 3f, 3f, paint)
                paint.pathEffect = null
                paint.style = Paint.Style.FILL
            }
            else -> {
                // MODERN_BOX or SPLIT_DIGITS
                paint.color = if (isCleanWhite) Color.parseColor("#F8FAFC") else 0x330F172A.toInt()
                canvas.drawRoundRect(boxRect, 4f, 4f, paint)
                paint.color = if (isCleanWhite) Color.parseColor("#CBD5E1") else template.primaryColorHex.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRoundRect(boxRect, 4f, 4f, paint)
                paint.style = Paint.Style.FILL
            }
        }

        // Code text inside box
        val codeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (template.codeBoxStyle == CodeBoxStyle.SCRATCH_FOIL) Color.WHITE
                    else if (isCleanWhite) Color.parseColor("#0F172A")
                    else Color.WHITE
            textSize = if (isCompact) 8.5f else 11f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val displayCode = if (card.password != card.username) "${card.username} : ${card.password}" else card.username
        canvas.drawText(displayCode, boxRect.centerX(), boxRect.centerY() + 3.5f, codeTextPaint)

        // 4. Footer Row
        val footerY = rect.bottom - 4f
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isCleanWhite) Color.parseColor("#64748B") else 0xB3FFFFFF.toInt()
            textSize = if (isCompact) 5.5f else 6.5f
            textAlign = Paint.Align.RIGHT
        }
        val phoneStr = if (template.showSupportPhone && card.supportPhone.isNotBlank()) "خدمة العملاء: ${card.supportPhone}" else template.footerNote
        canvas.drawText(phoneStr, infoRight, footerY, footerPaint)

        // Serial Number and Creation Date on left of footer
        val formattedDate = if (template.showCreationDate) {
            try {
                SimpleDateFormat(template.dateFormatPattern, Locale.US).format(Date(template.createdAt))
            } catch (_: Exception) {
                SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(template.createdAt))
            }
        } else null

        val leftFooterText = when {
            template.showSerialNumber && formattedDate != null -> "#${card.serialNumber} • $formattedDate"
            template.showSerialNumber -> "#${card.serialNumber}"
            formattedDate != null -> formattedDate
            else -> ""
        }

        if (leftFooterText.isNotBlank()) {
            val snPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isCleanWhite) Color.parseColor("#94A3B8") else 0x80FFFFFF.toInt()
                textSize = if (isCompact) 5.5f else 6.5f
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText(leftFooterText, rect.left + 6f, footerY, snPaint)
        }
    }

    private fun drawThermalCard(
        canvas: Canvas,
        card: GeneratedCard,
        template: CardTemplate,
        width: Float,
        height: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
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

        canvas.drawText("المستخدم: ${card.username}", width / 2f, y, monoPaint)
        y += 15f
        if (card.password != card.username) {
            canvas.drawText("الرقم السري: ${card.password}", width / 2f, y, monoPaint)
            y += 15f
        }

        if (card.supportPhone.isNotBlank()) {
            canvas.drawText("الدعم الفني: ${card.supportPhone}", width / 2f, y, subPaint)
            y += 14f
        }

        // Cut divider line
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        canvas.drawLine(10f, height - 2f, width - 10f, height - 2f, dashPaint)
    }

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

    fun openOrSharePdfFile(context: Context, file: File, title: String = "معاينة كروت الميكروتك") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pm = context.packageManager
            if (viewIntent.resolveActivity(pm) != null) {
                context.startActivity(viewIntent)
                return
            }
            sharePdfFile(context, file, title)
        } catch (_: Exception) {
            try {
                sharePdfFile(context, file, title)
            } catch (_: Exception) {}
        }
    }
}
