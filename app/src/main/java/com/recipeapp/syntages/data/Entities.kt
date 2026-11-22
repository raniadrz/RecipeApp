package com.recipeapp.syntages.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "recipes", indices = [Index(value = ["uuid"], unique = true)])
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val steps: List<String> = emptyList(),
    val photoPath: String? = null,
    val prepMinutes: Int = 0,
    val cookMinutes: Int = 0,
    /** 0 = εύκολη, 1 = μέτρια, 2 = δύσκολη */
    val difficulty: Int = 0,
    val servings: Int = 4,
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    val rating: Int = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "ingredients",
    foreignKeys = [ForeignKey(
        entity = Recipe::class,
        parentColumns = ["id"],
        childColumns = ["recipeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("recipeId")],
)
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long = 0,
    val position: Int = 0,
    val name: String,
    val quantity: Double? = null,
    val unit: String = "",
    val section: String = "",
)

data class RecipeWithIngredients(
    @Embedded val recipe: Recipe,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredientsRaw: List<Ingredient>,
) {
    val ingredients: List<Ingredient> get() = ingredientsRaw.sortedBy { it.position }
}

@Serializable
@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double? = null,
    val unit: String = "",
    val section: String = "",
    val checked: Boolean = false,
    val manual: Boolean = false,
    /** Τίτλοι συνταγών από τις οποίες προήλθε το είδος, χωρισμένοι με " · ". */
    val sources: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "meal_plan",
    foreignKeys = [ForeignKey(
        entity = Recipe::class,
        parentColumns = ["id"],
        childColumns = ["recipeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("recipeId"), Index("epochDay")],
)
data class MealPlanEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    /** 0 = πρωινό, 1 = μεσημεριανό, 2 = βραδινό */
    val meal: Int,
    val recipeId: Long,
)

data class PlanRow(
    val id: Long,
    val epochDay: Long,
    val meal: Int,
    val recipeId: Long,
    val title: String,
    val photoPath: String?,
)
