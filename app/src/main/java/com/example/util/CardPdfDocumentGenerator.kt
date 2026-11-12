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
import com.example.data.card.CardCutLineStyle
import com.example.data.card.CardTemplate
import com.example.data.card.CardThemePreset
import com.example.data.card.GeneratedCard
import com.example.data.card.PrintPageFormat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-Resolution Multi-Card PDF Generator matching SamMikrotik Web Engine.
 * Supports A4 24-card grid (3x8), 42-card grid (3x14), 18-card grid (3x6), 10-card grid (2x5),
 * and 80mm thermal rolls with rich theme-faithful rendering and cutting marks.
 */
object CardPdfDocumentGenerator {

    private const val A4_WIDTH_POINTS = 595
    private const val A4_HEIGHT_POINTS = 842
    private const val PRINT_DPI = 300f
    private const val POINTS_PER_INCH = 72f
    private const val DPI_SCALE = PRINT_DPI / POINTS_PER_INCH
    private const val PIXELS_PER_MM = PRINT_DPI / 25.4f
    private const val A4_WIDTH_MM = 210f
    private const val A4_HEIGHT_MM = 297f
    private const val A4_WIDTH_PRINT_PX = A4_WIDTH_MM * PIXELS_PER_MM
    private const val A4_HEIGHT_PRINT_PX = A4_HEIGHT_MM * PIXELS_PER_MM
    private const val THERMAL_80MM_WIDTH = 226

    fun generateBatchPdf(
        context: Context,
        batch: CardBatch,
        pageFormat: PrintPageFormat = PrintPageFormat.A4_GRID_24,
        onProgress: ((generatedPage: Int, totalPages: Int) -> Unit)? = null
    ): File {
        val document = PdfDocument()
        val cards = batch.cards
        val template = batch.templateSnapshot

        val textPaint = textPaint().apply {
            color = Color.BLACK
            typeface = Typeface.DEFAULT_BOLD
        }
        val cutLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.75f * DPI_SCALE
            pathEffect = DashPathEffect(floatArrayOf(4f * DPI_SCALE, 3f * DPI_SCALE), 0f)
        }

        val preloadedBgBitmap: Bitmap? = if (!template.customBackgroundImagePath.isNullOrBlank()) {
            val bgFile = File(template.customBackgroundImagePath)
            if (bgFile.exists() && bgFile.length() > 0) {
                try {
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inScaled = false
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
                    onProgress?.invoke(index + 1, cards.size.coerceAtLeast(1))
                }
            } else {
                val savedGrid = template.printGridSettings.normalized()
                val cols = if (savedGrid.isCustom) savedGrid.columns else pageFormat.cols
                val rows = if (savedGrid.isCustom) savedGrid.rows else pageFormat.rows
                val cardsPerPage = (if (savedGrid.isCustom) cols * rows else pageFormat.cardsPerPage)
                    .coerceAtLeast(1)
                val grid = savedGrid.copy(columns = cols, rows = rows).normalized()
                val marginH = grid.horizontalMarginMm * PIXELS_PER_MM
                val marginV = grid.verticalMarginMm * PIXELS_PER_MM
                val gapX = grid.horizontalGapMm * PIXELS_PER_MM
                val gapY = grid.verticalGapMm * PIXELS_PER_MM
                val cellWidth = (A4_WIDTH_PRINT_PX - marginH * 2f - gapX * (cols - 1)) / cols
                val cellHeight = (A4_HEIGHT_PRINT_PX - marginV * 2f - gapY * (rows - 1)) / rows
                val totalPages = kotlin.math.max(1, (cards.size + cardsPerPage - 1) / cardsPerPage)
                val cutLineStyle = if (!template.showCutLines) CardCutLineStyle.NONE else grid.cutLineStyle

                for (pageIndex in 0 until totalPages) {
                    val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_POINTS, A4_HEIGHT_POINTS, pageIndex + 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas
                    val saveCount = canvas.save()
                    canvas.scale(1f / DPI_SCALE, 1f / DPI_SCALE)

                    // Draw in 300-DPI coordinates; the PDF page itself remains physical A4.
                    textPaint.color = Color.parseColor("#0F172A")
                    textPaint.textSize = 8f * DPI_SCALE
                    textPaint.textAlign = Paint.Align.LEFT
                    val headerY = 14f * DPI_SCALE
                    canvas.drawText(
                        "${template.networkName} • هاتف الدعم: ${template.supportPhone} • صفحة ${pageIndex + 1} من $totalPages",
                        marginH,
                        headerY,
                        textPaint
                    )

                    textPaint.textAlign = Paint.Align.RIGHT
                    val formatTitle = "ورقة A4 (210 × 297 مم) • ${cols} أعمدة × ${rows} صفوف (${cardsPerPage} كرت/ورقة)"
                    canvas.drawText(formatTitle, A4_WIDTH_PRINT_PX - marginH, headerY, textPaint)

                    val startIndex = pageIndex * cardsPerPage
                    val endIndex = kotlin.math.min(startIndex + cardsPerPage, cards.size)

                    for (i in startIndex until endIndex) {
                        val localIndex = i - startIndex
                        val col = localIndex % cols
                        val row = localIndex / cols

                        val cellLeft = marginH + col * (cellWidth + gapX)
                        val cellTop = marginV + row * (cellHeight + gapY)
                        val cellRect = RectF(cellLeft, cellTop, cellLeft + cellWidth, cellTop + cellHeight)
                        drawA4Card(canvas, cards[i], template, cellRect, pageFormat, preloadedBgBitmap)
                        drawCutLines(canvas, cellRect, cutLineStyle, cutLinePaint)
                    }

                    canvas.restoreToCount(saveCount)
                    document.finishPage(page)
                    onProgress?.invoke(pageIndex + 1, totalPages)
                }
            }

            val cacheDir = File(context.cacheDir, "cards_pdf").apply { mkdirs() }
            val file = File(cacheDir, "cards_batch_${batch.batchNumber}_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            return file
        } finally {
            preloadedBgBitmap?.recycle()
            document.close()
        }
    }

