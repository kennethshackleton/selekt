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

package com.bloomberg.selekt

import org.junit.jupiter.api.Test
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.stubbing.Answer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val POINTER = 42L
private const val DB = 43L
private const val INTERVAL_MILLIS = 2_000L

internal class SQLPreparedStatementTest {
    @Test
    fun clearBindings(): Unit = mock<SQLite>().run {
        SQLPreparedStatement(POINTER, "SELECT * FROM Foo", this, CommonThreadLocalRandom).clearBindings()
        verify(this, times(1)).clearBindings(eq(POINTER))
    }

    @Test
    fun stepWithRetryDone() {
        val sqlite = mock<SQLite> {
            whenever(it.stepWithoutThrowing(any())) doReturn SQL_DONE
        }
        val statement = SQLPreparedStatement(POINTER, "BEGIN IMMEDIATE TRANSACTION", sqlite, CommonThreadLocalRandom)
        assertEquals(SQL_DONE, statement.step(INTERVAL_MILLIS))
    }

    @Test
    fun stepWithRetryRow() {
        val sqlite = mock<SQLite> {
            whenever(it.stepWithoutThrowing(any())) doReturn SQL_ROW
        }
        val statement = SQLPreparedStatement(POINTER, "SELECT * FROM Foo", sqlite, CommonThreadLocalRandom)
        assertEquals(SQL_ROW, statement.step(INTERVAL_MILLIS))
    }

    @Test
    fun stepWithRetryExpires() {
        val sqlite = mock<SQLite> {
            whenever(it.databaseHandle(any())) doReturn DB
            whenever(it.step(any())) doReturn SQL_BUSY
        }
        val statement = SQLPreparedStatement(POINTER, "BEGIN BLAH", sqlite, CommonThreadLocalRandom)
        assertFailsWith<Exception> {
            statement.step(0L)
        }
    }

    @Test
    fun stepWithRetryCanUltimatelySucceed() {
        val sqlite = mock<SQLite> {
            whenever(it.stepWithoutThrowing(any())) doAnswer object : Answer<SQLCode> {
                private var count = 0

                override fun answer(invocation: InvocationOnMock) = when (count++) {
                    0 -> SQL_BUSY
                    else -> SQL_DONE
                }
            }
        }
        val statement = SQLPreparedStatement(POINTER, "BEGIN IMMEDIATE TRANSACTION", sqlite, CommonThreadLocalRandom)
        assertEquals(SQL_DONE, statement.step(500L))
    }

    @Test
    fun stepRetryDoesNotStackOverflow() {
        val sqlite = mock<SQLite> {
            whenever(it.databaseHandle(any())) doReturn DB
            whenever(it.stepWithoutThrowing(any())) doReturn SQL_BUSY
        }
        val statement = SQLPreparedStatement(POINTER, "BEGIN BLAH", sqlite, CommonThreadLocalRandom)
        assertFailsWith<Exception> {
            statement.step(2_000L)
        }
    }

    @Test
    fun stepRejectsNegativeInterval() {
        val statement = SQLPreparedStatement(POINTER, "BEGIN BLAH", mock(), CommonThreadLocalRandom)
        assertFailsWith<IllegalArgumentException> {
            statement.step(-1L)
        }
    }

    @Test
    fun isBusyTrue() {
        val sqlite = mock<SQLite> {
            whenever(it.databaseHandle(any())) doReturn DB
            whenever(it.statementBusy(any())) doReturn 1
        }
        assertTrue(SQLPreparedStatement(POINTER, "BEGIN BLAH", sqlite, CommonThreadLocalRandom).isBusy())
    }

    @Test
    fun isBusyFalse() {
        val sqlite = mock<SQLite> {
            whenever(it.databaseHandle(any())) doReturn DB
            whenever(it.statementBusy(any())) doReturn 0
        }
        assertFalse(SQLPreparedStatement(POINTER, "BEGIN BLAH", sqlite, CommonThreadLocalRandom).isBusy())
    }

