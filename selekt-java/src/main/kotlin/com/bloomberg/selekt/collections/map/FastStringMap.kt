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
    @JvmField
    var _size: Int = 0
    @JvmField
    val store = arrayOfNulls<FastBucket<Entry<T>>>(capacity)
    private val hashLimit = capacity - 1
    private var spare: FastBucket<Entry<T>>? = null

    fun isEmpty() = 0 == _size

    fun containsKey(key: String): Boolean {
        val hashCode = hash(key)
        val index = hashIndex(hashCode)
        return store[index]?.firstOrNull(hashCode, key) != null
    }

    inline fun getEntryElsePut(
        key: String,
        supplier: () -> T
    ): Entry<T> {
        val hashCode = hash(key)
        val index = hashIndex(hashCode)
        return bucketAt(index).let {
            it.firstOrNull(hashCode, key) ?: addAssociation(it, hashCode, key, supplier())
        }
    }

    @PublishedApi
    internal fun bucketAt(index: Int) = store[index] ?: (spare ?: FastBucket(1)).also {
        store[index] = it
        spare = null
    }

    open fun removeEntry(key: String): Entry<T> {
        val hashCode = hash(key)
        val index = hashIndex(hashCode)
        val bucket = store[index]
        return (bucket?.removeFirst {
            this.hashCode == hashCode && this.key == key
        } ?: throw NoSuchElementException()).also {
            _size -= 1
            if (bucket.isEmpty()) {
                spare = bucket
                store[index] = null
            }
        }
    }

    open fun addAssociation(
        bucket: FastBucket<Entry<T>>,
        hashCode: Int,
        key: String,
        value: T
    ): Entry<T> = createEntry(hashCode, key, value).also {
        bucket.add(it)
        _size += 1
    }

    open fun createEntry(
        hashCode: Int,
        key: String,
        value: T
    ): Entry<T> = Entry(hashCode, key, value)

    @Suppress("NOTHING_TO_INLINE")
    @PublishedApi
    internal inline fun entryMatching(
        index: Int,
        hashCode: Int,
        key: String
    ): Entry<T>? = store[index]?.firstOrNull(hashCode, key)

    fun hash(key: String): Int = key.hashCode()

    fun hashIndex(hashCode: Int): Int = hashCode and hashLimit

    @Suppress("NOTHING_TO_INLINE")
    @PublishedApi
    internal inline fun FastBucket<Entry<T>>.firstOrNull(
        hashCode: Int,
        key: String
    ) = firstOrNull {
        this.hashCode == hashCode && this.key == key
    }

    open class Entry<T>(
        @JvmField
        var hashCode: Int,
        @JvmField
        var key: String,
        @JvmField
        var value: T?
    )
}