    private fun textPaint(flags: Int = Paint.ANTI_ALIAS_FLAG): Paint =
        Paint(flags).apply {
            isAntiAlias = true
            isSubpixelText = true
            isLinearText = true
        }

    private fun drawCutLines(
        canvas: Canvas,
        cardRect: RectF,
        style: CardCutLineStyle,
        paint: Paint
    ) {
        when (style) {
            CardCutLineStyle.CONTINUOUS -> canvas.drawRect(cardRect, paint)
            CardCutLineStyle.CORNER_MARKS -> {
                val mark = minOf(4f * DPI_SCALE, cardRect.width() * 0.1f, cardRect.height() * 0.15f)
                canvas.drawLine(cardRect.left, cardRect.top, cardRect.left + mark, cardRect.top, paint)
                canvas.drawLine(cardRect.left, cardRect.top, cardRect.left, cardRect.top + mark, paint)
                canvas.drawLine(cardRect.right - mark, cardRect.top, cardRect.right, cardRect.top, paint)
                canvas.drawLine(cardRect.right, cardRect.top, cardRect.right, cardRect.top + mark, paint)
                canvas.drawLine(cardRect.left, cardRect.bottom - mark, cardRect.left, cardRect.bottom, paint)
                canvas.drawLine(cardRect.left, cardRect.bottom, cardRect.left + mark, cardRect.bottom, paint)
                canvas.drawLine(cardRect.right - mark, cardRect.bottom, cardRect.right, cardRect.bottom, paint)
                canvas.drawLine(cardRect.right, cardRect.bottom - mark, cardRect.right, cardRect.bottom, paint)
            }
            CardCutLineStyle.NONE -> Unit
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
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            isAntiAlias = true
        }
        val isCleanWhite = template.themePreset == CardThemePreset.CLEAN_WHITE
        val isCompact = format == PrintPageFormat.A4_GRID_42 || format == PrintPageFormat.A4_GRID_24
        val w = rect.width()
        val h = rect.height()

        if (preloadedBgBitmap != null && preloadedBgBitmap.width > 0 && preloadedBgBitmap.height > 0) {
            canvas.drawBitmap(preloadedBgBitmap, null, rect, paint)
            if (template.overlayAlpha > 0f) {
                paint.color = Color.BLACK
                paint.alpha = (template.overlayAlpha * 255).toInt().coerceIn(0, 255)
                canvas.drawRoundRect(rect, 4f * DPI_SCALE, 4f * DPI_SCALE, paint)
                paint.alpha = 255
            }
        } else {
            if (isCleanWhite) {
                paint.color = Color.WHITE
                paint.shader = null
                canvas.drawRoundRect(rect, 4f * DPI_SCALE, 4f * DPI_SCALE, paint)

                paint.color = Color.parseColor("#CBD5E1")
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.75f * DPI_SCALE
                canvas.drawRoundRect(rect, 4f * DPI_SCALE, 4f * DPI_SCALE, paint)
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
                canvas.drawRoundRect(rect, 4f * DPI_SCALE, 4f * DPI_SCALE, paint)
                paint.shader = null

                paint.color = template.primaryColorHex.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.8f * DPI_SCALE
                canvas.drawRoundRect(rect, 4f * DPI_SCALE, 4f * DPI_SCALE, paint)
                paint.style = Paint.Style.FILL
            }
        }

        fun centerX(layout: com.example.data.card.ElementLayout) =
            rect.left + layout.xRatio.coerceIn(0f, 1f) * w
        fun centerY(layout: com.example.data.card.ElementLayout) =
            rect.top + layout.yRatio.coerceIn(0f, 1f) * h
        fun textPaint(
            layout: com.example.data.card.ElementLayout,
            baseSizePt: Float,
            typeface: Typeface = Typeface.DEFAULT
        ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isAntiAlias = true
            isSubpixelText = true
            isLinearText = true
            color = layout.textColorHex.toInt()
            textSize = baseSizePt * DPI_SCALE * layout.scale * layout.fontSizeScale.coerceIn(0.5f, 2f)
            this.typeface = if (layout.isBold) {
                Typeface.create(typeface, Typeface.BOLD)
            } else {
                Typeface.create(typeface, Typeface.NORMAL)
            }
            textAlign = Paint.Align.CENTER
        }
        fun drawCenteredText(
            layout: com.example.data.card.ElementLayout,
            text: String,
            baseSizePt: Float,
            typeface: Typeface = Typeface.DEFAULT
        ) {
            val textPaint = textPaint(layout, baseSizePt, typeface)
            fitTextToWidth(textPaint, text, w * layout.widthRatio.coerceIn(0f, 1f))
            val baseline = centerY(layout) - (textPaint.ascent() + textPaint.descent()) / 2f
            canvas.drawText(text, centerX(layout), baseline, textPaint)
        }

        if (template.showNetworkName && template.networkLayout.isVisible) {
            val netLayout = template.networkLayout
            drawCenteredText(netLayout, card.networkName, if (isCompact) 7.5f else 9.5f)
        }

        if (template.showCreationDate && template.creationDateLayout.isVisible) {
            val dateLayout = template.creationDateLayout
            val formattedDate = try {
                SimpleDateFormat(template.dateFormatPattern, Locale.US).format(Date(template.createdAt))
            } catch (_: Exception) {
                SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(template.createdAt))
            }

            val badgeW = w * dateLayout.widthRatio.coerceIn(0f, 1f)
            val badgeH = h * dateLayout.heightRatio.coerceIn(0f, 1f)
            val dateCenterX = centerX(dateLayout)
            val dateCenterY = centerY(dateLayout)
            val dateBadgeRect = RectF(dateCenterX - badgeW / 2, dateCenterY - badgeH / 2, dateCenterX + badgeW / 2, dateCenterY + badgeH / 2)

            paint.color = Color.parseColor("#0F172A")
            paint.alpha = 230
            canvas.drawRoundRect(dateBadgeRect, 3f * DPI_SCALE, 3f * DPI_SCALE, paint)
            paint.color = template.primaryColorHex.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.6f * DPI_SCALE
            canvas.drawRoundRect(dateBadgeRect, 3f * DPI_SCALE, 3f * DPI_SCALE, paint)
            paint.style = Paint.Style.FILL

            val dateTextPaint = textPaint(dateLayout, if (isCompact) 6.5f else 7.5f)
            fitTextToWidth(dateTextPaint, formattedDate, badgeW)
            val dateBaseline = dateCenterY - (dateTextPaint.ascent() + dateTextPaint.descent()) / 2f
            canvas.drawText(formattedDate, dateCenterX, dateBaseline, dateTextPaint)
        }

