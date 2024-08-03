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

fun baseNCounter(base: Int, digits: Int) = permute(*repeatedArray(0..<base, digits))

fun <T, R> Sequence<T>.associateWithIndexed(valueFunction: (Int, T) -> R): Map<T, R> {
    val map = mutableMapOf<T, R>()
    forEachIndexed { i, key ->
        val value = valueFunction(i, key)
        map[key] = value
    }
    return map
}

fun <T, R> Sequence<T>.reverseAssociateWithIndexed(keyFunction: (Int, T) -> R): Map<R, T> {
    val map = mutableMapOf<R, T>()
    forEachIndexed { i, value ->
        val key = keyFunction(i, value)
        map[key] = value
    }
    return map
}

fun <T, R> Sequence<T>.reverseAssociateWith(keyFunction: (T) -> R): Map<R, T> {
    val map = mutableMapOf<R, T>()
    forEach {
        val key = keyFunction(it)
        map[key] = it
    }
    return map
}