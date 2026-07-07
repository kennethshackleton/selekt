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

package com.bloomberg.selekt.detekt

import io.github.detekt.test.utils.KotlinCoreEnvironmentWrapper
import io.github.detekt.test.utils.createEnvironment
import io.gitlab.arturbosch.detekt.test.lint
import io.gitlab.arturbosch.detekt.test.lintWithContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [RequiresTrustedInputRule]. Each snippet declares its own
 * annotation and sink so the test module does not need to depend on the real
 * `selekt-api` module on the classpath.
 */
internal class RequiresTrustedInputRuleTest {

    private lateinit var env: KotlinCoreEnvironmentWrapper

    @BeforeEach
    fun setUp() {
        env = createEnvironment()
    }

    @AfterEach
    fun tearDown() {
        env.dispose()
    }

    @Test
    fun `string literal argument to annotated parameter is accepted`() {
        val findings = RequiresTrustedInputRule().lintWithContext(
            env.env,
            wrap("fun caller() { sink(\"users\") }")
        )
        assertTrue(findings.isEmpty(), "expected no findings, got: $findings")
    }

    @Test
    fun `dynamic string argument to annotated parameter is flagged`() {
        val findings = RequiresTrustedInputRule().lintWithContext(
            env.env,
            wrap("fun caller(userSupplied: String) { sink(userSupplied) }")
        )
        assertEquals(1, findings.size, "expected one finding, got: $findings")
        assertEquals("RequiresTrustedInput", findings.single().id)
    }

    @Test
    fun `const val argument is accepted`() {
        val findings = RequiresTrustedInputRule().lintWithContext(
            env.env,
            wrap(
                """
                const val TABLE = "users"
                fun caller() { sink(TABLE) }
                """.trimIndent()
            )
        )
        assertTrue(findings.isEmpty(), "expected no findings, got: $findings")
    }

    @Test
    fun `annotation on caller parameter propagates trust`() {
        val findings = RequiresTrustedInputRule().lintWithContext(
            env.env,
            wrap("fun wrapper(@RequiresTrustedInput table: String) { sink(table) }")
        )
        assertTrue(findings.isEmpty(), "expected no findings, got: $findings")
    }

    @Test
    fun `interpolated string is flagged`() {
        val findings = RequiresTrustedInputRule().lintWithContext(
            env.env,
            wrap("fun caller(prefix: String) { sink(\"t_\${prefix}\") }")
        )
        assertEquals(1, findings.size, "expected one finding, got: $findings")
    }

    @Test
    fun `arrayOf of literals is accepted`() {
        val findings = RequiresTrustedInputRule().lintWithContext(
            env.env,
            wrap(
                """
                fun columnsSink(@RequiresTrustedInput cols: Array<String>) {}
                fun caller() { columnsSink(arrayOf("id", "name")) }
                """.trimIndent()
            )
        )
        assertTrue(findings.isEmpty(), "expected no findings, got: $findings")
    }

    @Test
    fun `arrayOf with dynamic element is flagged`() {
        val findings = RequiresTrustedInputRule().lintWithContext(
            env.env,
            wrap(
                """
                fun columnsSink(@RequiresTrustedInput cols: Array<String>) {}
                fun caller(x: String) { columnsSink(arrayOf("id", x)) }
                """.trimIndent()
            )
        )
        assertEquals(1, findings.size, "expected one finding, got: $findings")
    }

    @Test
    fun `rule is a no-op without type resolution and does not crash`() {
        val findings = RequiresTrustedInputRule().lint(
            wrap("fun caller(x: String) { sink(x) }")
        )
        assertTrue(findings.isEmpty(), "expected no findings without type resolution, got: $findings")
    }

    @Test
    fun `unannotated call is untouched`() {
        val findings = RequiresTrustedInputRule().lintWithContext(
            env.env,
            """
            package example
            fun plain(x: String) {}
            fun caller(x: String) { plain(x) }
            """.trimIndent()
        )
        assertTrue(findings.isEmpty(), "expected no findings, got: $findings")
    }

    /**
     * Every snippet is prefixed with the annotation declaration and a sink
     * whose `table` parameter is annotated. Everything lives in one package so
     * a single-file Kotlin snippet is valid.
     */
    private fun wrap(body: String): String = """
        package com.bloomberg.selekt.annotations

        @Retention(AnnotationRetention.BINARY)
        @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
        annotation class RequiresTrustedInput

        fun sink(@RequiresTrustedInput table: String) {}

        $body
    """.trimIndent()
}
