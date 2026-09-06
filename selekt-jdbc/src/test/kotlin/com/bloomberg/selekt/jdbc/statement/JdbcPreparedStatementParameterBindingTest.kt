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

package com.bloomberg.selekt.jdbc.statement

import java.io.File
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class JdbcPreparedStatementParameterBindingTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `question marks in literals and comments are not parameters`() {
        connection().use { connection ->
            connection.prepareStatement("SELECT '?' AS literal, ? AS bound /* ? */ -- ?\n").use { statement ->
                assertEquals(1, statement.parameterMetaData.parameterCount)
                statement.setString(1, "value")
                assertFailsWith<SQLException> { statement.setString(2, "out of range") }
                statement.executeQuery().use { result ->
                    assertTrue(result.next())
                    assertEquals("?", result.getString("literal"))
                    assertEquals("value", result.getString("bound"))
                }
            }
        }
    }

    @Test
    fun `numbered and repeated named parameters use SQLite slot indexes`() {
        connection().use { connection ->
            connection.prepareStatement(
                "SELECT ?2 AS numbered, :name AS named, :name AS repeated"
            ).use { statement ->
                assertEquals(3, statement.parameterMetaData.parameterCount)
                statement.setInt(2, 42)
                statement.setString(3, "value")
                statement.executeQuery().use { result ->
                    assertTrue(result.next())
                    assertEquals(42, result.getInt("numbered"))
                    assertEquals("value", result.getString("named"))
                    assertEquals("value", result.getString("repeated"))
                }
            }
        }
    }

    private fun connection() = DriverManager.getConnection(
        "jdbc:sqlite:${File(tempDir, "parameters.db").absolutePath}"
    )
}
