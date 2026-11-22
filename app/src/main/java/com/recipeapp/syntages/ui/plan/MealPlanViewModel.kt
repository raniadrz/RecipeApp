package com.recipeapp.syntages.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipeapp.syntages.data.PlanRow
import com.recipeapp.syntages.data.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class MealPlanViewModel(private val repo: RecipeRepository) : ViewModel() {
    val weekStart = MutableStateFlow(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: StateFlow<List<PlanRow>> = weekStart.flatMapLatest { start ->
        repo.observePlan(start.toEpochDay(), start.plusDays(6).toEpochDay())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipes = repo.observeRecipes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun shiftWeek(weeks: Long) { weekStart.value = weekStart.value.plusWeeks(weeks) }
    fun thisWeek() { weekStart.value = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }

    fun add(day: LocalDate, meal: Int, recipeId: Long) = viewModelScope.launch { repo.addPlan(day.toEpochDay(), meal, recipeId) }
    fun remove(id: Long) = viewModelScope.launch { repo.removePlan(id) }
    fun clearWeek() = viewModelScope.launch {
        val s = weekStart.value
        repo.clearPlan(s.toEpochDay(), s.plusDays(6).toEpochDay())
    }

    fun buildShoppingList(onDone: (meals: Int, items: Int) -> Unit) = viewModelScope.launch {
        val s = weekStart.value
        val (meals, items) = repo.addPlanToShopping(s.toEpochDay(), s.plusDays(6).toEpochDay())
        onDone(meals, items)
    }
}
