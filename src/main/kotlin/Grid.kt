import kotlin.math.sqrt

class Grid(private val numbers: Array<Array<Int?>>) {

    private val sideLength by lazy { numbers.size }
    private val length by lazy { sideLength * sideLength }
    private val squareSideLength by lazy { sqrt(sideLength.toDouble()).toInt() }

    override fun toString(): String {
        return numbers
            .map { row ->
                row
                    .joinToString("") { it?.toString() ?: " " }
                    .chunked(squareSideLength)
                    .joinToString("|")
            }
            .chunked(squareSideLength).joinToString(
                "\n"
                        + "-"
                    .repeat(sideLength)
                    .chunked(squareSideLength)
                    .joinToString("+")
                        + "\n"
            ) { it.joinToString("\n") }
    }

    operator fun get(column: Int, row: Int) = numbers[row][column]

    fun getRow(row: Int) = numbers[row].asSequence()

    fun getColumn(column: Int) = numbers.map { it[column] }.asSequence()

    fun getSquare(x: Int, y: Int) = sequence {
        for (x1 in 0..2) {
            for (y1 in 0..2) {
                val x2 = squareSideLength * x + x1
                val y2 = squareSideLength * y + y1
                yield(numbers[y2][x2])
            }
        }
    }

    fun getSquareOf(x: Int, y: Int) = getSquare(
        x.floorDiv(squareSideLength),
        y.floorDiv(squareSideLength)
    )

    private fun solveObvious(): Boolean {
        var modified = false
        baseNCounter(sideLength, 2)
            .filter { (x, y) -> numbers[y][x] == null }
            .forEach { (x, y) ->
                val possibilities = getPossibilities(x, y)
                if (possibilities.size == 1) {
                    val option = possibilities.first()
                    println("($x, $y) can only be $option")
                    numbers[y][x] = option
                    modified = true
                }
            }
        return modified
    }

    private fun isValid(): Boolean {
        if ((0..<sideLength)
                .map { getRow(it).toSet() }
                .any { it.size != sideLength }) return false
        if ((0..<sideLength)
                .map { getColumn(it).toSet() }
                .any { it.size != sideLength }) return false
        return baseNCounter(squareSideLength, 2)
            .map { (x, y) -> getSquare(x, y).toSet() }
            .all { it.size != sideLength }
    }

    private fun copy(): Grid {
        return Grid(numbers.map { it.copyOf() }.toTypedArray())
    }

    private fun getPossibilities(x: Int, y: Int): List<Int> {
        numbers[y][x]?.let { return listOf(it) }
        val possibilities = (1..sideLength).toMutableList()
        getRow(y).forEach(possibilities::remove)
        getColumn(x).forEach(possibilities::remove)
        getSquareOf(x, y).forEach(possibilities::remove)
        return possibilities
    }

    private fun solveOnlyOption(): Boolean {
        var modified = false
        // Get all collections (rows, columns, squares)
        val rows = (0..<sideLength).map { getRow(it).reverseAssociateWithIndexed { i, _ -> it to i } }
        val columns = (0..<sideLength).map { getColumn(it).reverseAssociateWithIndexed { i, _ -> i to it } }
        val squareRange = 0..<squareSideLength
        val squares = permute(squareRange, squareRange).map { (x, y) -> getSquare(x, y).reverseAssociateWithIndexed { i, _ ->
            val xOffset = i.floorDiv(squareSideLength)
            val yOffset = i % squareSideLength
            x * squareSideLength + xOffset to y * squareSideLength + yOffset
        } }
        val collections = rows + columns + squares
        // Analyse each collection
        collections.forEach { collection ->
            val possibilities = collection.mapValues { getPossibilities(it.key.first, it.key.second) }
            for (value in 1..sideLength) {
                val canBeThis = possibilities.filterValues { value in it }
                if (canBeThis.size == 1) {
                    val key = canBeThis.keys.first()
                    val x = key.first
                    val y = key.second
                    if (numbers[y][x] == null) {
                        numbers[y][x] = value
                        println("solveOnlyOption: ($x, $y) is the only candidate for $value")
                        modified = true
                    }
                }
            }
        }
        return modified
    }

    fun solve() {
        // TODO: Look for pairs and triples, i.e. these two are 1 and 8 so no others can be 1 or 8
        while (solveObvious() || solveOnlyOption()) { continue }
        println(this)
        solveIterative()
    }

    private fun solveIterative() {
        val unknowns = baseNCounter(sideLength, 2)
            .filter { (x, y) -> numbers[y][x] == null }
            .map { (x, y) -> x to y }
            .toList()
        val possibilities = unknowns.associateWith { (x, y) -> getPossibilities(x, y) }
        possibilities.forEach { println(it) }
        println(possibilities.values.map { it.size.toULong() }.runningFold(1uL, ULong::times))
        val predictedCount = possibilities.values.map { it.size.toULong() }.fold(1uL, ULong::times)
        println("Probably checking $predictedCount permutations")
        val permutations = permute(*possibilities.values.toTypedArray()).map { mapOf(*unknowns.zip(it).toTypedArray()) }
        for (permutation in permutations) {
            val gridCopy = copy()
            permutation.forEach { (x, y), value -> gridCopy.numbers[y][x] = value }
            if (gridCopy.isValid()) {
                println("Found working permutation: $permutation")
                break
            }
        }
    }
}