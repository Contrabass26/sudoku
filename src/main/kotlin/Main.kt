const val numbersStr = " 5   9      4 76  7    1  95    4     8 5  7 23  1      6   2      9     81   735"

fun main() {
    val grid = Grid(numbersStr.chunked(9)
        .map { row -> row
            .map { if (it == ' ') null else it.digitToInt() }
            .toTypedArray() }
        .toTypedArray())
    grid.solve()
    println(grid)
}