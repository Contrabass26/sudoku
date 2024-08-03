fun main() {
    val grid = Grid(
        0 to 0 to 6,
        1 to 1 to 5,
        2 to 1 to 7,
        6 to 1 to 8,
        3 to 2 to 1,
        5 to 2 to 4,
        7 to 2 to 9,
        0 to 3 to 4,
        1 to 3 to 1,
        4 to 3 to 9,
        5 to 4 to 3,
        7 to 4 to 7,
        1 to 5 to 9,
        2 to 5 to 8,
        3 to 6 to 4,
        6 to 6 to 5,
        7 to 6 to 6,
        5 to 7 to 8,
        6 to 7 to 3,
        3 to 8 to 3,
        4 to 8 to 1,
        5 to 8 to 7,
        8 to 8 to 4
    )
    println(grid[0, 0])
    grid.solve()
    grid.show()
}