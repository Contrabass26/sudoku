import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc.getPerspectiveTransform
import org.bytedeco.opencv.global.opencv_imgproc.warpPerspective
import org.bytedeco.opencv.opencv_core.IplImage
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Size
import org.opencv.core.CvType.CV_32F
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*

const val WHITE_THRESHOLD = 128
const val SCALE_FACTOR = 0.1

private lateinit var lastImage: BufferedImage

fun main() {
    println("Loading image...")
    lastImage = ImageIO.read(File("recognition/in.jpg"))
    println("Loaded image")
    // Scale
    transformAndSave("scale") {
        val result = BufferedImage((it.width * SCALE_FACTOR).toInt(), (it.height * SCALE_FACTOR).toInt(), BufferedImage.TYPE_INT_RGB)
        val graphics = result.graphics as Graphics2D
        graphics.drawImage(it, AffineTransform.getScaleInstance(SCALE_FACTOR, SCALE_FACTOR), null)
        result
    }
    // Black and white
    transformAndSave("black_and_white") {
        val result = BufferedImage(it.width, it.height, BufferedImage.TYPE_INT_RGB)
        permute(0..<it.width, 0..<it.height).forEach { (x, y) ->
            print("\r($x, $y)")
            val oldColor = Color(it.getRGB(x, y))
            val max = max(oldColor.red, oldColor.green, oldColor.blue)
            val newColor = if (max >= WHITE_THRESHOLD) Color.WHITE else Color.BLACK
            result.setRGB(x, y, newColor.rgb)
        }
        result
    }
    // Find corners
    val corners = mutableListOf<Triple<Int, Int, Int>>() // x, y, angle
    permute(10..<(lastImage.width - 10), 10..<(lastImage.height - 10)).forEach { (x, y) ->
        val color = lastImage.getRGB(x, y)
        if (color.and(0xff) == 1) return@forEach // Stop if white
        // For each degree
        val scores = (0..<360 step 5).associateWith {
            val radians = Math.toRadians(it.toDouble())
            val xOffset = cos(radians)
            val yOffset = sin(radians)
            var xCurrent = x.toDouble()
            var yCurrent = y.toDouble()
            var steps = 0 // The number of black pixels in this direction (including original one)
            var lastColor = color
            while (lastColor and 0xff == 0 && steps < 10) {
                xCurrent += xOffset
                yCurrent += yOffset
                if (xCurrent >= lastImage.width || yCurrent >= lastImage.height || xCurrent < 0 || yCurrent < 0) break
                lastColor = lastImage.getRGB(xCurrent.roundToInt(), yCurrent.roundToInt())
                steps++
            }
            steps
        }
        // If we stand out from a lot of angles, probably not good
        val average = scores.values.average()
        if (average > 3) {
            return@forEach
        }
        // Look for corners
        (0..<360)
            .filter { scores[it] == 10 }
            .filter { scores[it rot 90] == 10 }
            .filter { scores[it rot 180] == 1 }
            .filter { scores[it rot 270] == 1 }
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
        // Get total error for this permutation
//        cornerSet.windowedPairsCircular().sumOf { (current, next) ->
//            // Calculate what the direction would be
//            val xDiff = next.first - current.first
//            val yDiff = next.second - current.second
//            val angle = (Math.toDegrees(atan(yDiff / xDiff.toDouble())) + 360) % 360
//            abs(angle - current.third)
//        }
        // Get rotation
        val differences = cornerSet.map { it.third }.zip(0..<360 step 90).map { (a, b) -> abs(a - b) }
        differences.max() - differences.min()
    }
    println(bestCorners)
    // Remake image as 300x300
    val srcImage = opencv_imgcodecs.imread("recognition/black_and_white.jpg")
    val destImage = performPerspectiveWarp(
        srcImage,
        bestCorners[0].first,
        bestCorners[0].second,
        bestCorners[1].first,
        bestCorners[1].second,
        bestCorners[2].first,
        bestCorners[2].second,
        bestCorners[3].first,
        bestCorners[3].second,
    )
    opencv_imgcodecs.imwrite("recognition/final_cv.jpg", destImage)
}

private infix fun Int.rot(degrees: Int) = (this + degrees) % 360

private fun transformAndSave(name: String, transformer: (BufferedImage) -> BufferedImage) {
    println("[$name] Transforming...")
    lastImage = transformer(lastImage)
    println("[$name] Finished transforming; saving...")
    ImageIO.write(lastImage, "jpg", File("recognition/$name.jpg"))
    println("[$name] Done")
}

private fun performPerspectiveWarp(
    imageMat: Mat,
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
    x3: Int,
    y3: Int,
    x4: Int,
    y4: Int
): Mat {

    val srcCorners = FloatPointer(
        x1.toFloat(), y1.toFloat(),
        x2.toFloat(), y2.toFloat(),
        x3.toFloat(), y3.toFloat(),
        x4.toFloat(), y4.toFloat()
    )


    val dstCorners = FloatPointer(
        0f, 0f,
        300f, 0f,
        300f, 300f,
        0f, 300f
    )

    //create matrices with width 2 to hold the x,y values, and 4 rows, to hold the 4 different corners.
    val src = Mat(Size(2, 4), CV_32F, srcCorners)
    val dst = Mat(Size(2, 4), CV_32F, dstCorners)

    val perspective: Mat = getPerspectiveTransform(src, dst)
    val result = Mat()
    warpPerspective(imageMat, result, perspective, Size(300, 300))

    src.release()
    dst.release()
    srcCorners.deallocate()
    dstCorners.deallocate()

    return result
}