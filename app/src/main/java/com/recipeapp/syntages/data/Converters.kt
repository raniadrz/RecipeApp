package com.recipeapp.syntages.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromList(list: List<String>): String = Json.encodeToString(list)

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isBlank()) emptyList() else runCatching { Json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}
