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

class FastLinkedStringMap<T>(
    private val maxSize: Int,
    capacity: Int,
    private val disposal: (T) -> Unit
) {
    private val store = FastStringMap<Node<T>>(capacity)
    private var head: Node<T>? = null
    private var tail: Node<T>? = null

    fun containsKey(key: String): Boolean = store.containsKey(key)

    operator fun get(
        key: String,
        supplier: () -> T
    ): T = store[key, {
        if (store.size >= maxSize) {
            // Recycle the tail node.
            removeLast().apply {
                this.key = key
                value = supplier()
            }
        } else {
            Node(key, supplier())
        }
    }].also {
        putFirst(it)
    }.value

    fun remove(key: String) {
        store.remove(key)?.let {
            disposal(it.unlink().value)
        }
    }

    private fun Node<T>.unlink() = apply {
        previous?.let { it.next = next }
        next?.let { it.previous = previous }
        if (this === head) {
            head = next
        }
        if (this === tail) {
            tail = previous
        }
        previous = null
        next = null
    }

    @PublishedApi
    @JvmSynthetic
    internal fun putFirst(node: Node<T>): Unit = node.run {
        if (this === head) {
            return@run
        }
        previous?.let { it.next = next }
        next?.let { it.previous = previous }
        if (this === tail) {
            tail = previous
        }
        next = head
        previous = null
        head?.let { it.previous = this }
        head = this
        if (tail == null) {
            tail = this
        }
    }

    @PublishedApi
    @JvmSynthetic
    internal fun removeLast(): Node<T> = tail!!.apply {
        previous?.let { it.next = null } ?: run { head = null }
        tail = previous
        previous = null
        store.remove(key)
        disposal(value)
    }

    class Node<T>(
        @JvmField
        var key: String,
        @JvmField
        var value: T
    ) {
        @JvmField
        var previous: Node<T>? = null
        @JvmField
        var next: Node<T>? = null
    }
}
