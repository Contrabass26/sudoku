package recognition

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.cpu.nativecpu.NDArray
import org.nd4j.linalg.learning.config.Sgd
import permute
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JOptionPane
import javax.swing.JPanel
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val IMAGE_WIDTH = 300
private val LAYERS = arrayOf(81, 30, 7)
private val OUTPUT_KEYS = arrayOf("Nothing", "Line", "Intersection", "Top-left", "Top-right", "Bottom-right", "Bottom-left")
private const val RNG_SEED = 26L
private val MODEL_FILE = File("recognition/model")
private val DATA_FILE = File("recognition/data.json")
private val MAPPER = ObjectMapper()

fun main() {
    // Load data file
    val data = Data.load()
    // Load image
    println("Load")
    val inImage = ImageIO.read(File("recognition/in.jpg"))
    // Scale
    println("Scale")
    val scaledImage = getImage("recognition/scale.jpg", data.hashes) {
        val sf = IMAGE_WIDTH / inImage.getWidth(null).toDouble()
        val height = (inImage.getHeight(null) * sf).roundToInt()
        val new = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_BYTE_GRAY)
        val graphics = new.graphics as Graphics2D
        val transform = AffineTransform.getScaleInstance(sf, sf)
        graphics.drawImage(inImage, transform, null)
        new
    }
    // Grayscale
    println("Grayscale")
    val height = scaledImage.height
    val grayImage = getImage("recognition/gray.jpg", data.hashes) {
        val grayImage = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_INT_RGB)
        permute(0..<IMAGE_WIDTH, 0..<height).map { (x, y) -> x to y }
            .forEach { (x, y) ->
                val rgb = scaledImage.getRGB(x, y)
                val r = (rgb shr 16) and 0xff
                val g = (rgb shr 8) and 0xff
                val b = rgb and 0xff
                val gray = (r + g + b) / 3
                val newRgb = gray or (gray shl 8) or (gray shl 16)
                grayImage.setRGB(x, y, newRgb)
            }
        grayImage
    }
    // Function to get gray value from image
    val getGray: (Int, Int) -> Float? = { x, y ->
        if (x in 0..<IMAGE_WIDTH && y in 0..<height)
            (grayImage.getRGB(x, y) and 0xff) / 255f
        else null
    }
    // Neural network
    println("Initialise neural network")
    val model = if (MODEL_FILE.exists()) {
        MultiLayerNetwork.load(MODEL_FILE, true)
    } else {
        val config = NeuralNetConfiguration.Builder()
            .weightInit(WeightInit.XAVIER)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .updater(Sgd(0.05))
            .seed(RNG_SEED)
            .list()
            .layer(
                0, DenseLayer.Builder()
                    .nIn(LAYERS[0])
                    .nOut(LAYERS[1])
                    .build()
            )
            .layer(
                1, OutputLayer.Builder()
                    .nOut(LAYERS[2])
                    .build()
            )
            .build()
        MultiLayerNetwork(config).also { it.init() }
    }
    model.setListeners(ScoreIterationListener(1))
    // Get training examples
    println("Get training examples")
    val squareSize = sqrt(LAYERS[0].toDouble()).toInt()
    val halfSquare = (squareSize - 1) / 2
    val squareRange = -halfSquare..halfSquare
    val trainingExamples = permute(0..<IMAGE_WIDTH, 0..<height).map { (xStart, yStart) ->
        val square = permute(squareRange, squareRange).map { (yOffset, xOffset) ->
            val x = xStart + xOffset
            val y = yStart + yOffset
            getGray(x, y)
        }.toList()
        val average = square.filterNotNull().average().toFloat()
        square.map { it ?: average }.toFloatArray()
    }.sortedBy { Objects.hash(it) } // Same "random" order every time
    // Train
    val classify: (FloatArray) -> Pair<String, Double> = { example ->
        val input = NDArray(example)
        val current = model.feedForward(input).last()
        (0..<7)
            .associateWith { current.getDouble(it) }
            .mapKeys { OUTPUT_KEYS[it.key] }
            .maxBy { it.value }.toPair()
    }
    // Iterate from saved point
    trainingExamples.drop(data.next)
    println("Training from ${data.next}")
    trainingExamples.forEach { example ->
        val panel = object : JPanel() {
            override fun paint(g: Graphics?) {
                example.forEachIndexed { i, value ->
                    val x = (i % squareSize) * 30
                    val y = i.floorDiv(squareSize) * 30
                    g!!.color = value.let { Color(it, it, it) }
                    g.fillRect(x, y, 30, 30)
                    g.color = Color.BLACK
                    g.drawRect(x, y, 30, 30)
                }
                g!!.color = Color.RED
                g.drawRect(halfSquare * 30, halfSquare * 30, 30, 30)
            }

            override fun getPreferredSize() = Dimension(270, 270)
        }
        val current = classify(example)
        val result = JOptionPane.showOptionDialog(
            null,
            panel,
            "What is this? $current",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            OUTPUT_KEYS,
            "Nothing"
        )
        if (result == JOptionPane.CLOSED_OPTION) {
            save(model, data)
            return
        }
        val labels = FloatArray(7)
        labels[result] = 1f
        model.fit(NDArray(example), NDArray(labels))
        data.next++
    }
    save(model, data)
}

private fun getImage(filename: String, hashes: MutableMap<String, Int>, orElse: () -> BufferedImage): BufferedImage {
    val file = File(filename)
    if (file.exists()) {
        val hash = hashes[filename]
        if (hash != null) {
            val savedImage = ImageIO.read(file)
            if (hash == Objects.hash(savedImage)) {
                println("Loaded $filename from file")
                return savedImage
            }
        }
    }
    // Fallback
    val image = orElse()
    ImageIO.write(image, "jpg", file)
    hashes[filename] = Objects.hash(image)
    return image
}

private fun save(model: MultiLayerNetwork, data: Data) {
    println("Save")
    model.save(MODEL_FILE)
    data.save()
}

@JsonDeserialize(using = DataDeserialiser::class)
data class Data(var next: Int, val hashes: MutableMap<String, Int>) {
    companion object {
        private fun default() = Data(0, mutableMapOf())

        fun load(): Data {
            return if (DATA_FILE.exists())
                MAPPER.readValue(DATA_FILE, Data::class.java)
            else default()
        }
    }

    fun save() {
        MAPPER.writeValue(DATA_FILE, this)
    }
}

class DataDeserialiser : StdDeserializer<Data>(Data::class.java) {
    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): Data {
        val node: JsonNode = parser!!.codec.readTree(parser)
        val next = node.get("next").intValue()
        val hashes = mutableMapOf<String, Int>()
        node.get("hashes").fields().forEach { (key, value) ->
            hashes[key] = value.intValue()
        }
        return Data(next, hashes)
    }
}