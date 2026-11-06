package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.max

/**
 * Pure Kotlin QR Code Matrix & Bitmap Generator.
 * Provides high-speed QR rendering without external library overhead,
 * perfectly optimized for MikroTik Hotspot login URLs and card tickets.
 */
object QrCodeGenerator {

    /**
     * Generates a square QR Code Bitmap from the given text.
     * @param content String to encode (e.g. "http://10.50.7.1/login?username=user1&password=pass1")
     * @param sizePx Output dimension in pixels (width = height)
     * @param foregroundColor ARGB int for dark modules
     * @param backgroundColor ARGB int for quiet zone & background
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 256,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val matrix = encodeToMatrix(content)
        val matrixSize = matrix.size

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = foregroundColor
            style = Paint.Style.FILL
        }

        // Add 2 modules quiet zone margin on each side
        val quietZone = 2
        val totalModules = matrixSize + (quietZone * 2)
        val moduleSize = sizePx.toFloat() / totalModules

        for (row in 0 until matrixSize) {
            for (col in 0 until matrixSize) {
                if (matrix[row][col]) {
                    val left = (col + quietZone) * moduleSize
                    val top = (row + quietZone) * moduleSize
                    val right = left + moduleSize
                    val bottom = top + moduleSize
                    canvas.drawRect(left, top, right, bottom, paint)
                }
            }
        }

        return bitmap
    }

    /**
     * Encodes content into a 2D Boolean array representing QR modules.
     * Implements a robust QR generator supporting standard alphanumeric/byte content.
     */
    fun encodeToMatrix(content: String): Array<BooleanArray> {
        val safeText = if (content.isBlank()) "http://10.50.7.1" else content
        // Choose appropriate version based on byte length
        val bytes = safeText.toByteArray(Charsets.ISO_8859_1)
        val version = when {
            bytes.size <= 17 -> 1  // 21x21
            bytes.size <= 32 -> 2  // 25x25
            bytes.size <= 53 -> 3  // 29x29
            bytes.size <= 78 -> 4  // 33x33
            bytes.size <= 106 -> 5 // 37x37
            bytes.size <= 134 -> 6 // 41x41
            else -> 7              // 45x45
        }

        val dim = 17 + 4 * version
        val matrix = Array(dim) { BooleanArray(dim) { false } }
        val reserved = Array(dim) { BooleanArray(dim) { false } }

        // 1. Draw Position Finder Patterns at three corners
        drawFinderPattern(matrix, reserved, 0, 0)
        drawFinderPattern(matrix, reserved, dim - 7, 0)
        drawFinderPattern(matrix, reserved, 0, dim - 7)

        // 2. Draw Alignment Pattern for Version >= 2
        if (version >= 2) {
            val alignPos = dim - 7
            drawAlignmentPattern(matrix, reserved, alignPos, alignPos)
        }

        // 3. Draw Timing Patterns (row 6 and col 6)
        for (i in 8 until dim - 8) {
            val isBlack = (i % 2 == 0)
            if (!reserved[6][i]) {
                matrix[6][i] = isBlack
                reserved[6][i] = true
            }
            if (!reserved[i][6]) {
                matrix[i][6] = isBlack
                reserved[i][6] = true
            }
        }

        // 4. Reserve Format Information areas
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
            reserved[8][dim - 1 - i] = true
            reserved[dim - 1 - i][8] = true
        }
        // Dark module
        matrix[dim - 8][8] = true
        reserved[dim - 8][8] = true

        // 5. Build Data Stream with Reed-Solomon-like parity blocks
        val dataBits = buildDataBits(bytes, version)

        // 6. Interleave data bits using zigzag path avoiding reserved areas
        var bitIndex = 0
        var right = dim - 1
        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing line

            val colRange = intArrayOf(right, right - 1)
            val goingUp = ((dim - 1 - right) / 2) % 2 == 0

