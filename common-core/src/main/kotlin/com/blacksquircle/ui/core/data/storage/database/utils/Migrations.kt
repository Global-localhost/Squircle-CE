/*
 * Copyright 2023 Squircle CE contributors.
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

package com.blacksquircle.ui.core.data.storage.database.utils

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.blacksquircle.ui.core.data.factory.LanguageFactory

object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `${Tables.SERVERS}` (
                    `uuid` TEXT NOT NULL, 
                    `scheme` TEXT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `address` TEXT NOT NULL, 
                    `port` INTEGER NOT NULL, 
                    `auth_method` INTEGER NOT NULL, 
                    `username` TEXT NOT NULL, 
                    `password` TEXT NOT NULL, 
                    `private_key` TEXT NOT NULL, 
                    `passphrase` TEXT NOT NULL, 
                    PRIMARY KEY(`uuid`)
                )
            """,
            )
            database.execSQL("ALTER TABLE `${Tables.DOCUMENTS}` ADD COLUMN `filesystem_uuid` TEXT NOT NULL DEFAULT 'local'")
            val cursor = database.query("SELECT * FROM `${Tables.DOCUMENTS}`")
            if (cursor.moveToFirst()) {
                do {
                    val columnUuid = cursor.getColumnIndexOrThrow("uuid")
                    val columnPath = cursor.getColumnIndexOrThrow("path")
                    val uuid = cursor.getString(columnUuid)
                    val path = cursor.getString(columnPath)
                    database.execSQL("UPDATE `${Tables.DOCUMENTS}` SET `path` = 'file://$path' WHERE `uuid` = '$uuid';")
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `${Tables.DOCUMENTS}` ADD COLUMN `language` TEXT NOT NULL DEFAULT 'plaintext'")
            val cursor = database.query("SELECT * FROM `${Tables.DOCUMENTS}`")
            if (cursor.moveToFirst()) {
                do {
                    val columnUuid = cursor.getColumnIndexOrThrow("uuid")
                    val columnPath = cursor.getColumnIndexOrThrow("path")
                    val uuid = cursor.getString(columnUuid)
                    val path = cursor.getString(columnPath)
                    val language = LanguageFactory.create(path).languageName
                    database.execSQL("UPDATE `${Tables.DOCUMENTS}` SET `language` = '$language' WHERE `uuid` = '$uuid';")
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `${Tables.THEMES}` ADD COLUMN `cursor_color` TEXT NOT NULL DEFAULT '#BBBBBB'")
            database.execSQL("ALTER TABLE `${Tables.SERVERS}` ADD COLUMN `initial_dir` TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE `${Tables.FONTS}` ADD COLUMN `font_uuid` TEXT NOT NULL DEFAULT 'legacy'")
            // database.execSQL("ALTER TABLE `${Tables.FONTS}` DROP COLUMN `support_ligatures`")
        }
    }
}