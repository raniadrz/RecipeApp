package com.recipeapp.syntages.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.recipeapp.syntages.ui.cooking.CookingScreen
import com.recipeapp.syntages.ui.plan.MealPlanScreen
import com.recipeapp.syntages.ui.recipes.RecipeDetailScreen
import com.recipeapp.syntages.ui.recipes.RecipeEditScreen
import com.recipeapp.syntages.ui.recipes.RecipesRoute
import com.recipeapp.syntages.ui.settings.SettingsScreen
import com.recipeapp.syntages.ui.shopping.ShoppingScreen

private enum class TopDest(val route: String, val label: String, val icon: ImageVector) {
    RECIPES("recipes", "Συνταγές", Icons.Filled.MenuBook),
    PLAN("plan", "Πρόγραμμα", Icons.Filled.CalendarMonth),
    SHOPPING("shopping", "Λίστα αγορών", Icons.Filled.ShoppingCart),
    SETTINGS("settings", "Ρυθμίσεις", Icons.Filled.Settings),
}

@Composable
fun SyntagesApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val top = TopDest.entries.firstOrNull { it.route == route }

    // Κάτω μπάρα σε κινητό, πλαϊνή μπάρα σε foldable/tablet· κρυμμένη σε επεξεργασία/μαγείρεμα.
    val layoutType = if (top == null) NavigationSuiteType.None
    else NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            TopDest.entries.forEach { d ->
                item(
                    selected = top == d,
                    onClick = { nav.navigateTop(d.route) },
                    icon = { Icon(d.icon, contentDescription = null) },
                    label = { Text(d.label) },
                )
            }
        },
    ) {
        NavHost(nav, startDestination = TopDest.RECIPES.route) {
            composable(TopDest.RECIPES.route) {
                RecipesRoute(
                    onOpenRecipe = { nav.navigate("recipe/$it") },
                    onAdd = { nav.navigate("edit/0") },
                    onEdit = { nav.navigate("edit/$it") },
                    onCook = { nav.navigate("cook/$it") },
                )
            }
            composable("recipe/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                RecipeDetailScreen(
                    recipeId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("edit/$id") },
                    onCook = { nav.navigate("cook/$id") },
                    onDeleted = { nav.popBackStack() },
                )
            }
            composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                RecipeEditScreen(recipeId = id) { savedId ->
                    nav.popBackStack()
                    // Μετά τη δημιουργία νέας συνταγής, άνοιξέ την.
                    if (id == 0L && savedId != null) nav.navigate("recipe/$savedId")
                }
            }
            composable("cook/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                CookingScreen(recipeId = entry.arguments?.getLong("id") ?: 0L, onClose = { nav.popBackStack() })
            }
            composable(TopDest.PLAN.route) { MealPlanScreen(onOpenRecipe = { nav.navigate("recipe/$it") }) }
            composable(TopDest.SHOPPING.route) { ShoppingScreen() }
            composable(TopDest.SETTINGS.route) { SettingsScreen() }
        }
    }
}

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
