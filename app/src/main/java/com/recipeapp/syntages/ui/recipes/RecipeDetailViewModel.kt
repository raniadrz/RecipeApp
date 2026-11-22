package com.recipeapp.syntages.ui.recipes

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipeapp.syntages.data.RecipeRepository
import com.recipeapp.syntages.data.RecipeWithIngredients
import com.recipeapp.syntages.util.PhotoStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState
    data object Missing : DetailState
    data class Ready(val data: RecipeWithIngredients) : DetailState
}

class RecipeDetailViewModel(private val app: Application, private val repo: RecipeRepository, val id: Long) : ViewModel() {
    val state: StateFlow<DetailState> = repo.observeRecipe(id)
        .map { if (it == null) DetailState.Missing else DetailState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailState.Loading)

    fun toggleFavorite(current: Boolean) = viewModelScope.launch { repo.setFavorite(id, !current) }
    fun setRating(r: Int) = viewModelScope.launch { repo.setRating(id, r) }
    fun saveNotes(n: String) = viewModelScope.launch { repo.setNotes(id, n.trim()) }

    fun addToShopping(r: RecipeWithIngredients, factor: Double, onDone: (Int) -> Unit) = viewModelScope.launch {
        onDone(repo.addRecipeToShopping(r, factor))
    }

    fun delete(r: RecipeWithIngredients, onDone: () -> Unit) = viewModelScope.launch {
        repo.deleteRecipe(r.recipe.id)
        PhotoStore.delete(app, r.recipe.photoPath)
        onDone()
    }
}
