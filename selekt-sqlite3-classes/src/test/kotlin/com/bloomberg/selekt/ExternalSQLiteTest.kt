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

package com.bloomberg.selekt

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ExternalSQLiteTest {
    companion object {
        private val sqlite = externalSQLiteSingleton()
    }

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `externalSQLiteSingleton creates instance`() {
        assertNotNull(sqlite)
    }

    @Test
    fun `libVersion returns non-empty string`() {
        val version = sqlite.libVersion()
        assertNotNull(version)
        assertTrue(version.isNotEmpty())
    }

    @Test
    fun `libVersionNumber returns positive integer`() {
        assertTrue(sqlite.libVersionNumber() > 0)
    }

    @Test
    fun `threadsafe returns non-zero value`() {
        assertTrue(sqlite.threadsafe() > 0)
    }

    @Test
    fun `can open and close database`() {
        val dbPath = File(tempDir, "test.db").absolutePath
        val dbHolder = LongArray(1)
        val openResult = sqlite.openV2(dbPath, 0x00000006, dbHolder)
        assertEquals(SQL_OK, openResult)
        assertTrue(dbHolder[0] > 0L)
        assertEquals(SQL_OK, sqlite.closeV2(dbHolder[0]))
    }

    @Test
    fun `can prepare and finalize statement`() {
        val dbPath = File(tempDir, "test.db").absolutePath
        val dbHolder = LongArray(1)
        sqlite.openV2(dbPath, 0x00000006, dbHolder)
        val db = dbHolder[0]
        try {
            val stmtHolder = LongArray(1)
            val sql = "SELECT 1"
            assertEquals(SQL_OK, sqlite.prepareV2(db, sql, sql.length, stmtHolder))
            assertTrue(stmtHolder[0] > 0L)
            assertEquals(SQL_OK, sqlite.finalize(stmtHolder[0]))
        } finally {
            sqlite.closeV2(db)
        }
    }

    @Test
    fun `can execute simple query`() {
        val dbPath = File(tempDir, "test.db").absolutePath
        val dbHolder = LongArray(1)
        sqlite.openV2(dbPath, 0x00000006, dbHolder)
        val db = dbHolder[0]
        try {
            val stmtHolder = LongArray(1)
            val sql = "SELECT 42"
            sqlite.prepareV2(db, sql, sql.length, stmtHolder)
            val statement = stmtHolder[0]
            try {
                assertEquals(SQL_ROW, sqlite.step(statement))
                assertEquals(42, sqlite.columnInt(statement, 0))
            } finally {
                sqlite.finalize(statement)
            }
        } finally {
            sqlite.closeV2(db)
        }
    }

    @Test
    fun `can bind and retrieve text`() {
        val dbPath = File(tempDir, "test.db").absolutePath
        val dbHolder = LongArray(1)
        sqlite.openV2(dbPath, 0x00000006, dbHolder)
        val db = dbHolder[0]
        try {
            val stmtHolder = LongArray(1)
            val sql = "SELECT ?"
            sqlite.prepareV2(db, sql, sql.length, stmtHolder)
            val statement = stmtHolder[0]
            try {
                val testText = "Hello, SQLite!"
                val bindResult = sqlite.bindText(statement, 1, testText)
                assertEquals(SQL_OK, bindResult)
                assertEquals(SQL_ROW, sqlite.step(statement))
                assertEquals(testText, sqlite.columnText(statement, 0))
            } finally {
                sqlite.finalize(statement)
            }
        } finally {
            sqlite.closeV2(db)
        }
    }
}
