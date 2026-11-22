package com.recipeapp.syntages.data

import androidx.room.withTransaction
import com.recipeapp.syntages.domain.Sections
import com.recipeapp.syntages.domain.Text
import com.recipeapp.syntages.domain.Units
import kotlinx.coroutines.flow.Flow

/** Ένα υλικό που θέλουμε να μπει στη λίστα αγορών. */
data class ShoppingInput(
    val name: String,
    val quantity: Double?,
    val unit: String,
    val section: String,
    val source: String = "",
    val manual: Boolean = false,
)

class RecipeRepository(val db: AppDatabase) {
    private val recipes = db.recipeDao()
    private val shopping = db.shoppingDao()
    private val plan = db.mealPlanDao()

    // ---------- Συνταγές ----------
    fun observeRecipes(): Flow<List<RecipeWithIngredients>> = recipes.observeAll()
    fun observeRecipe(id: Long): Flow<RecipeWithIngredients?> = recipes.observe(id)
    suspend fun getRecipe(id: Long) = recipes.get(id)
    suspend fun getAllRecipes() = recipes.getAll()
    suspend fun recipeCount() = recipes.count()
    suspend fun saveRecipe(recipe: Recipe, ingredients: List<Ingredient>): Long = recipes.save(recipe, ingredients)
    suspend fun deleteRecipe(id: Long) = recipes.delete(id)
    suspend fun setFavorite(id: Long, favorite: Boolean) = recipes.setFavorite(id, favorite)
    suspend fun setRating(id: Long, rating: Int) = recipes.setRating(id, rating)
    suspend fun setNotes(id: Long, notes: String) = recipes.setNotes(id, notes)

    // ---------- Λίστα αγορών ----------
    fun observeShopping(): Flow<List<ShoppingItem>> = shopping.observeAll()
    suspend fun setChecked(id: Long, checked: Boolean) = shopping.setChecked(id, checked)
    suspend fun updateShoppingItem(item: ShoppingItem) = shopping.update(item)
    suspend fun deleteShoppingItem(id: Long) = shopping.delete(id)
    suspend fun clearChecked() = shopping.deleteChecked()
    suspend fun clearShopping() = shopping.deleteAll()

    /**
     * Προσθέτει υλικά στη λίστα ενώνοντας τα ίδια: αν υπάρχει ήδη «2 κρεμμύδια» και προστεθεί
     * «1 κρεμμύδι», η λίστα γράφει «3 κρεμμύδια». Γραμμάρια/κιλά και ml/λίτρα αθροίζονται μεταξύ τους.
     * Επιστρέφει πόσα είδη προστέθηκαν ή ενημερώθηκαν.
     */
    suspend fun addToShopping(inputs: List<ShoppingInput>): Int = db.withTransaction {
        val current = shopping.getAll().filter { !it.checked }.toMutableList()
        var count = 0
        for (input in inputs) {
            if (input.name.isBlank()) continue
            val (unit, qty) = Units.toBase(input.unit, input.quantity)
            val key = Text.matchKey(input.name)
            val idx = current.indexOfFirst { Text.matchKey(it.name) == key && Units.normalizeUnit(it.unit) == unit }
            if (idx >= 0) {
                val old = current[idx]
                val newQty = when {
                    old.quantity == null -> qty
                    qty == null -> old.quantity
                    else -> old.quantity + qty
                }
                val name = if ((newQty ?: 0.0) > 1 && input.name.trim().length > old.name.length) input.name.trim() else old.name
                val merged = old.copy(
                    name = name,
                    quantity = newQty,
                    sources = mergeSources(old.sources, input.source),
                )
                shopping.update(merged)
                current[idx] = merged
            } else {
                val item = ShoppingItem(
                    name = input.name.trim(),
                    quantity = qty,
                    unit = unit,
                    section = input.section.ifBlank { Sections.guess(input.name) },
                    manual = input.manual,
                    sources = input.source,
                )
                val id = shopping.insert(item)
                current += item.copy(id = id)
            }
            count++
        }
        count
    }

    private fun mergeSources(a: String, b: String): String {
        if (b.isBlank()) return a
        val parts = a.split(" · ").filter { it.isNotBlank() }.toMutableList()
        if (b !in parts) parts += b
        return parts.joinToString(" · ")
    }

    suspend fun addRecipeToShopping(r: RecipeWithIngredients, factor: Double = 1.0): Int =
        addToShopping(r.ingredients.map {
            ShoppingInput(it.name, it.quantity?.times(factor), it.unit, it.section, r.recipe.title)
        })

    // ---------- Πρόγραμμα γευμάτων ----------
    fun observePlan(from: Long, to: Long): Flow<List<PlanRow>> = plan.observeRange(from, to)
    suspend fun addPlan(epochDay: Long, meal: Int, recipeId: Long) = plan.insert(MealPlanEntry(epochDay = epochDay, meal = meal, recipeId = recipeId))
    suspend fun removePlan(id: Long) = plan.delete(id)
    suspend fun clearPlan(from: Long, to: Long) = plan.clearRange(from, to)

    /** Φτιάχνει τη λίστα αγορών για όλες τις συνταγές του προγράμματος της εβδομάδας. */
    suspend fun addPlanToShopping(from: Long, to: Long): Pair<Int, Int> {
        val entries = plan.getRange(from, to)
        if (entries.isEmpty()) return 0 to 0
        val byId = recipes.getMany(entries.map { it.recipeId }.distinct()).associateBy { it.recipe.id }
        val inputs = entries.mapNotNull { byId[it.recipeId] }.flatMap { r ->
            r.ingredients.map { ShoppingInput(it.name, it.quantity, it.unit, it.section, r.recipe.title) }
        }
        return entries.size to addToShopping(inputs)
    }
}