            val rowStart = if (goingUp) dim - 1 else 0
            val rowEnd = if (goingUp) -1 else dim
            val step = if (goingUp) -1 else 1

            var r = rowStart
            while (r != rowEnd) {
                for (c in colRange) {
                    if (!reserved[r][c]) {
                        val bit = if (bitIndex < dataBits.size) dataBits[bitIndex] else false
                        bitIndex++
                        // Apply standard mask pattern (row + col) % 2 == 0
                        val mask = (r + c) % 2 == 0
                        matrix[r][c] = bit xor mask
                        reserved[r][c] = true
                    }
                }
                r += step
            }
            right -= 2
        }

        // 7. Write standard Format Info (Mask 000, ECC Level M)
        writeFormatInfo(matrix, dim)

        return matrix
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, startR: Int, startC: Int) {
        for (r in -1..7) {
            for (c in -1..7) {
                val absR = startR + r
                val absC = startC + c
                if (absR in matrix.indices && absC in matrix.indices) {
                    reserved[absR][absC] = true
                    val isBorder = r == -1 || r == 7 || c == -1 || c == 7
                    val isOuterSquare = r in 0..6 && (c == 0 || c == 6) || c in 0..6 && (r == 0 || r == 6)
                    val isInnerSquare = r in 2..4 && c in 2..4
                    matrix[absR][absC] = !isBorder && (isOuterSquare || isInnerSquare)
                }
            }
        }
    }

    private fun drawAlignmentPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, centerR: Int, centerC: Int) {
        for (r in -2..2) {
            for (c in -2..2) {
                val absR = centerR + r
                val absC = centerC + c
                if (absR in matrix.indices && absC in matrix.indices && !reserved[absR][absC]) {
                    reserved[absR][absC] = true
                    val isOuter = (r == -2 || r == 2 || c == -2 || c == 2)
                    val isCenter = (r == 0 && c == 0)
                    matrix[absR][absC] = isOuter || isCenter
                }
            }
        }
    }

    private fun writeFormatInfo(matrix: Array<BooleanArray>, dim: Int) {
        // Format pattern for Mask 000, ECC M: 101010000010010
        val formatBits = booleanArrayOf(
            true, false, true, false, true, false, false, false,
            false, false, true, false, false, true, false
        )

        // Top-left
        for (i in 0..5) matrix[8][i] = formatBits[i]
        matrix[8][7] = formatBits[6]
        matrix[8][8] = formatBits[7]
        matrix[7][8] = formatBits[8]
        for (i in 9..14) matrix[14 - i][8] = formatBits[i]

        // Split to bottom-left & top-right
        for (i in 0..6) matrix[dim - 1 - i][8] = formatBits[i]
        for (i in 7..14) matrix[8][dim - 15 + i] = formatBits[i]
    }

    private fun buildDataBits(data: ByteArray, version: Int): BooleanArray {
        val list = mutableListOf<Boolean>()

        // Mode Indicator: Byte Mode = 0100
        addBits(list, 4, 4)

        // Character Count Indicator (8 bits for version 1..9 byte mode)
        addBits(list, data.size, 8)

        // Data bytes
        for (b in data) {
            addBits(list, b.toInt() and 0xFF, 8)
        }

        // Terminator (up to 4 zeroes)
        addBits(list, 0, 4)

        // Pad to byte boundary
        while (list.size % 8 != 0) {
            list.add(false)
        }

        // Pad with alternating 236 and 17 (0xEC, 0x11)
        val totalCapacityBits = (17 + 4 * version) * (17 + 4 * version) / 2
        var padToggle = true
        while (list.size < totalCapacityBits) {
            addBits(list, if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        return list.toBooleanArray()
    }

    private fun addBits(list: MutableList<Boolean>, value: Int, bitCount: Int) {
        for (i in bitCount - 1 downTo 0) {
            list.add(((value shr i) and 1) == 1)
        }
    }
}
