import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

fun main() {
    // Get grid
    val cacheFile = File("grid_cache.txt")
    val gridStr = if (cacheFile.exists()) {
        println("Using grid cache")
        BufferedReader(FileReader(cacheFile))
            .use { it.readLine() }
    } else {
        print("Enter grid: ")
        readln().also {
            BufferedWriter(FileWriter(cacheFile))
                .use { writer -> writer.write(it) }
        }
    }
    val known = mutableMapOf<Coordinate, Int>()
    gridStr.forEachIndexed { i, c ->
        if (c == ' ') return@forEachIndexed
        val x = i % 9
        val y = i.floorDiv(9)
        known[x to y] = c.digitToInt()
    }
    // Solve
    val solver = Solver(known)
    solver.solve()
    solver.show()
}