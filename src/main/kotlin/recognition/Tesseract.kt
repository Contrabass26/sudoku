package recognition

import Solver
import mapToPairs
import net.sourceforge.tess4j.Tesseract
import org.bytedeco.javacpp.FloatPointer
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc.getPerspectiveTransform
import org.bytedeco.opencv.global.opencv_imgproc.warpPerspective
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Size
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
private const val GRID_SIZE = 9 * 32

fun main() {
    // Load image
    val image = ImageIO.read(File("recognition/in4.jpg"))
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
    // Split up into cells
    val offsetRange = 2..<30
    val cells = permute(0..<9, 0..<9).mapToPairs().associateWith { (xCell, yCell) ->
        val cellImage = BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB)
        permute(offsetRange, offsetRange).forEach { (yOffset, xOffset) ->
            val x = xCell * 32 + xOffset
            val y = yCell * 32 + yOffset
            val color = grayImage.getRGB(x, y)
            cellImage.setRGB(xOffset - 2, yOffset - 2, color)
        }
        ImageIO.write(cellImage, "jpg", File("recognition/cell/$xCell,$yCell.jpg"))
        cellImage
    }
    // Tesseract
    val tesseract = Tesseract()
    tesseract.setDatapath("recognition/tessdata")
    tesseract.setVariable("tessedit_char_whitelist", "123456789")
    val cellValues = cells.mapValues { (_, image) ->
        val string = tesseract.doOCR(image).filterNot { it.isWhitespace() }
        if (string.isEmpty()) null else string.toInt()
    }.filterValues { it != null }.mapValues { it.value!! }.toMutableMap()
    cellValues.forEach { (coords, value) -> println("$coords: $value") }
    // Let the user check
    val squareSize = GRID_SIZE / 9
    val checkPanel = object : JPanel() {
        override fun paint(graphics: Graphics?) {
            super.paint(graphics)
            val g = graphics!! as Graphics2D
            // Draw image
            g.drawImage(transformed, 0, 0, null)
            // Translucent rectangle
            g.color = Color(1f, 1f, 1f, 0.5f)
            g.fillRect(0, 0, width, this.height)
            // Labels over the top
            g.font = g.font.deriveFont(26f)
            g.color = Color.RED
            cellValues.forEach { (coords, value) ->
                val stringBounds = g.fontMetrics.getStringBounds(value.toString(), g)
                val x = (coords.first + 0.5) * squareSize - stringBounds.width * 0.5
                val y = (coords.second + 0.5) * squareSize + stringBounds.height * 0.5
                g.drawString(value.toString(), x.roundToInt(), y.roundToInt())
            }
        }

        override fun getPreferredSize() = Dimension(GRID_SIZE, GRID_SIZE)
    }
    checkPanel.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
            val x = e!!.x / squareSize
            val y = e.y / squareSize
            val newValue = JOptionPane.showInputDialog(null, "Enter new value", "Set value", JOptionPane.QUESTION_MESSAGE)
            if (newValue.isEmpty()) {
                cellValues.remove(x to y)
            } else {
                cellValues[x to y] = newValue.toInt()
            }
            checkPanel.repaint()
        }
    })
    JOptionPane.showConfirmDialog(null, checkPanel, "Check digits", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)
    // Solve
    val solver = Solver(cellValues)
    solver.solve()
    solver.show()
}