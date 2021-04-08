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

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.same
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThatExceptionOfType

internal class SQLBlobOutputStreamTest {
    @Test
    fun close() {
        mock<SQLBlob>().let {
            BlobOutputStream(it).close()
            verify(it, never()).close()
        }
    }

    @Test
    fun outputStreamChecksOffset() {
        BlobOutputStream(mock()).run {
            assertThatExceptionOfType(ArrayIndexOutOfBoundsException::class.java).isThrownBy {
                write(ByteArray(1), -1, 1)
            }
        }
    }

    @Test
    fun outputStreamChecksLength() {
        BlobOutputStream(mock()).run {
            assertThatExceptionOfType(ArrayIndexOutOfBoundsException::class.java).isThrownBy {
                write(ByteArray(1), 0, -1)
            }
        }
    }

    @Test
    fun outputStreamChecksUpperBounds() {
        BlobOutputStream(mock()).run {
            assertThatExceptionOfType(ArrayIndexOutOfBoundsException::class.java).isThrownBy {
                write(ByteArray(1), 1, 1)
            }
        }
    }

    @Test
    fun writeToReadOnlyBlob() {
        mock<SQLBlob>().apply {
            whenever(readOnly) doReturn true
        }.let {
            assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
                BlobOutputStream(it)
            }
        }
    }

    @Test
    fun writeFromNegativeStart() {
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            BlobOutputStream(mock(), start = -1)
        }
    }

    @Test
    fun writeTooAdvancedStart() {
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            BlobOutputStream(mock(), start = 1)
        }
    }

    @Test
    fun writeEmptySource() {
        val blob = mock<SQLBlob>().apply {
            whenever(size) doReturn 1
        }
        BlobOutputStream(blob).use {
            it.write(byteArrayOf())
            verify(blob, never()).write(any(), any(), any(), any())
        }
    }

    @Test
    fun writeToEmptyBlob() {
        val blob = mock<SQLBlob>().apply {
            whenever(size) doReturn 0
        }
        BlobOutputStream(blob).use {
            assertThatExceptionOfType(IndexOutOfBoundsException::class.java).isThrownBy {
                it.write(byteArrayOf(0x42))
            }
        }
    }

    @Test
    fun writeSingleByte() {
        val expectedSize = 1
        val expectedByte = 0x42.toByte()
        val blob = mock<SQLBlob>().apply {
            whenever(size) doReturn expectedSize
            whenever(read(any(), any(), any(), any())).doAnswer {
                requireNotNull(it.arguments[1] as? ByteArray)[0] = expectedByte
                Unit
            }
        }
        BlobOutputStream(blob).use {
            it.write(expectedByte.toInt())
            verify(blob, times(1)).write(eq(0), argThat { first() == expectedByte }, eq(0), eq(1))
        }
    }

    @Test
    fun writeFromSource() {
        val expectedSource = ByteArray(40)
        val blob = mock<SQLBlob>().apply {
            whenever(size) doReturn 100
        }
        BlobOutputStream(blob).use {
            it.write(expectedSource, 0, expectedSource.size)
            verify(blob, times(1)).write(eq(0), same(expectedSource), eq(0), eq(expectedSource.size))
            it.write(expectedSource, 0, expectedSource.size)
            verify(blob, times(1)).write(eq(expectedSource.size), same(expectedSource), eq(0), eq(expectedSource.size))
            it.write(expectedSource, 0, 20)
            verify(blob, times(1)).write(eq(2 * expectedSource.size), same(expectedSource), eq(0), eq(20))
            assertThatExceptionOfType(IndexOutOfBoundsException::class.java).isThrownBy {
                it.write(byteArrayOf(0x42))
            }
        }
    }

    @Test
    fun writeFromSourceWithStart() {
        val expectedSource = ByteArray(40)
        val blob = mock<SQLBlob>().apply {
            whenever(size) doReturn 100
        }
        BlobOutputStream(blob, 40).use {
            it.write(expectedSource, 0, expectedSource.size)
            verify(blob, times(1)).write(eq(expectedSource.size), same(expectedSource), eq(0), eq(expectedSource.size))
            it.write(expectedSource, 0, 20)
            verify(blob, times(1)).write(eq(2 * expectedSource.size), same(expectedSource), eq(0), eq(20))
            assertThatExceptionOfType(IndexOutOfBoundsException::class.java).isThrownBy {
                it.write(byteArrayOf(0x42))
            }
        }
    }
}