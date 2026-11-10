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
        val isCompact = format == PrintPageFormat.A4_GRID_42 || format == PrintPageFormat.A4_GRID_24

        val w = rect.width()
        val h = rect.height()

        // 1. Background Fill: Custom Gallery Image (Center-Crop Aspect Ratio) OR Theme Solid/Gradient
        var hasCustomBg = false
        if (preloadedBgBitmap != null && preloadedBgBitmap.width > 0 && preloadedBgBitmap.height > 0) {
            hasCustomBg = true
            // CenterCrop: preserve aspect ratio so circles and graphics are NEVER distorted
            val bmpW = preloadedBgBitmap.width.toFloat()
            val bmpH = preloadedBgBitmap.height.toFloat()
            val bmpRatio = bmpW / bmpH
            val rectRatio = w / h
            val srcRect = android.graphics.Rect()

            if (bmpRatio > rectRatio) {
                // Bitmap is wider than destination rect -> crop equal amounts from left and right
                val visibleW = (bmpH * rectRatio).toInt().coerceIn(1, preloadedBgBitmap.width)
                val left = ((preloadedBgBitmap.width - visibleW) / 2).coerceAtLeast(0)
                srcRect.set(left, 0, left + visibleW, preloadedBgBitmap.height)
            } else {
                // Bitmap is taller than destination rect -> crop equal amounts from top and bottom
                val visibleH = (bmpW / rectRatio).toInt().coerceIn(1, preloadedBgBitmap.height)
                val top = ((preloadedBgBitmap.height - visibleH) / 2).coerceAtLeast(0)
                srcRect.set(0, top, preloadedBgBitmap.width, top + visibleH)
            }
            canvas.drawBitmap(preloadedBgBitmap, srcRect, rect, paint)

            if (template.overlayAlpha > 0f) {
                paint.color = Color.BLACK
                paint.alpha = (template.overlayAlpha * 255).toInt().coerceIn(0, 255)
                canvas.drawRoundRect(rect, 4f, 4f, paint)
                paint.alpha = 255
            }
        } else {
            if (isCleanWhite) {
                paint.color = Color.WHITE
                paint.shader = null
                canvas.drawRoundRect(rect, 4f, 4f, paint)

                paint.color = Color.parseColor("#CBD5E1")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.75f
                canvas.drawRoundRect(rect, 4f, 4f, paint)
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
                canvas.drawRoundRect(rect, 4f, 4f, paint)
                paint.shader = null

                // Subtle border
                paint.color = template.primaryColorHex.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.8f
                canvas.drawRoundRect(rect, 4f, 4f, paint)
                paint.style = Paint.Style.FILL
            }
        }

        // 2. Elements Layout: Proportional positioning matching WYSIWYG canvas
        // (A) Network Name
        if (template.showNetworkName && template.networkLayout.isVisible) {
            val netLayout = template.networkLayout
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (hasCustomBg || !isCleanWhite) Color.WHITE else Color.parseColor("#0F172A")
                textSize = (if (isCompact) 7.5f else 9.5f) * netLayout.scale
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            val netX = rect.right - 6f + (netLayout.offsetX / 170f) * (w / 2)
            val netY = rect.top + (if (isCompact) 11f else 15f) + (netLayout.offsetY / 90f) * (h / 2)
            canvas.drawText(card.networkName, netX, netY, titlePaint)
        }

        // (B) Creation Date Badge (Matches screen badge)
        if (template.showCreationDate && template.serialLayout.isVisible) {
            val dateLayout = template.serialLayout
            val formattedDate = try {
                SimpleDateFormat(template.dateFormatPattern, Locale.US).format(Date(template.createdAt))
            } catch (_: Exception) {
                SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(template.createdAt))
            }

            val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (if (isCompact) 6.5f else 7.5f) * dateLayout.scale
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val textWidth = dateTextPaint.measureText(formattedDate)
            val badgeW = (textWidth + 8f) * dateLayout.widthScale.coerceIn(0.6f, 1.8f)
            val badgeH = (if (isCompact) 10f else 13f) * dateLayout.scale

            val dateCenterX = rect.centerX() + (dateLayout.offsetX / 170f) * (w / 2)
            val dateCenterY = rect.top + (if (isCompact) 8f else 12f) + (dateLayout.offsetY / 90f) * (h / 2)
            val dateBadgeRect = RectF(dateCenterX - badgeW / 2, dateCenterY - badgeH / 2, dateCenterX + badgeW / 2, dateCenterY + badgeH / 2)

            paint.color = Color.parseColor("#0F172A")
            paint.alpha = 230
            canvas.drawRoundRect(dateBadgeRect, 3f, 3f, paint)
            paint.color = template.primaryColorHex.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.6f
            canvas.drawRoundRect(dateBadgeRect, 3f, 3f, paint)
            paint.style = Paint.Style.FILL

            canvas.drawText(formattedDate, dateBadgeRect.centerX(), dateBadgeRect.centerY() + (badgeH * 0.28f), dateTextPaint)
        }

        // (C) Price Badge
        if (template.showPrice && template.priceLayout.isVisible) {
            val priceLayout = template.priceLayout
            val baseBadgeWidth = if (isCompact) 34f else 44f
            val badgeWidth = (baseBadgeWidth * priceLayout.scale * priceLayout.widthScale.coerceIn(0.4f, 2.0f))
            val badgeH = (if (isCompact) 11f else 15f) * priceLayout.scale

            val priceCenterX = rect.left + 5f + badgeWidth / 2 + (priceLayout.offsetX / 170f) * (w / 2)
            val priceCenterY = rect.top + (if (isCompact) 9f else 13f) + (priceLayout.offsetY / 90f) * (h / 2)
            val badgeRect = RectF(priceCenterX - badgeWidth / 2, priceCenterY - badgeH / 2, priceCenterX + badgeWidth / 2, priceCenterY + badgeH / 2)

            paint.color = template.badgeBgColorHex.toInt()
            canvas.drawRoundRect(badgeRect, 3f, 3f, paint)

            val pricePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = template.badgeTextColorHex.toInt()
                textSize = (if (isCompact) 7f else 8.5f) * priceLayout.scale
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(card.priceText, badgeRect.centerX(), badgeRect.centerY() + (badgeH * 0.28f), pricePaint)
        }

        // (D) QR Code
        if (template.showQrCode && template.qrCodeLayout.isVisible) {
            val qrLayout = template.qrCodeLayout
            val qrSize = ((if (isCompact) (h * 0.55f) else (h * 0.45f)) * qrLayout.scale).toInt().coerceIn(16, 50)
            val qrLeft = rect.left + 6f + (qrLayout.offsetX / 170f) * (w / 2)
            val qrTop = rect.centerY() - (qrSize / 2) + (qrLayout.offsetY / 90f) * (h / 2)

            try {
                paint.color = Color.WHITE
                canvas.drawRoundRect(RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), 3f, 3f, paint)
                val qrBitmap = QrCodeGenerator.generateQrBitmap(card.loginUrlWithCredentials, sizePx = qrSize)
                canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
            } catch (_: Exception) {}
        }

        // (E) Quota & Validity
        if (template.showValidity && template.metaLayout.isVisible) {
            val metaLayout = template.metaLayout
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (hasCustomBg || !isCleanWhite) template.primaryColorHex.toInt() else Color.parseColor("#0284C7")
                textSize = (if (isCompact) 6f else 7.5f) * metaLayout.scale
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            val metaStr = listOf(card.quotaText, card.validityText).filter { it.isNotBlank() }.joinToString(" • ")
            if (metaStr.isNotBlank()) {
                val metaX = rect.right - 6f + (metaLayout.offsetX / 170f) * (w / 2)
                val metaY = rect.centerY() - (if (isCompact) 11f else 15f) + (metaLayout.offsetY / 90f) * (h / 2)
                canvas.drawText(metaStr, metaX, metaY, metaPaint)
            }
        }

        // (F) Code Box (Centered by default or offset by touch)
        if (template.codeBoxLayout.isVisible) {
            val codeLayout = template.codeBoxLayout
            // Normalized relative position matching WYSIWYG Reference Canvas (340dp x 185dp)
            val relNormX = codeLayout.offsetX / 340f
            val relNormY = codeLayout.offsetY / 185f

            val centerX = rect.centerX() + (relNormX * w)
            val centerY = rect.centerY() + (relNormY * h)

            val maxAvailableWidth = if (template.showQrCode && template.qrCodeLayout.isVisible) (w * 0.70f) else w
            val boxWidth = (maxAvailableWidth * codeLayout.widthScale.coerceIn(0.20f, 1.0f) * (if (hasCustomBg) 0.92f else 0.85f)).coerceIn(35f, w * 0.96f)
            val boxHeight = (if (isCompact) (h * 0.32f) else (h * 0.25f)) * codeLayout.scale.coerceIn(0.6f, 1.4f)
            val boxRect = RectF(centerX - boxWidth / 2, centerY - boxHeight / 2, centerX + boxWidth / 2, centerY + boxHeight / 2)

            when (template.codeBoxStyle) {
                CodeBoxStyle.PILL_BADGE -> {
                    paint.color = if (isCleanWhite) Color.parseColor("#F1F5F9") else Color.WHITE
                    canvas.drawRoundRect(boxRect, boxHeight / 2, boxHeight / 2, paint)
                    paint.color = template.primaryColorHex.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRoundRect(boxRect, boxHeight / 2, boxHeight / 2, paint)
                    paint.style = Paint.Style.FILL
                }
                CodeBoxStyle.SCRATCH_FOIL -> {
                    paint.color = Color.parseColor("#D97706")
                    canvas.drawRoundRect(boxRect, 3f, 3f, paint)
                    paint.color = Color.parseColor("#FDE68A")
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.8f
                    canvas.drawRoundRect(boxRect, 3f, 3f, paint)
                    paint.style = Paint.Style.FILL
                }
                CodeBoxStyle.SPLIT_DIGITS -> {
                    val digits = card.username
                    val count = digits.length.coerceAtLeast(1)
                    val spacing = 2f
                    val digitBoxW = ((boxWidth - (spacing * (count - 1))) / count).coerceAtLeast(6f)
                    for (dIdx in 0 until count) {
                        val dLeft = boxRect.left + (dIdx * (digitBoxW + spacing))
                        val dRect = RectF(dLeft, boxRect.top, dLeft + digitBoxW, boxRect.bottom)
                        paint.color = Color.WHITE
                        canvas.drawRoundRect(dRect, 2.5f, 2.5f, paint)
                        paint.color = template.primaryColorHex.toInt()
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 0.8f
                        canvas.drawRoundRect(dRect, 2.5f, 2.5f, paint)
                        paint.style = Paint.Style.FILL

                        val digitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#0F172A")
                            textSize = (boxHeight * 0.60f).coerceAtLeast(8f)
                            typeface = Typeface.MONOSPACE
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.drawText(digits[dIdx].toString(), dRect.centerX(), dRect.centerY() + (boxHeight * 0.22f), digitPaint)
                    }
                }
                else -> {
                    // MODERN_BOX
                    paint.color = Color.WHITE
                    canvas.drawRoundRect(boxRect, 4f, 4f, paint)
                    paint.color = template.primaryColorHex.toInt()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRoundRect(boxRect, 4f, 4f, paint)
                    paint.style = Paint.Style.FILL
                }
            }

            // Draw text inside if not SPLIT_DIGITS (split digits handles its own text above)
            if (template.codeBoxStyle != CodeBoxStyle.SPLIT_DIGITS) {
                val codeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (template.codeBoxStyle == CodeBoxStyle.SCRATCH_FOIL) Color.WHITE else Color.parseColor("#0F172A")
                    textSize = (boxHeight * 0.60f * codeLayout.scale.coerceIn(0.8f, 1.3f)).coerceAtLeast(8f)
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                }
                val displayCode = if (card.password != card.username) "${card.username} : ${card.password}" else card.username
                canvas.drawText(displayCode, boxRect.centerX(), boxRect.centerY() + (boxHeight * 0.22f), codeTextPaint)
            }
        }

        // (G) Support Phone & Footer
        if (template.showSupportPhone && template.supportPhoneLayout.isVisible && card.supportPhone.isNotBlank()) {
            val phoneLayout = template.supportPhoneLayout
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (hasCustomBg || !isCleanWhite) Color.parseColor("#E2E8F0") else Color.parseColor("#64748B")
                textSize = (if (isCompact) 5f else 6.5f) * phoneLayout.scale
                textAlign = Paint.Align.RIGHT
            }
            val phoneX = rect.right - 6f + (phoneLayout.offsetX / 170f) * (w / 2)
            val phoneY = rect.bottom - 3f + (phoneLayout.offsetY / 90f) * (h / 2)
            canvas.drawText("الدعم: ${card.supportPhone}", phoneX, phoneY, footerPaint)
        }

        // (H) Serial Number
        if (template.showSerialNumber && template.serialLayout.isVisible) {
            val snLayout = template.serialLayout
            val snPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (hasCustomBg || !isCleanWhite) Color.parseColor("#CBD5E1") else Color.parseColor("#94A3B8")
                textSize = (if (isCompact) 5f else 6.5f) * snLayout.scale
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.LEFT
            }
            val snX = rect.left + 6f + (snLayout.offsetX / 170f) * (w / 2)
            val snY = rect.bottom - 3f + (snLayout.offsetY / 90f) * (h / 2)
            canvas.drawText("#${card.serialNumber}", snX, snY, snPaint)
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
