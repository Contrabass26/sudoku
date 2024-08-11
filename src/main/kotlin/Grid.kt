class Grid {

    private val map = mutableMapOf<Coordinate, Possibilities>()

    companion object {
        fun locations() = permute(0..8, 0..8).map { (x, y) -> x to y }
    }

    val set = CoordinateConsumer<Possibilities> { pair, value ->
        map[pair] = value
    }

    val get = CoordinateFunction { pair -> map[pair] }

    val getDefinite = CoordinateFunction { x, y ->
        val options = get(x, y)
        if (options?.size == 1) options.first() else null
    }

    val contains = CoordinateFunction(map::contains)

    fun getValuedCollections() = SquareCollection.all().map { collection -> collection.associateWith { get(it)!! } }
}