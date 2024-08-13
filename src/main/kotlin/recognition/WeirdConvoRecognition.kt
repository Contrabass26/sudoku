package recognition

import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc.getPerspectiveTransform
import org.bytedeco.opencv.global.opencv_imgproc.warpPerspective
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Size
import org.opencv.core.CvType.CV_32F
import permute
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private const val IMAGE_WIDTH = 300
private const val KERNEL_SIZE = 7

fun main() {
    val inImage = ImageIO.read(File("recognition/in2.jpg"))
    // Scale
    println("Scaling")
    val sf = IMAGE_WIDTH / inImage.getWidth(null).toDouble()
    val height = (inImage.getHeight(null) * sf).roundToInt()
    val scaledImage = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB)
    val graphics = scaledImage.graphics as Graphics2D
    val transform = AffineTransform.getScaleInstance(sf, sf)
    graphics.drawImage(inImage, transform, null)
    // Map to useful values for convolution
    println("Grayscale map")
    val mapped = permute(0..<IMAGE_WIDTH, 0..<height).map { (x, y) -> x to y }
        .associateWith { (x, y) ->
            val rgb = scaledImage.getRGB(x, y)
            val r = (rgb shr 16) and 0xff
            val g = (rgb shr 8) and 0xff
            val b = rgb and 0xff
            val gray = (r + g + b) / 3
            gray - 128
        } // Between ±128
    println("Grayscale image")
    val grayImage = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB)
    permute(0..<IMAGE_WIDTH, 0..<height).forEach { (x, y) ->
        val gray = mapped[x to y]!! + 128
        val rgb = gray or (gray shl 8) or (gray shl 16)
        grayImage.setRGB(x, y, rgb)
    }
    ImageIO.write(grayImage, "jpg", File("recognition/gray.jpg"))
    // Convolve for each corner
    println("Convolution")
    val halfSize = (KERNEL_SIZE - 1) / 2
    val offsetRange = -halfSize..halfSize
    val transforms = listOf<(Double) -> Double>({ it }, { -it })
    val corners = (0..90 step 5).associateWith { angle ->
        println("Angle $angle")
        val cos = cos(Math.toRadians(angle.toDouble()))
        val sin = sin(Math.toRadians(angle.toDouble()))
        permute(transforms, transforms).map { (yTransform, xTransform) ->
            val kernel = getKernel(xTransform, yTransform, offsetRange)
            val convolved = mapped.keys.associateWith { (xBase, yBase) ->
                permute(offsetRange, offsetRange).map { (xOffset, yOffset) ->
                    val x = (xBase + cos * xOffset - sin * yOffset).roundToInt()
                    val y = (yBase + sin * xOffset + cos * yOffset).roundToInt()
                    val kernelValue = kernel[xOffset to yOffset]!!.toDouble()
                    mapped[x to y]?.times(kernelValue) ?: 0.0
                }.average()
            }
            val candidates = convolved.entries.sortedBy { it.value }.take(20)
            val best = candidates.minBy {
                xTransform(it.key.first.toDouble().pow(2)) + yTransform(
                    it.key.second.toDouble().pow(2)
                )
            }
            best
        }.toList() // TL, TR, BL, BR
    }.minBy { angle -> angle.value.map { it.value }.average() }.value.map { it.key }
    // Perspective transform
    println("Perspective")
    val stream = ByteArrayOutputStream()
    ImageIO.write(grayImage, "jpg", stream)
    stream.flush()
    val srcImage = opencv_imgcodecs.imdecode(Mat(*stream.toByteArray()), 1)
    val srcCorners = FloatPointer(
        corners[0].first.toFloat(), corners[0].second.toFloat(),
        corners[1].first.toFloat(), corners[1].second.toFloat(),
        corners[2].first.toFloat(), corners[2].second.toFloat(),
        corners[3].first.toFloat(), corners[3].second.toFloat()
    )
    val dstCorners = FloatPointer(
        0f, 0f,
        300f, 0f,
        0f, 300f,
        300f, 300f
    )
    val src = Mat(Size(2, 4), CV_32F, srcCorners)
    val dst = Mat(Size(2, 4), CV_32F, dstCorners)
    val perspective: Mat = getPerspectiveTransform(src, dst)
    val result = Mat()
    warpPerspective(
        srcImage,
        result, perspective, Size(300, 300)
    )
    src.release()
    dst.release()
    srcCorners.deallocate()
    dstCorners.deallocate()
    opencv_imgcodecs.imwrite("recognition/out.jpg", result)
}

private fun getKernel(xTransform: (Double) -> Double, yTransform: (Double) -> Double, offsetRange: IntRange) =
    permute(offsetRange, offsetRange).map { (x, y) -> x to y }
        .associateWith { (x, y) ->
            val xd = x.toDouble()
            val yd = y.toDouble()
            when {
                x == 0 && yTransform(yd) >= 0 -> 1.0
                y == 0 && xTransform(xd) >= 0 -> 1.0
                xTransform(xd) > 0 && yTransform(yd) > 0 -> 0.5
                else -> -1.0
            }
        }