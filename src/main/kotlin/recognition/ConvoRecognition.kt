package recognition

import max
import permute
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.floor

fun main() {
    val inImage = ImageIO.read(File("recognition/in.jpg"))
    // Black and white
    val bwImage = BufferedImage(inImage.width, inImage.height, BufferedImage.TYPE_INT_RGB)
    permute(0..<inImage.width, 0..<inImage.height).forEach { (x, y) ->
        val oldColor = Color(inImage.getRGB(x, y))
        val max = max(oldColor.red, oldColor.green, oldColor.blue)
        val newColor = if (max >= WHITE_THRESHOLD) Color.WHITE else Color.BLACK
        bwImage.setRGB(x, y, newColor.rgb)
    }
    ImageIO.write(bwImage, "jpg", File("recognition/bw.jpg"))
    // Invert colours
    val invertedImage = BufferedImage(bwImage.width, bwImage.height, BufferedImage.TYPE_INT_RGB)
    permute(0..<bwImage.width, 0..<bwImage.height).forEach { (x, y) ->
        invertedImage.setRGB(x, y, 0xffffff - bwImage.getRGB(x, y))
    }
    ImageIO.write(invertedImage, "jpg", File("recognition/invert.jpg"))
    // Convolve
    val kernel = Kernel(5, 5, getKernelArray(10))
    val convolution = ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null)
    val outImage = convolution.filter(invertedImage, null)
    ImageIO.write(outImage, "jpg", File("recognition/all_conv.jpg"))
}

fun getKernelArray(size: Int): FloatArray {
    val mid = floor(size / 2.0).toInt()
    return permute(0..<size, 0..<size).map { (x, y) ->
        when {
            x == mid && y >= mid || y == mid && x >= mid -> 1f
            x > mid && y > mid -> 0f
            else -> -1f
        }
    }.toList().toFloatArray().also { println(it.contentToString()) }
}