        if (template.showPrice && template.priceLayout.isVisible) {
            val priceLayout = template.priceLayout
            val badgeWidth = w * priceLayout.widthRatio.coerceIn(0f, 1f)
            val badgeH = h * priceLayout.heightRatio.coerceIn(0f, 1f)

            val priceCenterX = centerX(priceLayout)
            val priceCenterY = centerY(priceLayout)
            val badgeRect = RectF(priceCenterX - badgeWidth / 2, priceCenterY - badgeH / 2, priceCenterX + badgeWidth / 2, priceCenterY + badgeH / 2)

            paint.color = template.badgeBgColorHex.toInt()
            canvas.drawRoundRect(badgeRect, 3f * DPI_SCALE, 3f * DPI_SCALE, paint)

            val pricePaint = textPaint(priceLayout, (badgeH * 0.62f) / DPI_SCALE, Typeface.DEFAULT)
            pricePaint.textSize = (badgeH * 0.62f * priceLayout.fontSizeScale.coerceIn(0.5f, 2f))
                .coerceAtLeast(5f * DPI_SCALE)
            fitTextToWidth(pricePaint, card.priceText, badgeWidth)
            val priceBaseline = priceCenterY - (pricePaint.ascent() + pricePaint.descent()) / 2f
            canvas.drawText(card.priceText, priceCenterX, priceBaseline, pricePaint)
        }

