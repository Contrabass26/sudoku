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