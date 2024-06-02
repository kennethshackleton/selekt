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
class FastStringMap<T>(capacity: Int) {
    val size: Int
        inline get() = _size
    var _size: Int = 0
    val store = arrayOfNulls<Entry<Any?>>(capacity)
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
            entry = entry.next
        }
        return false
    }

    inline operator fun get(
        key: String,
        supplier: () -> T
    ): T {
        val hashCode = hash(key)
        val index = hashIndex(hashCode)
        var entry = store[index]
        while (entry != null) {
            if (entry.hashCode == hashCode && entry.key == key) {
                @Suppress("UNCHECKED_CAST")
                return entry.value as T
            }
            entry = entry.next
        }
        return addAssociation(index, hashCode, key, supplier())
    }

    fun remove(key: String): T? {
        val hashCode = hash(key)
        val index = hashIndex(hashCode)
        var entry = store[index]
        var previous: Entry<Any?>? = null
        while (entry != null) {
            if (entry.hashCode == hashCode && entry.key == key) {
                @Suppress("UNCHECKED_CAST")
                return removeAssociation(entry, previous) as T
            }
            previous = entry
            entry = entry.next
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun removeEntry(entry: Entry<T>): T? {
        var current = store[entry.index]
        var previous: Entry<Any?>? = null
        while (current != null) {
            if (current === entry) {
                return removeAssociation(current, previous) as T
            }
            previous = current
            current = current.next
        }
        return null
    }

    fun addAssociation(
        index: Int,
        hashCode: Int,
        key: String,
        value: T
    ): T {
        store[index] = Entry(index, hashCode, key, value, store[index])
        _size += 1
        return value
    }

    private fun removeAssociation(
        entry: Entry<Any?>,
        previousEntry: Entry<Any?>?
    ): Any? {
        if (previousEntry == null) {
            store[entry.index] = entry.next
        } else {
            previousEntry.next = entry.next
        }
        _size -= 1
        return entry.reset()
    }

    fun hash(key: String): Int = key.hashCode()

    fun hashIndex(hashCode: Int): Int = hashCode and hashLimit

    class Entry<T>(
        @JvmField
        var index: Int,
        @JvmField
        var hashCode: Int,
        @JvmField
        var key: String,
        @JvmField
        var value: T?,
        @JvmField
        var next: Entry<T?>?
    ) {
        fun reset(): T? = value.also { _ ->
            key = ""
            value = null
            next = null
        }
    }
}
