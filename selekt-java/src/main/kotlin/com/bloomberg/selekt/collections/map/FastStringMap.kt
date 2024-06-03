/*
 * Copyright 2024 Bloomberg Finance L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bloomberg.selekt.collections.map

/**
 * @param capacity a power of two.
 */
open class FastStringMap<T>(capacity: Int) {
    val size: Int
        inline get() = _size
    var _size: Int = 0
    val store = arrayOfNulls<Entry<T>>(capacity)
    private val hashLimit = capacity - 1

    fun isEmpty() = 0 == _size

    fun containsKey(key: String): Boolean {
        val hashCode = hash(key)
        val index = hashIndex(hashCode)
        var entry = store[index]
        while (entry != null) {
            if (entry.hashCode == hashCode && entry.key == key) {
                return true
            }
            entry = entry.after
        }
        return false
    }

    inline fun getEntryElsePut(
        key: String,
        supplier: () -> T
    ): Entry<T> {
        val hashCode = hash(key)
        val index = hashIndex(hashCode)
        var entry = store[index]
        while (entry != null) {
            if (entry.hashCode == hashCode && entry.key == key) {
                return entry
            }
            entry = entry.after
        }
        return addAssociation(index, hashCode, key, supplier())
    }

    open fun removeEntry(key: String): Entry<T> {
        val hashCode = hash(key)
        val index = hashIndex(hashCode)
        var entry = store[index]
        var previous: Entry<T>? = null
        while (entry != null) {
            if (entry.hashCode == hashCode && entry.key == key) {
                return removeAssociation(entry, previous)
            }
            previous = entry
            entry = entry.after
        }
        throw NoSuchElementException()
    }

    @Suppress("UNCHECKED_CAST")
    fun removeEntry(entry: Entry<T>): T? {
        var current = store[entry.index]
        var previous: Entry<T>? = null
        while (current != null) {
            if (current === entry) {
                return removeAssociation(current, previous) as T
            }
            previous = current
            current = current.after
        }
        return null
    }

    open fun addAssociation(
        index: Int,
        hashCode: Int,
        key: String,
        value: T
    ): Entry<T> = createEntry(index, hashCode, key, value).also {
        store[index] = it
        _size += 1
    }

    open fun createEntry(
        index: Int,
        hashCode: Int,
        key: String,
        value: T
    ): Entry<T> = Entry(index, hashCode, key, value, store[index])

    private fun removeAssociation(
        entry: Entry<T>,
        previousEntry: Entry<T>?
    ): Entry<T> {
        if (previousEntry == null) {
            store[entry.index] = entry.after
        } else {
            previousEntry.after = entry.after
        }
        _size -= 1
        return entry
    }

    fun hash(key: String): Int = key.hashCode()

    fun hashIndex(hashCode: Int): Int = hashCode and hashLimit

    open class Entry<T>(
        @JvmField
        var index: Int,
        @JvmField
        var hashCode: Int,
        @JvmField
        var key: String,
        @JvmField
        var value: T?,
        @JvmField
        var after: Entry<T>?
    ) {
        fun reset(): T? = value.also { _ ->
            key = ""
            value = null
            after = null
        }
    }
}
