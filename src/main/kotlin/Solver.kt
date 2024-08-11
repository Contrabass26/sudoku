class Solver(known: Map<Coordinate, Int>) {

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
        var modified = true
        while (modified) {
            modified = false
            Grid.locations().associateWith { grid.get(it) }.filterValues { it?.size != 1 }.forEach { (pair, current) ->
                val candidates = (1..9).toMutableList()
                val toRemove = SquareCollection
                    .affectedBy(pair.first, pair.second)
                    .mapNotNull { (x, y) -> grid.getDefinite(x, y) }
                    .toSet()
                candidates.removeAll(toRemove)
                if (current != candidates) {
                    // Only replace if the new list is shorter (or it's the only option)
                    if (current == null || candidates.size < current.size) {
                        modified = true
                        grid.set(pair, candidates)
                    }
                }
            }
        }
    }

    fun solveOnlyOption(): Boolean {
        println("New iteration of only option")
        var modified = false
        val collections = SquareCollection.all()
        for (collection in collections) {
            for (i in 1..9) {
                // Get cells that could be i
                val candidates = collection.associateWith { grid.get(it)!! }.filterValues { it.contains(i) }
                if (candidates.size == 1) {
                    val candidate = candidates.entries.first()
                    if (candidate.value.size != 1) { // Make sure it's not already certain
                        candidate.value.removeIf { it != i }
                        modified = true
                        SquareCollection.affectedBy(candidate.key)
                            .map { grid.get(it)!! }
                            .forEach { it.remove(i) }
                        println("Only option: ${candidate.key} is the only candidate for $i")
                    }
                } else if (candidates.isNotEmpty()) {
                    // Do the candidates share multiple collections?
                    val otherCollection = candidates.keys
                        .map { (x, y) -> SquareCollection.containing(x, y).toList().filterNot { it == collection } }
                        .intersect()
                        .firstOrNull()
                    if (otherCollection != null) {
                        // Remove i from all other members of this collection
                        val toRemoveFrom = otherCollection.filter { it !in candidates.keys && i in grid.get(it)!! }
                        if (toRemoveFrom.isNotEmpty()) {
                            println("Only option: ${candidates.keys} are also all in $otherCollection")
                            modified = true
                            toRemoveFrom
                                .map { grid.get(it)!! }
                                .forEach { it.remove(i) }
                        }
                    }
                }
            }
        }
        return modified
    }

    fun solveCombos(length: Int): Boolean {
        var modified = false
        println("New iteration of combos ($length)")
        val collections = grid.getValuedCollections()
        for (collection in collections) {
            if (collection.count { it.value.size != 1 } <= length) continue // If there's not enough blank squares for the combo size
            val filtered = collection.filterValues { it.size in 2..length }.entries.toMutableList()
            checkCombos(length, filtered).forEach { combo ->
                val participants = combo.map { it.value }.foldToSet()
                val others = collection
                    .filterKeys { key -> key !in combo.map { it.key } }
                    .filterValues { it.containsAny(participants) } // Make sure there's something to do
                if (others.isNotEmpty()) {
                    others.forEach { it.value.removeAll(participants) }
                    modified = true
                    println("Combos ($length): ${combo.map { it.key }} cover $participants")
                }
            }
        }
        return modified
    }

    private fun checkCombos(length: Int, collection: MutableList<Map.Entry<Coordinate, Possibilities>>, vararg knownIndices: Int): Sequence<List<Map.Entry<Coordinate, Possibilities>>> = sequence {
        val known = knownIndices.map { collection[it] }.toSet()
        if (known.map { it.value }.foldToSet().size > length) return@sequence
        if (knownIndices.size == length) yield(known.toList())
        else (0..<collection.size).forEach { i ->
            if (i in knownIndices) return@forEach // Already part of the current combo
            yieldAll(checkCombos(length, collection, *knownIndices, i))
        }
    }

    fun solve() {
        var justCalculated = true
        var modified = false
        while (justCalculated || modified) {
            modified = solveOnlyOption() || solveCombos(2) || solveCombos(3)
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
                val value = if (options?.size == 1) options.first().toString() else " "
                print(value)
            }
            println()
        }
    }
}