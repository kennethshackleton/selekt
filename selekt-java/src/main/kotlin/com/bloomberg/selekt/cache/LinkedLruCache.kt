/*
 * Copyright 2020 Bloomberg Finance L.P.
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

package com.bloomberg.selekt.cache

import com.bloomberg.selekt.collections.map.FastLinkedStringMap
import javax.annotation.concurrent.NotThreadSafe

@NotThreadSafe
class LinkedLruCache<T>(
    @PublishedApi
    @JvmField
    internal val maxSize: Int,
    @PublishedApi
    @JvmField
    internal val store: FastLinkedStringMap<T & Any>
) {
    constructor(
        maxSize: Int,
        disposal: (T & Any) -> Unit
    ) : this(maxSize, FastLinkedStringMap<T & Any>(
        maxSize = maxSize,
        disposal = disposal,
        accessOrder = true
    ))

    fun evict(key: String) {
        store.removeKey(key)
    }

    fun evictAll() {
        store.clear()
    }

    inline fun get(key: String, supplier: () -> T & Any): T & Any = store.getElsePut(key, supplier)

    fun containsKey(key: String) = store.containsKey(key)
}
