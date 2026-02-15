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

package com.bloomberg.selekt.jdbc.exposed

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.assertEquals

private object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val name = text("name")
    val email = text("email")

    override val primaryKey = PrimaryKey(id)
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExposedIntegrationTest {
    private lateinit var database: Database
    private lateinit var tempDir: Path
    private lateinit var dbFile: Path

    @BeforeAll
    fun setUpAll() {
        tempDir = Files.createTempDirectory("selekt-test-")
        dbFile = tempDir.resolve("test.db")
        database = Database.connect({ DriverManager.getConnection("jdbc:sqlite:$dbFile") })
        transaction(database) {
            SchemaUtils.create(Users)
        }
    }

    @AfterEach
    fun tearDown() {
        transaction(database) {
            Users.deleteAll()
        }
    }

    @AfterAll
    fun tearDownAll() {
        transaction(database) {
            SchemaUtils.drop(Users)
        }
        tempDir.toFile().delete()
    }

    @Test
    fun basicInsertAndSelect() {
        transaction(database) {
            Users.insert {
                it[name] = "Alice"
                it[email] = "alice@example.com"
            }
            Users.insert {
                it[name] = "Bob"
                it[email] = "bob@example.com"
            }
            Users.selectAll().toList().let {
                assertEquals(2, it.size)
                val alice = it.first { it[Users.name] == "Alice" }
                assertEquals("alice@example.com", alice[Users.email])
            }
        }
    }

    @Test
    fun updateRecords() {
        transaction(database) {
            val id = Users.insert {
                it[name] = "Charlie"
                it[email] = "charlie@example.com"
            } get Users.id
            Users.update({ Users.id eq id }) {
                it[email] = "charlie.updated@example.com"
            }
            Users.selectAll().where { Users.id eq id }.single().let {
                assertEquals("charlie.updated@example.com", it[Users.email])
            }
        }
    }

    @Test
    fun deleteRecords() {
        transaction(database) {
            Users.insert {
                it[name] = "David"
                it[email] = "david@example.com"
            }
            Users.insert {
                it[name] = "Eve"
                it[email] = "eve@example.com"
            }
            assertEquals(2, Users.selectAll().count())
            Users.deleteWhere { name eq "David" }
            Users.selectAll().toList().let {
                assertEquals(1, it.size)
                assertEquals("Eve", it.first()[Users.name])
            }
        }
    }

    @Test
    fun batchInsert() {
        transaction(database) {
            listOf("Jack", "Kate", "Liam", "Mia", "Noah").forEach { userName ->
                Users.insert {
                    it[name] = userName
                    it[email] = "$userName@example.com".lowercase()
                }
            }
            assertEquals(5, Users.selectAll().count())
        }
    }

    @Test
    fun transactionRollback() {
        transaction(database) {
            Users.insert {
                it[name] = "Oscar"
                it[email] = "oscar@example.com"
            }
            assertEquals(1, Users.selectAll().count())
            rollback()
        }
        transaction(database) {
            assertEquals(0, Users.selectAll().count())
        }
    }

    @Test
    fun idGeneration() {
        transaction(database) {
            val idOne = Users.insert {
                it[name] = "Peter"
                it[email] = "peter@example.com"
            } get Users.id
            val idTwo = Users.insert {
                it[name] = "Quinn"
                it[email] = "quinn@example.com"
            } get Users.id
            assertEquals(true, idTwo > idOne)
        }
    }
}
