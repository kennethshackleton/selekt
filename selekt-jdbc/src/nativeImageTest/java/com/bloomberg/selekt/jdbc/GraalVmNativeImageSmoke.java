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

package com.bloomberg.selekt.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public final class GraalVmNativeImageSmoke {
    private GraalVmNativeImageSmoke() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:?poolSize=1");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE native_image_probe (id INTEGER, value TEXT)");
            statement.execute("INSERT INTO native_image_probe VALUES (7, 'ffm')");
            try (ResultSet result = statement.executeQuery(
                    "SELECT id AS result_id, value FROM native_image_probe")) {
                require(result.next(), "Expected one row");
                require(result.getLong(1) == 7L, "Unexpected integer value");
                require("ffm".equals(result.getString(2)), "Unexpected text value");
                require("result_id".equals(result.getMetaData().getColumnLabel(1)), "Unexpected column label");
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
