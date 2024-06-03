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
) : FastStringMap<T>(capacity) {
    private var head: LinkedEntry<T>? = null
    private var tail: LinkedEntry<T>? = null

    fun getElsePut(
        key: String,
        supplier: () -> T
    ): T = super.getEntryElsePut(key) {
        if (super.size >= maxSize) {
            removeLast()
        }
        supplier()
    }.value!!

    fun removeKey(key: String) {
        disposal((super.removeEntry(key) as LinkedEntry<T>).unlink().value!!)
    }

    private fun LinkedEntry<T>.unlink(): Entry<T> = apply {
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
    internal fun putFirst(node: LinkedEntry<T>): Unit = node.run {
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

    override fun addAssociation(
        index: Int,
        hashCode: Int,
        key: String,
        value: T
    ): Entry<T> = (super.addAssociation(index, hashCode, key, value) as LinkedEntry<T>).also {
        putFirst(it)
    }

    override fun createEntry(
        index: Int,
        hashCode: Int,
        key: String,
        value: T
    ): Entry<T> = LinkedEntry(index, hashCode, key, value, store[index])

    @PublishedApi
    @JvmSynthetic
    internal fun removeLast(): LinkedEntry<T> = tail!!.apply {
        previous?.let { it.next = null } ?: run { head = null }
        tail = previous
        previous = null
        super.removeEntry(key)
        disposal(value!!)
    }

    @PublishedApi
    internal class LinkedEntry<T>(
        index: Int,
        hashCode: Int,
        key: String,
        value: T,
        after: Entry<T>?
    ) : Entry<T>(index, hashCode, key, value, after) {
        @JvmField
        var previous: LinkedEntry<T>? = null
        @JvmField
        var next: LinkedEntry<T>? = null
    }
}
