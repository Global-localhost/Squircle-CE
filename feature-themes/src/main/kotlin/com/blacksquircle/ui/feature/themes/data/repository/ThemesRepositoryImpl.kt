/*
 * Copyright 2022 Squircle IDE contributors.
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

package com.blacksquircle.ui.feature.themes.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.blacksquircle.ui.core.data.storage.database.AppDatabase
import com.blacksquircle.ui.core.data.storage.database.entity.theme.ThemeEntity
import com.blacksquircle.ui.core.data.storage.keyvalue.SettingsManager
import com.blacksquircle.ui.core.domain.coroutine.DispatcherProvider
import com.blacksquircle.ui.feature.themes.data.converter.ThemeConverter
import com.blacksquircle.ui.feature.themes.data.model.ExternalTheme
import com.blacksquircle.ui.feature.themes.data.utils.InternalTheme
import com.blacksquircle.ui.feature.themes.domain.model.Meta
import com.blacksquircle.ui.feature.themes.domain.model.Property
import com.blacksquircle.ui.feature.themes.domain.model.PropertyItem
import com.blacksquircle.ui.feature.themes.domain.model.ThemeModel
import com.blacksquircle.ui.feature.themes.domain.repository.ThemesRepository
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File

class ThemesRepositoryImpl(
    private val dispatcherProvider: DispatcherProvider,
    private val settingsManager: SettingsManager,
    private val appDatabase: AppDatabase,
    private val context: Context
) : ThemesRepository {

    companion object {
        private const val FALLBACK_COLOR = "#000000"
        private const val MIME_TYPE_JSON = "application/json"
    }

    // region PROPERTIES

    private var textColor: String = FALLBACK_COLOR
    private var backgroundColor: String = FALLBACK_COLOR
    private var gutterColor: String = FALLBACK_COLOR
    private var gutterDividerColor: String = FALLBACK_COLOR
    private var gutterCurrentLineNumberColor: String = FALLBACK_COLOR
    private var gutterTextColor: String = FALLBACK_COLOR
    private var selectedLineColor: String = FALLBACK_COLOR
    private var selectionColor: String = FALLBACK_COLOR
    private var suggestionQueryColor: String = FALLBACK_COLOR
    private var findResultBackgroundColor: String = FALLBACK_COLOR
    private var delimiterBackgroundColor: String = FALLBACK_COLOR
    private var numberColor: String = FALLBACK_COLOR
    private var operatorColor: String = FALLBACK_COLOR
    private var keywordColor: String = FALLBACK_COLOR
    private var typeColor: String = FALLBACK_COLOR
    private var langConstColor: String = FALLBACK_COLOR
    private var preprocessorColor: String = FALLBACK_COLOR
    private var variableColor: String = FALLBACK_COLOR
    private var methodColor: String = FALLBACK_COLOR
    private var stringColor: String = FALLBACK_COLOR
    private var commentColor: String = FALLBACK_COLOR
    private var tagColor: String = FALLBACK_COLOR
    private var tagNameColor: String = FALLBACK_COLOR
    private var attrNameColor: String = FALLBACK_COLOR
    private var attrValueColor: String = FALLBACK_COLOR
    private var entityRefColor: String = FALLBACK_COLOR

    // endregion PROPERTIES

    override suspend fun fetchThemes(query: String): List<ThemeModel> {
        return withContext(dispatcherProvider.io()) {
            val defaultThemes = InternalTheme.values()
                .map(InternalTheme::theme)
                .filter { it.name.contains(query, ignoreCase = true) }
            val userThemes = appDatabase.themeDao().loadAll(query)
                .map(ThemeConverter::toModel)
            userThemes + defaultThemes
        }
    }

    override suspend fun fetchTheme(uuid: String): ThemeModel {
        return withContext(dispatcherProvider.io()) {
            val themeEntity = appDatabase.themeDao().load(uuid)
            ThemeConverter.toModel(themeEntity)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun importTheme(uri: Uri): ThemeModel {
        return withContext(dispatcherProvider.io()) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val themeJson = inputStream?.bufferedReader()?.use(BufferedReader::readText)!!
            val externalTheme = ExternalTheme.deserialize(themeJson)
            ThemeConverter.toModel(externalTheme)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun exportTheme(themeModel: ThemeModel) {
        withContext(dispatcherProvider.io()) {
            val fileName = "${themeModel.name}.json"
            val fileDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileText = ExternalTheme.serialize(ThemeConverter.toExternalTheme(themeModel))

            val resolver = context.contentResolver
            val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE_JSON)
                    put(MediaStore.MediaColumns.SIZE, fileText.length)
                }
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                val file = File(fileDir, fileName)
                val authority = "${context.packageName}.provider"
                file.deleteRecursively()
                file.createNewFile()
                FileProvider.getUriForFile(context, authority, file)
            } ?: Uri.EMPTY

            resolver.openOutputStream(fileUri)?.use { output ->
                output.write(fileText.toByteArray())
                output.flush()
            }
        }
    }

    override suspend fun createTheme(meta: Meta, properties: List<PropertyItem>) {
        return withContext(dispatcherProvider.io()) {
            for (property in properties) {
                when (property.propertyKey) {
                    Property.TEXT_COLOR -> textColor = property.propertyValue
                    Property.BACKGROUND_COLOR -> backgroundColor = property.propertyValue
                    Property.GUTTER_COLOR -> gutterColor = property.propertyValue
                    Property.GUTTER_DIVIDER_COLOR -> gutterDividerColor = property.propertyValue
                    Property.GUTTER_CURRENT_LINE_NUMBER_COLOR -> gutterCurrentLineNumberColor = property.propertyValue
                    Property.GUTTER_TEXT_COLOR -> gutterTextColor = property.propertyValue
                    Property.SELECTED_LINE_COLOR -> selectedLineColor = property.propertyValue
                    Property.SELECTION_COLOR -> selectionColor = property.propertyValue
                    Property.SUGGESTION_QUERY_COLOR -> suggestionQueryColor = property.propertyValue
                    Property.FIND_RESULT_BACKGROUND_COLOR -> findResultBackgroundColor = property.propertyValue
                    Property.DELIMITER_BACKGROUND_COLOR -> delimiterBackgroundColor = property.propertyValue
                    Property.NUMBER_COLOR -> numberColor = property.propertyValue
                    Property.OPERATOR_COLOR -> operatorColor = property.propertyValue
                    Property.KEYWORD_COLOR -> keywordColor = property.propertyValue
                    Property.TYPE_COLOR -> typeColor = property.propertyValue
                    Property.LANG_CONST_COLOR -> langConstColor = property.propertyValue
                    Property.PREPROCESSOR_COLOR -> preprocessorColor = property.propertyValue
                    Property.VARIABLE_COLOR -> variableColor = property.propertyValue
                    Property.METHOD_COLOR -> methodColor = property.propertyValue
                    Property.STRING_COLOR -> stringColor = property.propertyValue
                    Property.COMMENT_COLOR -> commentColor = property.propertyValue
                    Property.TAG_COLOR -> tagColor = property.propertyValue
                    Property.TAG_NAME_COLOR -> tagNameColor = property.propertyValue
                    Property.ATTR_NAME_COLOR -> attrNameColor = property.propertyValue
                    Property.ATTR_VALUE_COLOR -> attrValueColor = property.propertyValue
                    Property.ENTITY_REF_COLOR -> entityRefColor = property.propertyValue
                }
            }
            val themeEntity = ThemeEntity(
                uuid = meta.uuid,
                name = meta.name,
                author = meta.author,
                description = meta.description,
                textColor = textColor,
                backgroundColor = backgroundColor,
                gutterColor = gutterColor,
                gutterDividerColor = gutterDividerColor,
                gutterCurrentLineNumberColor = gutterCurrentLineNumberColor,
                gutterTextColor = gutterTextColor,
                selectedLineColor = selectedLineColor,
                selectionColor = selectionColor,
                suggestionQueryColor = suggestionQueryColor,
                findResultBackgroundColor = findResultBackgroundColor,
                delimiterBackgroundColor = delimiterBackgroundColor,
                numberColor = numberColor,
                operatorColor = operatorColor,
                keywordColor = keywordColor,
                typeColor = typeColor,
                langConstColor = langConstColor,
                preprocessorColor = preprocessorColor,
                variableColor = variableColor,
                methodColor = methodColor,
                stringColor = stringColor,
                commentColor = commentColor,
                tagColor = tagColor,
                tagNameColor = tagNameColor,
                attrNameColor = attrNameColor,
                attrValueColor = attrValueColor,
                entityRefColor = entityRefColor
            )

            appDatabase.themeDao().insert(themeEntity)

            textColor = FALLBACK_COLOR
            backgroundColor = FALLBACK_COLOR
            gutterColor = FALLBACK_COLOR
            gutterDividerColor = FALLBACK_COLOR
            gutterCurrentLineNumberColor = FALLBACK_COLOR
            gutterTextColor = FALLBACK_COLOR
            selectedLineColor = FALLBACK_COLOR
            selectionColor = FALLBACK_COLOR
            suggestionQueryColor = FALLBACK_COLOR
            findResultBackgroundColor = FALLBACK_COLOR
            delimiterBackgroundColor = FALLBACK_COLOR
            numberColor = FALLBACK_COLOR
            operatorColor = FALLBACK_COLOR
            keywordColor = FALLBACK_COLOR
            typeColor = FALLBACK_COLOR
            langConstColor = FALLBACK_COLOR
            variableColor = FALLBACK_COLOR
            methodColor = FALLBACK_COLOR
            stringColor = FALLBACK_COLOR
            commentColor = FALLBACK_COLOR
            tagColor = FALLBACK_COLOR
            tagNameColor = FALLBACK_COLOR
            attrNameColor = FALLBACK_COLOR
            attrValueColor = FALLBACK_COLOR
            entityRefColor = FALLBACK_COLOR
        }
    }

    override suspend fun removeTheme(themeModel: ThemeModel) {
        withContext(dispatcherProvider.io()) {
            appDatabase.themeDao().delete(ThemeConverter.toEntity(themeModel))
            if (settingsManager.colorScheme == themeModel.uuid) {
                settingsManager.remove(SettingsManager.KEY_COLOR_SCHEME)
            }
        }
    }

    override suspend fun selectTheme(themeModel: ThemeModel) {
        withContext(dispatcherProvider.io()) {
            settingsManager.colorScheme = themeModel.uuid
        }
    }
}