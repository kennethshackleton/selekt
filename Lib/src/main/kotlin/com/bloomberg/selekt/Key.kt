/*
 * Copyright 2021 Bloomberg Finance L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bloomberg.selekt

import com.bloomberg.selekt.annotations.Generated
import com.bloomberg.selekt.commons.zero

private const val KEY_SIZE = 32

internal class Key(value: ByteArray) {
    init {
        require(KEY_SIZE == value.size) { "Key must be 32 bytes in size." }
    }

    private val lock = Any()
    private val value: ByteArray = value.copyOf()

    fun zero() = synchronized(lock) {
        value.fill(0)
    }

    @Generated
    inline fun <R> use(action: (ByteArray) -> R) = synchronized(lock) {
        value.copyOf()
    }.let {
        try {
            action(it)
        } finally {
            it.zero()
        }
    }
}
