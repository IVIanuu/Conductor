package com.ivianuu.conductor

import android.os.Bundle
import java.util.*
import kotlin.collections.ArrayList

internal class Backstack : Iterable<RouterTransaction> {

    private val backstack = ArrayDeque<RouterTransaction>()

    val isEmpty
        get() = backstack.isEmpty()

    val size
        get() = backstack.size

    fun root(): RouterTransaction? {
        return if (backstack.size > 0) backstack.last else null
    }

    override fun iterator(): Iterator<RouterTransaction> {
        return backstack.iterator()
    }

    fun reverseIterator(): Iterator<RouterTransaction> {
        return backstack.descendingIterator()
    }

    fun pop(): RouterTransaction {
        val popped = backstack.pop()
        popped.controller.destroy()
        return popped
    }

    fun peek(): RouterTransaction? {
        return backstack.peek()
    }

    fun remove(transaction: RouterTransaction) {
        backstack.removeFirstOccurrence(transaction)
    }

    fun push(transaction: RouterTransaction) {
        backstack.push(transaction)
    }

    fun popAll(): List<RouterTransaction> {
        val list = mutableListOf<RouterTransaction>()
        while (!isEmpty) {
            list.add(pop())
        }
        return list
    }

    fun setBackstack(backstack: List<RouterTransaction>) {
        this.backstack.clear()
        backstack.forEach(this.backstack::push)
    }

    operator fun contains(transaction: RouterTransaction): Boolean {
        return backstack.contains(transaction)
    }

    fun saveInstanceState(outState: Bundle) {
        val entryBundles = mutableListOf<Bundle>()
        for (entry in backstack) {
            entryBundles.add(entry.saveInstanceState())
        }

        outState.putParcelableArrayList(KEY_ENTRIES, ArrayList(entryBundles))
    }

    fun restoreInstanceState(savedInstanceState: Bundle) {
        val entryBundles = savedInstanceState
            .getParcelableArrayList<Bundle>(KEY_ENTRIES)?.reversed()
        if (entryBundles != null) {
            for (transactionBundle in entryBundles) {
                backstack.push(RouterTransaction(transactionBundle))
            }
        }
    }

    companion object {
        private const val KEY_ENTRIES = "Backstack.entries"
    }
}
