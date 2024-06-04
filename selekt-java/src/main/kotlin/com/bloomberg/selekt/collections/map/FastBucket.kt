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

import com.bloomberg.selekt.commons.forUntilDown

/**
 * @param initialCapacity a power of 2.
 */
class FastBucket<T>(
    initialCapacity: Int
) {
    @PublishedApi
    @JvmField
    internal var store = arrayOfNulls<Any>(initialCapacity)
    @PublishedApi
    @JvmField
    internal var head = -1

    fun add(element: T) {
        head += 1
        ensureCapacity()
        store[head] = element
    }

    fun isEmpty(): Boolean = -1 == head

    inline fun firstOrNull(predicate: T.() -> Boolean): T? {
        head.forUntilDown(-1) { i ->
            @Suppress("UNCHECKED_CAST")
            (store[i]!! as T).let {
                if (predicate(it)) {
                    return it
                }
            }
        }
        return null
    }

    inline fun removeFirst(predicate: T.() -> Boolean): T {
        head.forUntilDown(-1) { i ->
            @Suppress("UNCHECKED_CAST")
            (store[i]!! as T).let {
                if (predicate(it)) {
                    if (i != head) {
                        store[i] = store[head]
                    }
                    store[head] = null
                    head -= 1
                    return it
                }
            }
        }
        throw NoSuchElementException()
    }

    private fun ensureCapacity() {
        if (head == store.size) {
            store = store.copyOf(store.size shl 1)
        }
    }
}
