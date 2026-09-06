/*
 * Copyright 2026 Bloomberg Finance L.P.
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

package com.bloomberg.selekt

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.concurrent.NotThreadSafe

/**
 * An explicitly owned database session. Operations and transaction state remain bound to this session when it is
 * handed between threads sequentially; concurrent access is not supported.
 *
 * Closing a session rolls back any active transaction before releasing its database reference.
 *
 * @since 1.1.1
 */
@NotThreadSafe
class SQLDatabaseSession internal constructor(
    private val database: SQLDatabase,
    internal val session: SQLSession,
    private val beforeSessionAccess: () -> Unit
) : Closeable {
    private val closed = AtomicBoolean(false)

    @get:JvmSynthetic
    val isActiveOnCurrentThread: Boolean
        get() = database.isSessionActive(session)

    /**
     * Runs [block] with this session selected for the duration of the call.
     */
    @JvmSynthetic
    fun <T> execute(block: SQLDatabase.() -> T): T {
        check(!closed.get()) { "Database session is closed." }
        beforeSessionAccess()
        return database.withSession(session, block)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                beforeSessionAccess()
                database.withSession(session) {
                    if (session.inTransaction) {
                        session.endTransaction()
                    }
                }
            } finally {
                database.release()
            }
        }
    }
}
