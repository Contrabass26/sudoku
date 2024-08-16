package recognition

import permute
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val IMAGE_WIDTH = 300
private const val STRENGTH_ALLOWANCE = 50
private const val STRENGTH_ADJACENT = 0.5

private var image: BufferedImage? = null

fun main() {
    image = ImageIO.read(File("recognition/in.jpg"))
    // Scale
    println("Scaling")
    val sf = IMAGE_WIDTH / image!!.getWidth(null).toDouble()
    val height = (image!!.getHeight(null) * sf).roundToInt()
    val scaledImage = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB)
    val graphics = scaledImage.graphics as Graphics2D
    val transform = AffineTransform.getScaleInstance(sf, sf)
    graphics.drawImage(image, transform, null)
    image = scaledImage
    // Grayscale
    println("Grayscale")
    val grayImage = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB)
    permute(0..<IMAGE_WIDTH, 0..<height).map { (x, y) -> x to y }
        .forEach { (x, y) ->
            val rgb = image!!.getRGB(x, y)
            val r = (rgb shr 16) and 0xff
            val g = (rgb shr 8) and 0xff
            val b = rgb and 0xff
            val gray = (r + g + b) / 3
            val newRgb = gray or (gray shl 8) or (gray shl 16)
            grayImage.setRGB(x, y, newRgb)
        }
    image = grayImage
    ImageIO.write(image, "jpg", File("recognition/gray.jpg"))
    // Convolve for edge detection
    println("Convolution")
    val kernel = Kernel(3, 3, floatArrayOf(-1f, -0.5f, 0f, -0.5f, 0f, 0.5f, 0f, 0.5f, 1f))
    val convolution = ConvolveOp(kernel)
    image = convolution.filter(image, null)
    ImageIO.write(image, "jpg", File("recognition/convolve.jpg"))
    // Look for lines
    println("Line detection")
    val lines = mutableSetOf<Line>()
    permute(0..<IMAGE_WIDTH, 0..<height, 0..<180 step 3).forEach { (xStart, yStart, angle) ->
        var x = xStart.toDouble()
        var y = yStart.toDouble()
        val strength = getGray(x.roundToInt(), y.roundToInt())!!
        // Validate strength
        if (strength < 0.5) {
            invalidate(xStart, yStart, strength = strength, angle = angle, reason = ValidationReason.TOO_WEAK)
            return@forEach
        }
        val xStep = cos(Math.toRadians(angle.toDouble()))
        val yStep = sin(Math.toRadians(angle.toDouble()))
        // Other directions
        val opposite = getGray((xStart - xStep).roundToInt(), (yStart - yStep).roundToInt())
        if (opposite != null) {
            if (abs(opposite - strength) < STRENGTH_ADJACENT) {
                invalidate(xStart, yStart, strength = strength, angle = angle, reason = ValidationReason.NOT_THE_START, hint = opposite)
                return@forEach
            }
        }
        val perp1Val = getGray((xStart - yStep).roundToInt(), (yStart + xStep).roundToInt())
        val perp1 = perp1Val?.let { abs(it - strength) < STRENGTH_ADJACENT }
        val perp2Val = getGray((xStart + yStep).roundToInt(), (yStart - xStep).roundToInt())
        val perp2 = perp2Val?.let { abs(it - strength) < STRENGTH_ADJACENT }
        if (perp1 == true && perp2 == true) {
            invalidate(xStart, yStart, strength = strength, angle = angle, reason = ValidationReason.NOT_A_LINE, hint = perp1Val to perp2Val)
            return@forEach
        }
        var length = 1
        var quota = 0.0
        while (true) {
            val newStrength = getGray(x.roundToInt(), y.roundToInt()) ?: break
            quota += abs(newStrength - strength)
            if (quota > STRENGTH_ALLOWANCE) break
            x += xStep
            y += yStep
            length++
        }
        lines.add(Line(xStart, yStart, (x - 1).roundToInt(), (y - 1).roundToInt(), length, strength, angle, quota))
    }
    println("Unfiltered: ${lines.size}")
    val filtered = mutableListOf<Line>()
    lines.forEach { line ->
        if (line.length / IMAGE_WIDTH.toDouble() < 0.5) {
            invalidate(line, ValidationReason.TOO_SHORT)
            return@forEach
        }
        filtered.forEachIndexed { i, other ->
            val distance = abs(line.xStart - other.xStart) + abs(line.yStart - other.yStart)
            if (distance < 5 && line.angle == other.angle) {
                // Choose the one with higher strength - bigger edge
                if (line.strength > other.strength) {
                    filtered[i] = line
                    invalidate(other, ValidationReason.CLOSE_TO_ANOTHER, line)
                } else {
                    invalidate(line, ValidationReason.CLOSE_TO_ANOTHER, other)
                    return@forEach
                }
            }
        }
        filtered.add(line)
    }
    println()
    filtered.sortedByDescending { it.quota }.forEach { println(it) }
    println("Filtered: ${filtered.size}")
}

fun getGray(x: Int, y: Int): Double? {
    return try {
        (image!!.getRGB(x, y) and 0xff) / 255.0
    }
    catch (e: ArrayIndexOutOfBoundsException) {
        null
    }
}

data class Line(val xStart: Int, val yStart: Int, val xEnd: Int, val yEnd: Int, val length: Int, val strength: Double, val angle: Int, val quota: Double)

fun invalidate(line: Line, reason: ValidationReason, hint: Any? = null) {
    if (line.xStart == 261 && line.yStart == 106) {
        println("Invalidated $line for $reason: $hint")
    }
}

fun invalidate(
    xStart: Int? = null,
    yStart: Int? = null,
    xEnd: Int? = null,
    yEnd: Int? = null,
    length: Int? = null,
    strength: Double? = null,
    angle: Int? = null,
    quota: Double? = null,
    reason: ValidationReason,
    hint: Any? = null
) {
    val line = mapOf("xStart" to xStart, "yStart" to yStart, "xEnd" to xEnd, "yEnd" to yEnd, "length" to length, "strength" to strength, "angle" to angle, "quota" to quota)
        .filterValues { it != null }
        .toString()
    if (xStart == 261 && yStart == 106) {
        println("Invalidated $line for $reason: $hint")
    }
}

enum class ValidationReason {
    TOO_SHORT, TOO_WEAK, NOT_THE_START, NOT_A_LINE, CLOSE_TO_ANOTHER;
}