        if (template.showQrCode && template.qrCodeLayout.isVisible) {
            val qrLayout = template.qrCodeLayout
            val qrSize = minOf(
                w * qrLayout.widthRatio.coerceIn(0f, 1f),
                h * qrLayout.heightRatio.coerceIn(0f, 1f)
            ).toInt().coerceAtLeast(1)
            val qrLeft = rect.left + qrLayout.xRatio.coerceIn(0f, 1f) * w - qrSize / 2f
            val qrTop = rect.top + qrLayout.yRatio.coerceIn(0f, 1f) * h - qrSize / 2f

            try {
                paint.color = Color.WHITE
                canvas.drawRoundRect(
                    RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize),
                    3f * DPI_SCALE,
                    3f * DPI_SCALE,
                    paint
                )
                val qrBitmap = QrCodeGenerator.generateQrBitmap(card.loginUrlWithCredentials, sizePx = qrSize)
                canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
                qrBitmap.recycle()
            } catch (_: Exception) {}
        }

        if (template.showValidity && template.metaLayout.isVisible) {
            val metaLayout = template.metaLayout
            val metaStr = listOf(card.quotaText, card.validityText).filter { it.isNotBlank() }.joinToString(" • ")
            if (metaStr.isNotBlank()) {
                val metaPaint = textPaint(
                    metaLayout,
                    (h * metaLayout.heightRatio.coerceIn(0.06f, 0.45f) * 0.65f) / DPI_SCALE
                )
                metaPaint.textSize = (h * metaLayout.heightRatio.coerceIn(0.06f, 0.45f) *
                    0.65f * metaLayout.fontSizeScale.coerceIn(0.5f, 2f)).coerceAtLeast(5f * DPI_SCALE)
                fitTextToWidth(metaPaint, metaStr, w * metaLayout.widthRatio.coerceIn(0f, 1f))
                val metaY = centerY(metaLayout) - (metaPaint.ascent() + metaPaint.descent()) / 2f
                canvas.drawText(metaStr, centerX(metaLayout), metaY, metaPaint)
            }
        }

        if (template.codeBoxLayout.isVisible) {
            val element = template.codeBoxLayout
            val elementWidth = element.widthRatio * rect.width()
            val elementHeight = element.heightRatio * rect.height()
            val codeTextPaint = textPaint(template.codeBoxLayout, 1f, Typeface.MONOSPACE).apply {
                color = template.codeBoxLayout.textColorHex.toInt()
                textSize = (elementHeight * 0.60f * template.codeBoxLayout.fontSizeScale.coerceIn(0.5f, 2f))
                    .coerceAtLeast(DPI_SCALE)
            }
            fitTextToWidth(codeTextPaint, card.username, elementWidth)
            val baseline = centerY(element) - (codeTextPaint.ascent() + codeTextPaint.descent()) / 2f
            canvas.drawText(card.username, centerX(element), baseline, codeTextPaint)
        }

        if (template.showSupportPhone && template.supportPhoneLayout.isVisible && card.supportPhone.isNotBlank()) {
            val phoneLayout = template.supportPhoneLayout
            val phoneText = "الدعم: ${card.supportPhone}"
            drawCenteredText(phoneLayout, phoneText, if (isCompact) 5f else 6.5f)
        }

        if (template.showSerialNumber && template.serialLayout.isVisible) {
            val snLayout = template.serialLayout
            val serialText = "#${card.serialNumber}"
            drawCenteredText(snLayout, serialText, if (isCompact) 5f else 6.5f, Typeface.MONOSPACE)
        }
    }

    private fun fitTextToWidth(paint: Paint, text: String, maxWidth: Float) {
        val measuredWidth = paint.measureText(text)
        if (maxWidth > 0f && measuredWidth > maxWidth) {
            paint.textSize *= maxWidth / measuredWidth
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
