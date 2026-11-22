package com.recipeapp.syntages.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipeapp.syntages.data.RecipeRepository
import com.recipeapp.syntages.data.RecipeWithIngredients
import com.recipeapp.syntages.data.SampleData
import com.recipeapp.syntages.domain.Text
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeFilters(
    val query: String = "",
    val favoritesOnly: Boolean = false,
    val category: String? = null,
    val tags: Set<String> = emptySet(),
    val difficulty: Int? = null,
) {
    val active get() = favoritesOnly || category != null || tags.isNotEmpty() || difficulty != null
}

data class RecipeHit(val item: RecipeWithIngredients, val matchedIngredients: List<String>)

data class RecipeListState(
    val loaded: Boolean = false,
    val total: Int = 0,
    val hits: List<RecipeHit> = emptyList(),
    val filters: RecipeFilters = RecipeFilters(),
    val categoriesInUse: List<String> = emptyList(),
)

class RecipeListViewModel(private val repo: RecipeRepository) : ViewModel() {
    private val filters = MutableStateFlow(RecipeFilters())

    private val all = repo.observeRecipes()

    val state: StateFlow<RecipeListState> = combine(all, filters) { recipes, f ->
        RecipeListState(
            loaded = true,
            total = recipes.size,
            hits = filter(recipes, f),
            filters = f,
            categoriesInUse = recipes.map { it.recipe.category }.distinct().sorted(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecipeListState())

    fun setQuery(q: String) = filters.update { it.copy(query = q) }
    fun toggleFavorites() = filters.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    fun setCategory(c: String?) = filters.update { it.copy(category = if (it.category == c) null else c) }
    fun toggleTag(t: String) = filters.update { it.copy(tags = if (t in it.tags) it.tags - t else it.tags + t) }
    fun setDifficulty(d: Int?) = filters.update { it.copy(difficulty = if (it.difficulty == d) null else d) }
    fun clearFilters() = filters.update { RecipeFilters(query = it.query) }

    fun toggleFavorite(id: Long, current: Boolean) = viewModelScope.launch { repo.setFavorite(id, !current) }

    fun addSamples() = viewModelScope.launch { SampleData.insert(repo) }

    companion object {
        /**
         * Αναζήτηση σε τίτλο, κατηγορία, ετικέτες και υλικά.
         * Με κόμμα («κοτόπουλο, πατάτες») ψάχνει συνταγές που έχουν ΟΛΑ τα υλικά.
         */
        fun filter(recipes: List<RecipeWithIngredients>, f: RecipeFilters): List<RecipeHit> {
            val terms = f.query.split(',').map { Text.normalize(it) }.filter { it.isNotBlank() }
            val multi = f.query.contains(',')
            return recipes.mapNotNull { r ->
                val rec = r.recipe
                if (f.favoritesOnly && !rec.favorite) return@mapNotNull null
                if (f.category != null && rec.category != f.category) return@mapNotNull null
                if (f.difficulty != null && rec.difficulty != f.difficulty) return@mapNotNull null
                if (!rec.tags.containsAll(f.tags)) return@mapNotNull null
                if (terms.isEmpty()) return@mapNotNull RecipeHit(r, emptyList())
                val ingNorm = r.ingredients.map { it.name to Text.normalize(it.name) }
                val matched = mutableListOf<String>()
                val allMatch = terms.all { term ->
                    val stem = Text.matchKey(term)
                    val ingHits = ingNorm.filter { (_, n) -> n.contains(term) || (stem.length >= 3 && Text.matchKey(n).contains(stem)) }
                    matched += ingHits.map { it.first }
                    val inMeta = !multi && (Text.normalize(rec.title).contains(term) ||
                        Text.normalize(rec.category).contains(term) ||
                        rec.tags.any { Text.normalize(it).contains(term) })
                    ingHits.isNotEmpty() || inMeta
                }
                if (allMatch) RecipeHit(r, matched.distinct()) else null
            }
        }
    }
}
