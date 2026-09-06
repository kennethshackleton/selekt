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

package com.bloomberg.selekt;

import java.lang.foreign.MemorySegment;
import kotlin.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SQLiteAllocationTest {
    @Test
    void releasesSQLiteAllocationAfterSuccessfulConversion() {
        var allocation = MemorySegment.ofAddress(42L);
        var released = new MemorySegment[1];

        var result = ExternalSQLiteKt.useSQLiteAllocation(
            allocation,
            segment -> {
                released[0] = segment;
                return Unit.INSTANCE;
            },
            segment -> "expanded SQL"
        );

        assertEquals("expanded SQL", result);
        assertSame(allocation, released[0]);
    }

    @Test
    void releasesSQLiteAllocationWhenConversionFails() {
        var allocation = MemorySegment.ofAddress(42L);
        var released = new MemorySegment[1];
        assertThrows(IllegalStateException.class, () -> ExternalSQLiteKt.useSQLiteAllocation(
            allocation,
            segment -> {
                released[0] = segment;
                return Unit.INSTANCE;
            },
            _ -> {
                throw new IllegalStateException("conversion failed");
            }
        ));
        assertSame(allocation, released[0]);
    }
}
