class Grid {

    private val map = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()

    companion object {
        fun locations() = permute(0..8, 0..8).map { (x, y) -> x to y }
    }

    val set = CoordinateConsumer<MutableList<Int>> { pair, value ->
        map[pair] = value
    }

    val get = CoordinateFunction { pair -> map[pair] }

    fun getRow(y: Int): List<Pair<Int, Int>> {
        return (0..8).map { it to y }
    }

    fun getColumn(x: Int): List<Pair<Int, Int>> {
        return (0..8).map { x to it }
    }

    fun getRows() = (0..8).map { getRow(it) }

    fun getColumns() = (0..8).map { getColumn(it) }

    val getDefinite = CoordinateFunction { x, y ->
        val options = get(x, y)
        if (options?.size == 1) options.first() else null
    }

    val contains = CoordinateFunction(map::contains)

    val getSquare = CoordinateFunction { x, y ->
        permute(0..2, 0..2)
            .map { (x1, y1) -> (x * 3 + x1) to (y * 3 + y1) }
            .toList()
    }

    val getSquareOf = CoordinateFunction { x, y ->
        getSquare(x.floorDiv(3), y.floorDiv(3))
    }

    fun getSquares() = permute(0..2, 0..2).map { (x, y) -> getSquare(x, y) }.toList()

    fun getCollections() = getRows() + getColumns() + getSquares()

    fun getValuedCollections() = getCollections().map { collection -> collection.associateWith { get(it)!! } }

    val getAffected = CoordinateFunction { x, y ->
        listOf(
            getRow(y),
            getColumn(x),
            getSquareOf(x, y)
        ).foldToSet().toList() - (x to y)
    }
}