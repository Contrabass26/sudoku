class Grid(vararg knownValues: Pair<Pair<Int, Int>, Int>) {

    private val possibilities = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()

    init {
        knownValues.forEach { (key, value) -> possibilities[key] = mutableListOf(value) }
    }

    fun getRow(y: Int) = (0..8)
        .map { it to y }
        .associateWith { (x, y) -> this[x, y] }

    fun getRowDefinite(y: Int) = possibilities.filterKeys { (_, y1) -> y1 == y }

    fun getRows() = (0..8).map { getRow(it) }

    fun getColumn(x: Int) = (0..8)
        .map { x to it }
        .associateWith { (x, y) -> this[x, y] }

    fun getColumnDefinite(x: Int) = possibilities.filterKeys { (x1, _) -> x1 == x }

    fun getColumns() = (0..8).map { getColumn(it) }

    fun getSquare(x: Int, y: Int) = permute(0..2, 0..2)
        .map { (x1, y1) -> (x * 3 + x1) to (y * 3 + y1) }
        .associateWith { (x, y) -> this[x, y] }

    fun getSquareDefinite(x: Int, y: Int) = possibilities.filterKeys { (x1, y1) -> x1.floorDiv(3) == x && y1.floorDiv(3) == y }

    fun getSquareOf(x: Int, y: Int) = getSquare(x.floorDiv(3), y.floorDiv(3))

    fun getSquareOfDefinite(x: Int, y: Int) = getSquareDefinite(x.floorDiv(3), y.floorDiv(3))

    fun getSquares() = permute(0..2, 0..2).map { (x, y) -> getSquare(x, y) }

    private fun getCollections() = getRows() + getColumns() + getSquares()

    private fun MutableList<Int>.removeDefinite(values: Iterable<Iterable<Int>>) {
        this.removeAll(values
            .filter { it.count() == 1 }
            .map { it.first() }
        )
    }

    private fun <T> MutableList<T>.removeFirstOrNull(predicate: (T) -> Boolean): T? {
        val value = this.firstOrNull(predicate)
        value?.let(::remove)
        return value
    }

    operator fun get(x: Int, y: Int): MutableList<Int> {
        return possibilities.getOrPut(x to y) {
            val canBe = (1..9).toMutableList()
            canBe.removeDefinite(getRowDefinite(y).values)
            canBe.removeDefinite(getColumnDefinite(x).values)
            canBe.removeDefinite(getSquareOfDefinite(x, y).values)
            object : ArrayList<Int>(canBe) {
                override fun remove(element: Int): Boolean {
                    val toReturn = super.remove(element)
                    possibilities.remove(x to y)
                    return toReturn
                }
            }
        }
    }

    fun solveOnlyOption(): Boolean {
        println("New iteration of only option")
        var modified = false
        val collections = getCollections()
        for (collection in collections) {
            for (i in 1..9) {
                val candidates = collection.filterValues { it.contains(i) && it.size != 1 }
                if (candidates.size == 1) {
                    candidates.values.first().removeIf { it != i }
                    modified = true
                    val location = candidates.keys.first()
                    println("Only option: $location is the only candidate for $i")
                }
            }
        }
        return modified
    }

    fun solve() {
        while (solveOnlyOption()) { continue }
    }

    fun show() {
        for (y in 0..8) {
            for (x in 0..8) {
                val options = this[x, y]
                val value = if (options.size == 1) options.first().toString() else " "
                print(value)
            }
            println()
        }
    }
}