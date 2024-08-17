package recognition

import mapToPairs
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc.getPerspectiveTransform
import org.bytedeco.opencv.global.opencv_imgproc.warpPerspective
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Size
import org.bytedeco.opencv.opencv_text.OCRTesseract
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.nd4j.linalg.cpu.nativecpu.NDArray
import org.opencv.core.CvType.CV_32F
import permute
import windowedPairsCircular
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JOptionPane
import javax.swing.JPanel
import kotlin.math.pow
import kotlin.math.roundToInt

private const val IMAGE_WIDTH = 800
private const val MARGIN = 0.05
private val CIRCLE_RADIUS = 5 to 10
private const val GRID_SIZE = 9 * 32 // 2 pixels each side to cut off lines

fun main() {
    // Load image
    val image = ImageIO.read(File("recognition/in2.jpg"))
    val sf = IMAGE_WIDTH / image.width.toDouble()
    val height = (sf * image.height).roundToInt()
    val transform = AffineTransform.getScaleInstance(sf, sf)
    // Initialise corners
    val margin = arrayOf((IMAGE_WIDTH * MARGIN).roundToInt(), (height * MARGIN).roundToInt())
    val end = arrayOf(IMAGE_WIDTH - margin[0], height - margin[1])
    val corners = mutableListOf(
        margin[0] to margin[1],
        end[0] to margin[1],
        end[0] to end[1],
        margin[0] to end[1]
    )
    var selectedCorner: Int? = null
    // Panel to select bounds
    val panel = object : JPanel() {
        override fun paint(graphics: Graphics?) {
            super.paint(graphics)
            val g = graphics!! as Graphics2D
            g.drawImage(image, transform, null)
            // Draw corners
            g.color = Color.RED
            corners.windowedPairsCircular().forEach { (a, b) ->
                g.drawLine(a.first, a.second, b.first, b.second)
            }
            corners.forEach { (x, y) ->
                // Inner circle
                g.color = Color.RED
                g.fillOval(x - CIRCLE_RADIUS.first, y - CIRCLE_RADIUS.first, CIRCLE_RADIUS.first * 2, CIRCLE_RADIUS.first * 2)
                // Outer
                g.color = Color(255, 0, 0, 128)
                g.fillOval(x - CIRCLE_RADIUS.second, y - CIRCLE_RADIUS.second, CIRCLE_RADIUS.second * 2, CIRCLE_RADIUS.second * 2)
            }
        }

        override fun getPreferredSize() = Dimension(IMAGE_WIDTH, height)
    }
    panel.addMouseListener(object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent?) {
            val index = corners.indexOfFirst { (x, y) ->
                (e!!.x - x).toDouble().pow(2.0) + (e.y - y).toDouble().pow(2.0) < 100
            }
            selectedCorner = if (index == -1) null else index
        }

        override fun mouseReleased(e: MouseEvent?) {
            selectedCorner = null
        }
    })
    panel.addMouseMotionListener(object : MouseMotionAdapter() {
        override fun mouseDragged(e: MouseEvent?) {
            if (selectedCorner != null) {
                corners[selectedCorner!!] = e!!.x to e.y
                panel.repaint()
            }
        }
    })
    // Show dialog
    JOptionPane.showConfirmDialog(null, panel, "Select grid bounds", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)
    val scaledCorners = corners.map { it.first / sf to it.second / sf }
    // Transform image
    println("Perspective transform")
    val stream = ByteArrayOutputStream()
    ImageIO.write(image, "jpg", stream)
    stream.flush()
    val srcImage = opencv_imgcodecs.imdecode(Mat(*stream.toByteArray()), 1)
    val srcCorners = FloatPointer(
        scaledCorners[0].first.toFloat(), scaledCorners[0].second.toFloat(),
        scaledCorners[1].first.toFloat(), scaledCorners[1].second.toFloat(),
        scaledCorners[2].first.toFloat(), scaledCorners[2].second.toFloat(),
        scaledCorners[3].first.toFloat(), scaledCorners[3].second.toFloat()
    )
    val floatSize = GRID_SIZE.toFloat()
    val dstCorners = FloatPointer(
        0f, 0f,
        floatSize, 0f,
        floatSize, floatSize,
        0f, floatSize
    )
    val src = Mat(Size(2, 4), CV_32F, srcCorners)
    val dst = Mat(Size(2, 4), CV_32F, dstCorners)
    val perspective: Mat = getPerspectiveTransform(src, dst)
    val result = Mat()
    warpPerspective(srcImage, result, perspective, Size(GRID_SIZE, GRID_SIZE))
    src.release()
    dst.release()
    srcCorners.deallocate()
    dstCorners.deallocate()
    opencv_imgcodecs.imwrite("recognition/transformed.jpg", result)
    val transformed = ImageIO.read(File("recognition/transformed.jpg"))
    // Grayscale
    println("Grayscale")
    val grayImage = BufferedImage(GRID_SIZE, GRID_SIZE, BufferedImage.TYPE_INT_RGB)
    permute(0..<GRID_SIZE, 0..<GRID_SIZE)
        .forEach { (x, y) ->
            val rgb = transformed.getRGB(x, y)
            val r = (rgb shr 16) and 0xff
            val g = (rgb shr 8) and 0xff
            val b = rgb and 0xff
            val full = 255 - (r + g + b) / 3
            val gray = if (full >= 128) 255 else 0
            val newRgb = gray or (gray shl 8) or (gray shl 16)
            grayImage.setRGB(x, y, newRgb)
        }
    ImageIO.write(grayImage, "jpg", File("recognition/gray.jpg"))
    // Get digit recognition model
    val model = MultiLayerNetwork.load(File("recognition/mnist_model"), false)
    val offsetRange = 2..<30
    val cells = permute(0..<9, 0..<9).mapToPairs().associateWith { (yCell, xCell) ->
        val cellImage = BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB)
        permute(offsetRange, offsetRange).map { (yOffset, xOffset) ->
            val x = xCell * 32 + xOffset
            val y = yCell * 32 + yOffset
            val color = grayImage.getRGB(x, y)
            cellImage.setRGB(xOffset - 2, yOffset - 2, color)
            (color and 0xff) / 255f
        }.toList().toFloatArray()
            .also { ImageIO.write(cellImage, "jpg", File("recognition/cell/$xCell,$yCell.jpg")) }
    }
    val values = cells.mapValues { (_, pixels) ->
        val ratings = model.feedForward(NDArray(pixels)).last()
        (0..9).associateWith { ratings.getDouble(0, it) }
    }
    val best = values.mapValues { (_, ratings) ->
        val dev = StandardDeviation(false).evaluate(ratings.values.toDoubleArray())
        if (dev >= 0.1) ratings.maxBy { it.value }.key else null
    }
    best.forEach { (coords, value) -> println("$coords: $value") }
}