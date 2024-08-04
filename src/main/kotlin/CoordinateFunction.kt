class CoordinateFunction<T>(private val partsFunction: (Int, Int) -> T, private val pairFunction: (Pair<Int, Int>) -> T) {

    constructor(partsFunction: (Int, Int) -> T) : this(partsFunction, { partsFunction(it.first, it.second) })

    constructor(pairFunction: (Pair<Int, Int>) -> T) : this({ x, y -> pairFunction(x to y) }, pairFunction)

    operator fun invoke(x: Int, y: Int) = partsFunction(x, y)

    operator fun invoke(pair: Pair<Int, Int>) = pairFunction(pair)
}

class CoordinateConsumer<T>(private val partsFunction: (Int, Int, T) -> Unit, private val pairFunction: (Pair<Int, Int>, T) -> Unit) {

    constructor(partsFunction: (Int, Int, T) -> Unit) : this(partsFunction, { pair, t -> partsFunction(pair.first, pair.second, t) })

    constructor(pairFunction: (Pair<Int, Int>, T) -> Unit) : this({ x, y, t -> pairFunction(x to y, t) }, pairFunction)

    operator fun invoke(x: Int, y: Int, t: T) = partsFunction(x, y, t)

    operator fun invoke(pair: Pair<Int, Int>, t: T) = pairFunction(pair, t)
}