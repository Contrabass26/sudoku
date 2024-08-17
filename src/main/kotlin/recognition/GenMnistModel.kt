package recognition

import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Nesterovs
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File

private const val BATCH_SIZE = 128
private const val SEED = 12345
private const val EPOCHS = 15

fun main() {
    // Setup network
    println("Neural network setup")
    val trainData = MnistDataSetIterator(BATCH_SIZE, true, SEED)
    val config = NeuralNetConfiguration.Builder()
        .seed(SEED.toLong()) // include a random seed for reproducibility
        // use stochastic gradient descent as an optimization algorithm
        .updater(Nesterovs(0.006, 0.9))
        .l2(1e-4)
        .list()
        .layer(
            DenseLayer.Builder() // create the first, input layer with xavier initialization
                .nIn(784)
                .nOut(1000)
                .activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .build()
        )
        .layer(
            OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) // create hidden layer
                .nIn(1000)
                .nOut(10)
                .activation(Activation.SOFTMAX)
                .weightInit(WeightInit.XAVIER)
                .build()
        )
        .build()
    println("Create model")
    val model = MultiLayerNetwork(config)
    model.init()
    model.setListeners(ScoreIterationListener(1))
    println("Train model")
    model.fit(trainData, EPOCHS)
    println("Save model")
    model.save(File("recognition/mnist_model"))
}