package com.recipeapp.syntages.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY title COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract fun observe(id: Long): Flow<RecipeWithIngredients?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract suspend fun get(id: Long): RecipeWithIngredients?

    @Transaction
    @Query("SELECT * FROM recipes WHERE id IN (:ids)")
    abstract suspend fun getMany(ids: List<Long>): List<RecipeWithIngredients>

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY title COLLATE NOCASE")
    abstract suspend fun getAll(): List<RecipeWithIngredients>

    @Query("SELECT uuid FROM recipes")
    abstract suspend fun allUuids(): List<String>

    @Query("SELECT id FROM recipes WHERE uuid = :uuid")
    abstract suspend fun idForUuid(uuid: String): Long?

    @Query("SELECT COUNT(*) FROM recipes")
    abstract suspend fun count(): Int

    @Insert
    abstract suspend fun insert(recipe: Recipe): Long

    @Update
    abstract suspend fun update(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Insert
    abstract suspend fun insertIngredients(list: List<Ingredient>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    abstract suspend fun deleteIngredients(recipeId: Long)

    @Query("UPDATE recipes SET favorite = :favorite WHERE id = :id")
    abstract suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE recipes SET rating = :rating WHERE id = :id")
    abstract suspend fun setRating(id: Long, rating: Int)

    @Query("UPDATE recipes SET notes = :notes WHERE id = :id")
    abstract suspend fun setNotes(id: Long, notes: String)

    @Transaction
    open suspend fun save(recipe: Recipe, ingredients: List<Ingredient>): Long {
        val id = if (recipe.id == 0L) insert(recipe) else {
            update(recipe); recipe.id
        }
        deleteIngredients(id)
        insertIngredients(ingredients.mapIndexed { i, ing -> ing.copy(id = 0, recipeId = id, position = i) })
        return id
    }
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY checked, createdAt")
    fun observeAll(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items")
    suspend fun getAll(): List<ShoppingItem>

    @Insert
    suspend fun insert(item: ShoppingItem): Long

    @Insert
    suspend fun insertAll(items: List<ShoppingItem>)

    @Update
    suspend fun update(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    suspend fun deleteChecked()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()

    @Query("UPDATE shopping_items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)
}

@Dao
interface MealPlanDao {
    @Query(
        """SELECT m.id AS id, m.epochDay AS epochDay, m.meal AS meal, m.recipeId AS recipeId,
                  r.title AS title, r.photoPath AS photoPath
           FROM meal_plan m JOIN recipes r ON r.id = m.recipeId
           WHERE m.epochDay BETWEEN :from AND :to
           ORDER BY m.epochDay, m.meal, m.id"""
    )
    fun observeRange(from: Long, to: Long): Flow<List<PlanRow>>

    @Query("SELECT * FROM meal_plan WHERE epochDay BETWEEN :from AND :to")
    suspend fun getRange(from: Long, to: Long): List<MealPlanEntry>

    @Query("SELECT * FROM meal_plan")
    suspend fun getAll(): List<MealPlanEntry>

    @Insert
    suspend fun insert(entry: MealPlanEntry): Long

    @Query("DELETE FROM meal_plan WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM meal_plan WHERE epochDay BETWEEN :from AND :to")
    suspend fun clearRange(from: Long, to: Long)
}
