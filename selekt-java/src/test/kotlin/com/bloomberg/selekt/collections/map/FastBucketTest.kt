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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class FastBucketTest {
    @Test
    fun isEmptyInitially() {
        assertTrue(FastBucket<Any>(1).isEmpty())
    }

    @Test
    fun isNotEmpty() {
        assertFalse(FastBucket<Any>(1).apply {
            add(Any())
        }.isEmpty())
    }

    @Test
    fun addRemove() {
        val first = Any()
        val bucket = FastBucket<Any>(1).apply { add(first) }
        assertSame(first, bucket.removeFirst { this === first })
    }

    @Test
    fun removeWhenEmptyThrows() {
        assertThrows<NoSuchElementException> {
            FastBucket<Any>(1).removeFirst { true }
        }
    }

    @Test
    fun expands() {
        val first = Any()
        val second = Any()
        val bucket = FastBucket<Any>(1).apply {
            add(first)
            add(second)
        }
        assertSame(second, bucket.removeFirst { true })
        assertSame(first, bucket.removeFirst { true })
        assertTrue(bucket.isEmpty())
    }
}