    @Test
    fun columnName() {
        val sqlite = mock<SQLite> {
            whenever(it.databaseHandle(any())) doReturn DB
            whenever(it.columnName(any(), any())) doReturn "foo"
        }
        assertEquals("foo", SQLPreparedStatement(POINTER, "BEGIN BLAH", sqlite, CommonThreadLocalRandom).columnName(0))
        verify(sqlite, times(1)).columnName(eq(POINTER), eq(0))
    }

    @Test
    fun bindBlobByName() {
        val blob = byteArrayOf(1, 2, 3)
        val sqlite = mock<SQLite> {
            whenever(it.bindParameterIndex(any(), eq(":data"))) doReturn 1
        }
        SQLPreparedStatement(POINTER, "INSERT INTO t VALUES (:data)", sqlite, CommonThreadLocalRandom)
            .bind(":data", blob)
        verify(sqlite, times(1)).bindParameterIndex(eq(POINTER), eq(":data"))
        verify(sqlite, times(1)).bindBlob(eq(POINTER), eq(1), eq(blob))
    }

    @Test
    fun bindDoubleByName() {
        val sqlite = mock<SQLite> {
            whenever(it.bindParameterIndex(any(), eq(":value"))) doReturn 1
        }
        SQLPreparedStatement(POINTER, "INSERT INTO t VALUES (:value)", sqlite, CommonThreadLocalRandom)
            .bind(":value", 3.14)
        verify(sqlite, times(1)).bindParameterIndex(eq(POINTER), eq(":value"))
        verify(sqlite, times(1)).bindDouble(eq(POINTER), eq(1), eq(3.14))
    }

    @Test
    fun bindIntByName() {
        val sqlite = mock<SQLite> {
            whenever(it.bindParameterIndex(any(), eq("@count"))) doReturn 2
        }
        SQLPreparedStatement(POINTER, "INSERT INTO t VALUES (?, @count)", sqlite, CommonThreadLocalRandom)
            .bind("@count", 42)
        verify(sqlite, times(1)).bindParameterIndex(eq(POINTER), eq("@count"))
        verify(sqlite, times(1)).bindInt(eq(POINTER), eq(2), eq(42))
    }

    @Test
    fun bindLongByName() {
        val sqlite = mock<SQLite> {
            whenever(it.bindParameterIndex(any(), eq($$"$id"))) doReturn 1
        }
        SQLPreparedStatement(POINTER, $$"SELECT * FROM t WHERE id = $id", sqlite, CommonThreadLocalRandom)
            .bind($$"$id", 123_456_789L)
        verify(sqlite, times(1)).bindParameterIndex(eq(POINTER), eq($$"$id"))
        verify(sqlite, times(1)).bindInt64(eq(POINTER), eq(1), eq(123_456_789L))
    }

    @Test
    fun bindStringByName() {
        val sqlite = mock<SQLite> {
            whenever(it.bindParameterIndex(any(), eq(":name"))) doReturn 1
        }
        SQLPreparedStatement(POINTER, "INSERT INTO t VALUES (:name)", sqlite, CommonThreadLocalRandom)
            .bind(":name", "test")
        verify(sqlite, times(1)).bindParameterIndex(eq(POINTER), eq(":name"))
        verify(sqlite, times(1)).bindText(eq(POINTER), eq(1), eq("test"))
    }

    @Test
    fun bindNullByName() {
        val sqlite = mock<SQLite> {
            whenever(it.bindParameterIndex(any(), eq(":nullable"))) doReturn 1
        }
        SQLPreparedStatement(POINTER, "INSERT INTO t VALUES (:nullable)", sqlite, CommonThreadLocalRandom)
            .bindNull(":nullable")
        verify(sqlite, times(1)).bindParameterIndex(eq(POINTER), eq(":nullable"))
        verify(sqlite, times(1)).bindNull(eq(POINTER), eq(1))
    }

    @Test
    fun bindByNameThrowsForUnknownParameter() {
        val sqlite = mock<SQLite> {
            whenever(it.bindParameterIndex(any(), eq(":unknown"))) doReturn 0
        }
        val statement = SQLPreparedStatement(
            POINTER,
            "INSERT INTO t VALUES (:known)",
            sqlite,
            CommonThreadLocalRandom
        )
        assertFailsWith<IllegalArgumentException> {
            statement.bind(":unknown", "value")
        }
    }
}
