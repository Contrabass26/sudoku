package recognition

import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.api.iterator.BaseDatasetIterator
import org.nd4j.linalg.learning.config.Sgd
import java.awt.Graphics2D
import java.awt.Toolkit
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.io.File
import javax.imageio.ImageIO
import javax.swing.GrayFilter
import kotlin.math.roundToInt

private const val IMAGE_WIDTH = 300
val LAYERS = arrayOf(10000, 100, 2)
const val RNG_SEED = 26L

fun main() {
    // Grayscale
    val inImage = ImageIO.read(File("recognition/in.jpg"))
    val grayFilter = GrayFilter(true, 50)
    val grayProducer = FilteredImageSource(inImage.source, grayFilter)
    val grayImage = Toolkit.getDefaultToolkit().createImage(grayProducer)
    // Scale
    val sf = IMAGE_WIDTH / grayImage.getWidth(null).toDouble()
    val height = (grayImage.getHeight(null) * sf).roundToInt()
    val scaledImage = BufferedImage(IMAGE_WIDTH, height, BufferedImage.TYPE_BYTE_GRAY)
    val graphics = scaledImage.graphics as Graphics2D
    val transform = AffineTransform.getScaleInstance(sf, sf)
    graphics.drawImage(grayImage, transform, null)
    // Save
    ImageIO.write(scaledImage, "jpg", File("recognition/scale.jpg"))
    // Neural net
    val config = NeuralNetConfiguration.Builder()
        .weightInit(WeightInit.XAVIER)
        .activation(Activation.RELU)
        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
        .updater(Sgd(0.05))
        .seed(RNG_SEED)
        .list()
        .layer(0, DenseLayer.Builder()
            .nIn(IMAGE_WIDTH * height)
            .nOut(LAYERS[0])
            .build())
        .layer(1, DenseLayer.Builder()
            .nIn(LAYERS[0])
            .nOut(LAYERS[1])
            .build())
        .layer(2, OutputLayer.Builder()
            .nIn(LAYERS[1])
            .nOut(LAYERS[2])
            .build())
        .build()
    val model = MultiLayerNetwork(config)
    model.init()
    model.setListeners(ScoreIterationListener(1))
}