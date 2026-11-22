package com.recipeapp.syntages.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipeapp.syntages.data.RecipeRepository
import com.recipeapp.syntages.data.ShoppingInput
import com.recipeapp.syntages.data.ShoppingItem
import com.recipeapp.syntages.domain.IngredientParser
import com.recipeapp.syntages.domain.Sections
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingState(
    val loaded: Boolean = false,
    val items: List<ShoppingItem> = emptyList(),
    val groups: List<Pair<String, List<ShoppingItem>>> = emptyList(),
) {
    val checkedCount get() = items.count { it.checked }
}

class ShoppingViewModel(private val repo: RecipeRepository) : ViewModel() {
    val state: StateFlow<ShoppingState> = repo.observeShopping().map { items ->
        val groups = items.groupBy { it.section.ifBlank { Sections.OTHER } }
            .toList()
            .sortedBy { Sections.order(it.first) }
            .map { (s, list) -> s to list.sortedWith(compareBy<ShoppingItem> { it.checked }.thenBy { it.name.lowercase() }) }
        ShoppingState(true, items, groups)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShoppingState())

    /** Προσθήκη με το χέρι, π.χ. «2 λίτρα γάλα» ή «χαρτί κουζίνας». */
    fun addManual(text: String) = viewModelScope.launch {
        val inputs = text.split('\n').mapNotNull { IngredientParser.parse(it) }.map {
            ShoppingInput(it.name, it.quantity, it.unit, Sections.guess(it.name), manual = true)
        }
        repo.addToShopping(inputs)
    }

    fun toggle(item: ShoppingItem) = viewModelScope.launch { repo.setChecked(item.id, !item.checked) }
    fun update(item: ShoppingItem) = viewModelScope.launch { repo.updateShoppingItem(item) }
    fun delete(item: ShoppingItem) = viewModelScope.launch { repo.deleteShoppingItem(item.id) }
    fun clearChecked() = viewModelScope.launch { repo.clearChecked() }
    fun clearAll() = viewModelScope.launch { repo.clearShopping() }
}
