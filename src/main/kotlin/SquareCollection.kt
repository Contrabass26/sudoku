abstract class SquareCollection : Iterable<Pair<Int, Int>> {

    companion object {
        val containing = CoordinateFunction { x, y -> Triple(Column(x), Row(y), Box.of(x, y)) }

        val affectedBy = CoordinateFunction { x, y -> containing(x, y).toList().fold(listOf<Pair<Int, Int>>()) { a, b -> a + b }.filterNot { it == x to y } }

        fun all() = Row.all() + Column.all() + Box.all()
    }

    abstract val indices: Iterable<Pair<Int, Int>>

    override fun iterator() = indices.iterator()

    override fun equals(other: Any?): Boolean {
        if (other is SquareCollection) {
            return indices == other.indices
        }
        return false
    }

    operator fun plus(other: SquareCollection) = indices + other.indices

    override fun hashCode() = indices.hashCode()

    class Row(private val y: Int) : SquareCollection() {
        companion object {
            fun all() = (0..8).map { Row(it) }
        }

        override val indices = (0..8).map { it to y }

        override fun toString() = "Row $y"
    }

    class Column(private val x: Int) : SquareCollection() {
        companion object {
            fun all() = (0..8).map { Column(it) }
        }

        override val indices = (0..8).map { x to it }

        override fun toString() = "Column $x"
    }

    class Box(private val x: Int, private val y: Int) : SquareCollection() {
        companion object {
            fun of(x: Int, y: Int) = Box(x.floorDiv(3), y.floorDiv(3))

            fun all() = permute(0..2, 0..2).map { (x, y) -> Box(x, y) }
        }

        override val indices = permute(0..2, 0..2)
            .map { (x1, y1) -> (x * 3 + x1) to (y * 3 + y1) }
            .toList()

        override fun toString() = "Box ($x, $y)"
    }
}