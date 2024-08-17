package recognition

import permute
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val IMAGE_WIDTH = 300
private const val KERNEL_SIZE = 9

fun main() {
    // Load image
    println("Load")
    val inImage = ImageIO.read(File("recognition/in.jpg"))
    // Scale
    println("Scale")
    val sf = IMAGE_WIDTH / inImage.getWidth(null).toDouble()
    val height = (inImage.getHeight(null) * sf).roundToInt()
    val scaledImage = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_BYTE_GRAY)
    val graphics = scaledImage.graphics as Graphics2D
    val transform = AffineTransform.getScaleInstance(sf, sf)
    graphics.drawImage(inImage, transform, null)
    // Grayscale and invert
    println("Grayscale")
    val grayImage = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB)
    permute(0..<IMAGE_WIDTH, 0..<height)
        .forEach { (x, y) ->
            val rgb = scaledImage.getRGB(x, y)
            val r = (rgb shr 16) and 0xff
            val g = (rgb shr 8) and 0xff
            val b = rgb and 0xff
            val gray = 255 - (r + g + b) / 3
            val newRgb = gray or (gray shl 8) or (gray shl 16)
            grayImage.setRGB(x, y, newRgb)
        }
    ImageIO.write(grayImage, "jpg", File("recognition/gray.jpg"))
    // Function to get gray value from image
    val getGray: (Int, Int) -> Float? = { x, y ->
        if (x in 0..<IMAGE_WIDTH && y in 0..<height)
            (grayImage.getRGB(x, y) and 0xff) / 255f
        else null
    }
    // Get balance
    println("Balance")
//    val kernel = Kernel(KERNEL_SIZE, KERNEL_SIZE, getKernel())
//    val convolution = ConvolveOp(kernel)
//    val convolveImage = convolution.filter(grayImage, null)
//    ImageIO.write(convolveImage, "jpg", File("recognition/convolve.jpg"))
    val balanceImage = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB)
    val halfSize = (KERNEL_SIZE - 1) / 2
    val kernelRange = -halfSize..halfSize
    permute(0..<IMAGE_WIDTH, 0..<height).forEach { (xStart, yStart) ->
        val pairs = permute(kernelRange, kernelRange).map { (xOffset, yOffset) ->
            val x = xStart + xOffset
            val y = yStart + yOffset
            val pixel = getGray(x, y) ?: 0.5f
            (x * pixel) to (y * pixel)
        }.toList()
        val red = pairs.map { it.first }.average() * 255 / 4
        val blue = pairs.map { it.second }.average() * 255 / 4
        val rgb = (red.roundToInt() shl 16) + blue.roundToInt()
        balanceImage.setRGB(xStart, yStart, rgb)
    }
    ImageIO.write(balanceImage, "jpg", File("recognition/balance.jpg"))
}

private fun getKernel(): FloatArray {
    val halfSize = (KERNEL_SIZE - 1) / 2
    val range = -halfSize..halfSize
    val values = permute(range, range).map { (y, x) ->
        sqrt(y.toFloat().pow(2) + x.toFloat().pow(2))
    }.toList()
    val total = values.sum()
    return values.map { it / total }.toFloatArray()
}

private operator fun Pair<Float, Float>.plus(other: Pair<Float, Float>) = (first + other.first) to (second + other.second)