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

package com.bloomberg.selekt.pools

import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.annotation.concurrent.GuardedBy

class SingleObjectPool<K : Any, T : IPooledObject<K>>(
    private val factory: IObjectFactory<T>,
    private val executor: ScheduledExecutorService,
    private val evictionDelayMillis: Long,
    private val evictionIntervalMillis: Long
) : IObjectPool<K, T> {
    private val mutex = Mutex()
    @GuardedBy("mutex")
    private var obj: T? = null
    @GuardedBy("mutex")
    private var canEvict = false
    @GuardedBy("mutex")
    private var future: Future<*>? = null

    private val isClosed: Boolean
        get() = mutex.isCancelled()

    override fun close() {
        if (!mutex.cancel()) {
            return
        }
        evictQuietly()
    }

    override fun borrowObject(): T {
        mutex.lock()
        return acquireObject()
    }

    override fun borrowObject(key: K) = borrowObject()

    fun borrowObjectOrNull() = if (mutex.tryLock(0L, true)) {
        acquireObject()
    } else {
        null
    }

    override fun returnObject(obj: T) {
        if (isClosed) {
            evictThenUnlock()
        } else {
            mutex.unlock()
        }
    }

    internal fun evict() = mutex.run {
        if (isClosed) {
            attemptUnparkWaiters()
            withTryLock {
                evictions()
            }
        } else {
            withTryLock(0L, false) {
                evictions()
            }
        }
    }?.let {
        factory.destroyObject(it)
    }

    private fun evictQuietly() {
        try {
            evict()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    @GuardedBy("mutex")
    private fun acquireObject(): T {
        canEvict = false
        return obj ?: runCatching { factory.makePrimaryObject() }.getOrElse {
            mutex.unlock()
            throw it
        }.also {
            obj = it
            attemptScheduleEviction()
        }
    }

    @GuardedBy("mutex")
    private fun attemptScheduleEviction() {
        if (future?.isCancelled == false || evictionIntervalMillis < 0L || isClosed) {
            return
        }
        canEvict = true
        future = executor.scheduleAtFixedRate(
            ::evict,
            evictionDelayMillis,
            evictionIntervalMillis,
            TimeUnit.MILLISECONDS
        )
    }

    @GuardedBy("mutex")
    private fun cancelScheduledEviction() {
        future?.let {
            future = null
            it.cancel(false)
        }
    }

    @GuardedBy("mutex")
    private fun evictions(): T? {
        if (isClosed) {
            factory.close()
        }
        return (if (canEvict && future?.isCancelled == false || isClosed) obj else null)?.also {
            obj = null
            cancelScheduledEviction()
        }.also {
            canEvict = true
        }
    }

    @GuardedBy("mutex")
    private fun evictThenUnlock() {
        try {
            evictions()
        } finally {
            mutex.unlock()
        }?.let {
            factory.destroyObject(it)
        }
    }
}
