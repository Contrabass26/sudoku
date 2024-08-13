package recognition

import max
import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc.getPerspectiveTransform
import org.bytedeco.opencv.global.opencv_imgproc.warpPerspective
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Size
import org.opencv.core.CvType.CV_32F
import permute
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*

const val WHITE_THRESHOLD = 128

fun main() {
    val inImage = ImageIO.read(File("recognition/in.jpg"))
    // Scale
    val sf = 400.0 / min(inImage.width, inImage.height)
    val scaledImage = BufferedImage(
        (inImage.width * sf).toInt(),
        (inImage.height * sf).toInt(),
        BufferedImage.TYPE_INT_RGB
    )
    val graphics = scaledImage.graphics as Graphics2D
    graphics.drawImage(inImage, AffineTransform.getScaleInstance(sf, sf), null)
    // Black and white
    val bwImage = BufferedImage(scaledImage.width, scaledImage.height, BufferedImage.TYPE_INT_RGB)
    permute(0..<scaledImage.width, 0..<scaledImage.height).forEach { (x, y) ->
        val oldColor = Color(scaledImage.getRGB(x, y))
        val max = max(oldColor.red, oldColor.green, oldColor.blue)
        val newColor = if (max >= WHITE_THRESHOLD) Color.WHITE else Color.BLACK
        bwImage.setRGB(x, y, newColor.rgb)
    }
    // Find corners
    val corners = mutableListOf<Triple<Int, Int, Int>>() // x, y, angle
    permute(10..<(bwImage.width - 10), 10..<(bwImage.height - 10)).forEach { (x, y) ->
        val color = bwImage.getRGB(x, y)
        if (color.and(0xff) == 1) return@forEach // Stop if white
        // For each degree
        val scores = (0..<360 step 5).associateWith { getAngleScore(x, y, it, bwImage) }
        // If we stand out from a lot of angles, probably not good
        val average = scores.values.average()
        if (average > 4) {
            return@forEach
        }
        // Look for corners
        (0..<360)
            .filter { scores[it] == 10 }
            .filter { scores[it rot 90] == 10 }
            .filter { scores[it rot 180] in 1..5 }
            .filter { scores[it rot 270] in 1..5 }
            .forEach { corners.add(Triple(x, y, it)) }
    }
    // Sort corners into types
    val topLeft = corners.filter { (_, _, a) -> a >= 315 || a < 45 }
    val topRight = corners.filter { (_, _, a) -> a in 45..<135 }
    val bottomRight = corners.filter { (_, _, a) -> a in 135..<225 }
    val bottomLeft = corners.filter { (_, _, a) -> a in 225..<315 }
    println(topLeft)
    println(topRight)
    println(bottomRight)
    println(bottomLeft)
    // Get probable coordinates
    val bestCorners = permute(topLeft, topRight, bottomRight, bottomLeft).minBy { cornerSet ->
        // Look for similar rotations
        val differences = cornerSet.map { it.third }.zip(0..<360 step 90).map { (a, b) -> abs(a - b) }
        differences.max() - differences.min()
    }
    println(bestCorners)
    // Remake image as 300x300
    val stream = ByteArrayOutputStream()
    ImageIO.write(bwImage, "jpg", stream)
    stream.flush()
    val srcImage = opencv_imgcodecs.imdecode(Mat(*stream.toByteArray()), 1)
    val srcCorners = FloatPointer(
        bestCorners[0].first.toFloat(), bestCorners[0].second.toFloat(),
        bestCorners[1].first.toFloat(), bestCorners[1].second.toFloat(),
        bestCorners[2].first.toFloat(), bestCorners[2].second.toFloat(),
        bestCorners[3].first.toFloat(), bestCorners[3].second.toFloat()
    )
    val dstCorners = FloatPointer(
        0f, 0f,
        300f, 0f,
        300f, 300f,
        0f, 300f
    )
    // Create matrices with 2 columns to hold the (x, y) values, and 4 rows to hold the 4 corners
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
    opencv_imgcodecs.imwrite("recognition/final_cv.jpg", result)
}

private fun getAngleScore(
    x: Int,
    y: Int,
    angle: Int,
    image: BufferedImage
): Int {
    val color = image.getRGB(x, y)
    val radians = Math.toRadians(angle.toDouble())
    val xOffset = cos(radians)
    val yOffset = sin(radians)
    var xCurrent = x.toDouble()
    var yCurrent = y.toDouble()
    var steps = 0 // The number of black pixels in this direction (including original one)
    var lastColor = color
    while (lastColor and 0xff == 0 && steps < 10) {
        xCurrent += xOffset
        yCurrent += yOffset
        if (xCurrent >= image.width || yCurrent >= image.height || xCurrent < 0 || yCurrent < 0) break
        lastColor = image.getRGB(xCurrent.roundToInt(), yCurrent.roundToInt())
        steps++
    }
    return steps
}

private infix fun Int.rot(degrees: Int) = (this + degrees) % 360