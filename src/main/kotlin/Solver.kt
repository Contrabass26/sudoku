class Solver(known: Map<Pair<Int, Int>, Int>) {

    private val grid = Grid()

    init {
        // Put the known ones in first
        known.forEach { (pair, value) ->
            grid.set(pair, mutableListOf(value))
        }
        // Now calculate possibilities for the rest
        calculateIndefinite()
    }

    fun calculateIndefinite() {
        Grid.locations().filter { !grid.contains(it) || grid.get(it).size != 1 }.forEach { pair ->
            val candidates = (1..9).toMutableList()
            val toRemove = grid
                .getAffected(pair.first, pair.second)
                .mapNotNull { (x, y) -> grid.getDefinite(x, y) }
                .toSet()
            candidates.removeAll(toRemove)
            grid.set(pair, candidates)
        }
    }

    fun solveOnlyOption(): Boolean {
        println("New iteration of only option")
        var modified = false
        val collections = grid.getValuedCollections()
        for (collection in collections) {
            for (i in 1..9) {
                // Get cells that could be i
                val candidates = collection.filterValues { it.contains(i) }
                if (candidates.size == 1) {
                    val candidate = candidates.entries.first()
                    if (candidate.value.size != 1) { // Make sure it's not already certain
                        candidate.value.removeIf { it != i }
                        modified = true
                        grid.getAffected(candidate.key)
                            .map { grid.get(it) }
                            .forEach { it.remove(i) }
                        println("Only option: ${candidate.key} is the only candidate for $i")
                    }
                }
            }
        }
        return modified
    }

    fun solvePairs(): Boolean {
        // TODO: Expand to triples and higher
        var modified = false
        println("New iteration of pairs")
        val collections = grid.getValuedCollections()
        for (collection in collections) {
            val filtered = collection.filterValues { it.size == 2 }.entries.toMutableList()
            while (filtered.isNotEmpty()) {
                val first = filtered.removeFirst()
                val second = filtered.removeFirstOrNull { it.value == first.value }
                if (second != null) {
                    val others = collection
                        .filterKeys { it != first.key && it != second.key }
                        .filterValues { it.containsAny(first.value) } // Make sure there's something to do
                    if (others.isNotEmpty()) {
                        others.forEach { it.value.removeAll(first.value) }
                        modified = true
                        println("Pairs: ${first.key} and ${second.key} cover ${first.value}")
                    }
                }
            }
        }
        return modified
    }

    fun solve() {
        var justCalculated = true
        var modified = false
        while (justCalculated || modified) {
            modified = solveOnlyOption() || solvePairs()
            justCalculated = if (!modified && !justCalculated) {
                println("Recalculating")
                calculateIndefinite()
                true
            } else false
        }
    }

    fun show() {
        for (y in 0..8) {
            for (x in 0..8) {
                val options = grid.get(x, y)
                val value = if (options.size == 1) options.first().toString() else " "
                print(value)
            }
            println()
        }
    }
}