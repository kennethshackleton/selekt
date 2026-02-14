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

import org.junit.jupiter.api.Test
import java.sql.ParameterMetaData
import java.sql.SQLException
import java.sql.Types
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class JdbcParameterMetaDataTest {
    @Test
    fun getParameterCount() {
        val metaData = JdbcParameterMetaData(5)
        assertEquals(5, metaData.parameterCount)
    }

    @Test
    fun getParameterCountZero() {
        val metaData = JdbcParameterMetaData(0)
        assertEquals(0, metaData.parameterCount)
    }

    @Test
    fun isNullable(): Unit = JdbcParameterMetaData(3).run {
        assertEquals(ParameterMetaData.parameterNullableUnknown, isNullable(1))
        assertEquals(ParameterMetaData.parameterNullableUnknown, isNullable(2))
        assertEquals(ParameterMetaData.parameterNullableUnknown, isNullable(3))
    }

    @Test
    fun isNullableInvalidIndex(): Unit = JdbcParameterMetaData(2).run {
        assertFailsWith<SQLException> {
            isNullable(0)
        }
        assertFailsWith<SQLException> {
            isNullable(3)
        }
        assertFailsWith<SQLException> {
            isNullable(-1)
        }
    }

    @Test
    fun isSigned(): Unit = JdbcParameterMetaData(3).run {
        assertFalse(isSigned(1))
        assertFalse(isSigned(2))
        assertFalse(isSigned(3))
    }

    @Test
    fun isSignedInvalidIndex(): Unit = JdbcParameterMetaData(2).run {
        assertFailsWith<SQLException> {
            isSigned(0)
        }
        assertFailsWith<SQLException> {
            isSigned(3)
        }
    }

    @Test
    fun getPrecision(): Unit = JdbcParameterMetaData(3).run {
        assertEquals(0, getPrecision(1))
        assertEquals(0, getPrecision(2))
        assertEquals(0, getPrecision(3))
    }

    @Test
    fun getPrecisionInvalidIndex(): Unit = JdbcParameterMetaData(2).run {
        assertFailsWith<SQLException> {
            getPrecision(0)
        }
        assertFailsWith<SQLException> {
            getPrecision(3)
        }
    }

    @Test
    fun getScale(): Unit = JdbcParameterMetaData(3).run {
        assertEquals(0, getScale(1))
        assertEquals(0, getScale(2))
        assertEquals(0, getScale(3))
    }

    @Test
    fun getScaleInvalidIndex(): Unit = JdbcParameterMetaData(2).run {
        assertFailsWith<SQLException> {
            getScale(0)
        }
        assertFailsWith<SQLException> {
            getScale(3)
        }
    }

    @Test
    fun getParameterType(): Unit = JdbcParameterMetaData(3).run {
        assertEquals(Types.VARCHAR, getParameterType(1))
        assertEquals(Types.VARCHAR, getParameterType(2))
        assertEquals(Types.VARCHAR, getParameterType(3))
    }

    @Test
    fun getParameterTypeInvalidIndex(): Unit = JdbcParameterMetaData(2).run {
        assertFailsWith<SQLException> {
            getParameterType(0)
        }
        assertFailsWith<SQLException> {
            getParameterType(3)
        }
    }

    @Test
    fun getParameterTypeName(): Unit = JdbcParameterMetaData(3).run {
        assertEquals("VARCHAR", getParameterTypeName(1))
        assertEquals("VARCHAR", getParameterTypeName(2))
        assertEquals("VARCHAR", getParameterTypeName(3))
    }

    @Test
    fun getParameterTypeNameInvalidIndex(): Unit = JdbcParameterMetaData(2).run {
        assertFailsWith<SQLException> {
            getParameterTypeName(0)
        }
        assertFailsWith<SQLException> {
            getParameterTypeName(3)
        }
    }

    @Test
    fun getParameterClassName(): Unit = JdbcParameterMetaData(3).run {
        assertEquals(String::class.java.name, getParameterClassName(1))
        assertEquals(String::class.java.name, getParameterClassName(2))
        assertEquals(String::class.java.name, getParameterClassName(3))
    }

    @Test
    fun getParameterClassNameInvalidIndex(): Unit = JdbcParameterMetaData(2).run {
        assertFailsWith<SQLException> {
            getParameterClassName(0)
        }
        assertFailsWith<SQLException> {
            getParameterClassName(3)
        }
    }

    @Test
    fun getParameterMode(): Unit = JdbcParameterMetaData(3).run {
        assertEquals(ParameterMetaData.parameterModeIn, getParameterMode(1))
        assertEquals(ParameterMetaData.parameterModeIn, getParameterMode(2))
        assertEquals(ParameterMetaData.parameterModeIn, getParameterMode(3))
    }

    @Test
    fun getParameterModeInvalidIndex(): Unit = JdbcParameterMetaData(2).run {
        assertFailsWith<SQLException> {
            getParameterMode(0)
        }
        assertFailsWith<SQLException> {
            getParameterMode(3)
        }
    }

    @Test
    fun unwrap(): Unit = JdbcParameterMetaData(2).run {
        assertSame(this, unwrap(JdbcParameterMetaData::class.java))
    }

    @Test
    fun unwrapToParameterMetaData(): Unit = JdbcParameterMetaData(2).run {
        assertSame(this, unwrap(ParameterMetaData::class.java))
    }

    @Test
    fun unwrapInvalidClass(): Unit = JdbcParameterMetaData(2).run {
        assertFailsWith<SQLException> {
            unwrap(String::class.java)
        }
    }

    @Test
    fun isWrapperFor(): Unit = JdbcParameterMetaData(2).run {
        assertTrue(isWrapperFor(JdbcParameterMetaData::class.java))
        assertTrue(isWrapperFor(ParameterMetaData::class.java))
    }

    @Test
    fun isWrapperForInvalidClass() {
        assertFalse(JdbcParameterMetaData(2).isWrapperFor(String::class.java))
    }

    @Test
    fun validateParameterIndexBoundaries(): Unit = JdbcParameterMetaData(5).run {
        getParameterType(1)
        getParameterType(5)
        assertFailsWith<SQLException> {
            getParameterType(0)
        }
        assertFailsWith<SQLException> {
            getParameterType(6)
        }
    }
}
