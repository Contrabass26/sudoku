import kotlin.math.roundToInt

typealias Coordinate = Pair<Int, Int>
typealias Possibilities = MutableList<Int>

inline fun <reified T> repeatedArray(element: T, length: Int): Array<T> {
    return sequence {
        for (i in 1..length) {
            this.yield(element)
        }
    }.toList().toTypedArray()
}

fun <T> permute(vararg iterables: Iterable<T>) = object : Iterator<List<T>> {
    var next: Array<Int>? = repeatedArray(0, iterables.size)

    override fun hasNext() = next != null

    override fun next(): List<T> {
        val indices = next?.copyOf() ?: throw IllegalStateException("No next element is available")
        increment()
        return indices.mapIndexed { i, value -> iterables[i].elementAt(value) }
    }

    private fun increment(i: Int = next!!.size - 1) {
        if (next!![i] == iterables[i].count() - 1) {
            if (i == 0) {
                next = null
            } else {
                next!![i] = 0
                increment(i - 1)
            }
        } else {
            next!![i]++
        }
    }
}.asSequence()

fun <T> MutableList<T>.removeFirstOrNull(predicate: (T) -> Boolean): T? {
    val value = this.firstOrNull(predicate)
    value?.let(::remove)
    return value
}

fun <T> Iterable<Iterable<T>>.foldToSet() = fold(mutableSetOf<T>()) { set, additives ->
    set.addAll(additives)
    set
}

fun <T> Iterable<T>.containsAny(other: Iterable<T>) = other.any { contains(it) }

fun <T> Iterable<Iterable<T>>.intersect() = this.fold(this.first()) { a, b -> a.intersect(b.toSet()) }

fun <T : Comparable<T>> max(vararg values: T) = values.max()

fun <T> Iterable<T>.windowedPairsCircular(): List<List<T>> {
    val lists = windowed(2).toMutableList()
    val extra = listOf(last(), first())
    lists.add(extra)
    return lists
}

fun roundToNearest(n: Double, gap: Double): Double {
    return (n / gap).roundToInt() * gap
}

fun <T> Iterable<T>.sample(proportion: Double): List<T> {
    val count = (count() * proportion).roundToInt()
    return shuffled().take(count)
}

inline fun <reified T> repeatedArray(length: Int, crossinline element: () -> T): Array<T> {
    return sequence {
        for (i in 1..length) {
            this.yield(element())
        }
    }.toList().toTypedArray()
}

fun <T> Sequence<List<T>>.mapToPairs() = map { (a, b) -> a to b }