package com.recipeapp.syntages

import com.recipeapp.syntages.data.Ingredient
import com.recipeapp.syntages.data.Recipe
import com.recipeapp.syntages.data.RecipeWithIngredients
import com.recipeapp.syntages.domain.IngredientParser
import com.recipeapp.syntages.domain.Sections
import com.recipeapp.syntages.domain.Text
import com.recipeapp.syntages.domain.Units
import com.recipeapp.syntages.ui.cooking.CookingViewModel
import com.recipeapp.syntages.ui.recipes.RecipeFilters
import com.recipeapp.syntages.ui.recipes.RecipeListViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainTest {
    @Test fun parsesIngredients() {
        assertEquals(IngredientParserResult("κιμάς", 500.0, "γρ."), IngredientParser.parse("500 γρ. κιμάς").r())
        assertEquals(IngredientParserResult("κρεμμύδια", 2.0, ""), IngredientParser.parse("2 κρεμμύδια").r())
        assertEquals(IngredientParserResult("ζάχαρη", 1.0, "κ.σ."), IngredientParser.parse("1 κουταλιά της σούπας ζάχαρη").r())
        assertEquals(IngredientParserResult("γάλα", 1.5, "λίτρο"), IngredientParser.parse("1,5 λίτρα γάλα").r())
        assertEquals(IngredientParserResult("βούτυρο", 0.5, "φλιτζ."), IngredientParser.parse("1/2 φλιτζάνι βούτυρο").r())
        assertEquals(IngredientParserResult("αλάτι", null, ""), IngredientParser.parse("αλάτι").r())
        assertEquals(IngredientParserResult("σκόρδο", 3.0, "σκελίδα"), IngredientParser.parse("• 3 σκελίδες σκόρδο").r())
        assertNull(IngredientParser.parse("   "))
    }

    @Test fun sameIngredientMatches() {
        assertEquals(Text.matchKey("κρεμμύδι"), Text.matchKey("Κρεμμύδια"))
        assertEquals(Text.matchKey("ντομάτα"), Text.matchKey("ντομάτες"))
        assertEquals(Text.matchKey("αυγό"), Text.matchKey("αυγά"))
        assertEquals(Text.matchKey("μεγάλα κρεμμύδια"), Text.matchKey("κρεμμύδι"))
        assertNotEquals(Text.matchKey("πιπέρι"), Text.matchKey("πιπεριά"))
        assertNotEquals(Text.matchKey("κρεμμύδι"), Text.matchKey("κρεμμυδάκια"))
    }

    @Test fun unitsConvertAndDisplay() {
        assertEquals("γρ." to 1500.0, Units.toBase("κιλό", 1.5))
        assertEquals("1,5 κιλά", Units.display(1500.0, "γρ."))
        assertEquals("1 λίτρο", Units.display(1000.0, "ml"))
        assertEquals("3", Units.display(3.0, ""))
        assertEquals("250 γρ.", Units.display(250.0, "γρ."))
        assertEquals("0,33", Text.formatQuantity(1.0 / 3))
    }

    @Test fun guessesSections() {
        assertEquals(Sections.VEG, Sections.guess("Κρεμμύδια"))
        assertEquals(Sections.MEAT, Sections.guess("κοτόπουλο μπούτια"))
        assertEquals(Sections.DAIRY, Sections.guess("φέτα"))
        assertEquals(Sections.SPICES, Sections.guess("πιπέρι"))
        assertEquals(Sections.VEG, Sections.guess("πράσινη πιπεριά"))
        assertEquals(Sections.PANTRY, Sections.guess("ελαιόλαδο"))
        assertEquals(Sections.PASTA, Sections.guess("φακές"))
        assertEquals(Sections.PANTRY, Sections.guess("ζωμός λαχανικών"))
        assertEquals(Sections.SPICES, Sections.guess("μοσχοκάρυδο"))
        assertEquals(Sections.OTHER, Sections.guess("χαρτί κουζίνας"))
    }

    @Test fun searchByIngredient() {
        val chicken = rwi(1, "Λεμονάτο", "κοτόπουλο", "πατάτες")
        val salad = rwi(2, "Χωριάτικη", "ντομάτες", "φέτα")
        val pasta = rwi(3, "Κοτόσουπα", "κότα", "πατάτα")
        val all = listOf(chicken, salad, pasta)
        assertEquals(listOf(1L), RecipeListViewModel.filter(all, RecipeFilters(query = "Κοτοπουλο")).map { it.item.recipe.id })
        assertEquals(listOf(1L, 3L), RecipeListViewModel.filter(all, RecipeFilters(query = "πατάτα")).map { it.item.recipe.id })
        assertEquals(listOf(1L), RecipeListViewModel.filter(all, RecipeFilters(query = "κοτόπουλο, πατάτες")).map { it.item.recipe.id })
        assertEquals(listOf(2L), RecipeListViewModel.filter(all, RecipeFilters(query = "χωριατ")).map { it.item.recipe.id })
    }

    @Test fun detectsStepTime() {
        assertEquals(40, CookingViewModel.detectMinutes("Ψήσε για 40 λεπτά στους 180°"))
        assertEquals(90, CookingViewModel.detectMinutes("Σιγόβρασε 1,5 ώρα"))
        assertEquals(30, CookingViewModel.detectMinutes("Άφησέ το μισή ώρα"))
        assertEquals(10, CookingViewModel.detectMinutes("Βράσε 10-15 λεπτά"))
        assertNull(CookingViewModel.detectMinutes("Αλάτισε και σέρβιρε"))
    }

    private data class IngredientParserResult(val name: String, val q: Double?, val unit: String)
    private fun com.recipeapp.syntages.domain.ParsedIngredient?.r() = this?.let { IngredientParserResult(it.name, it.quantity, it.unit) }

    private fun rwi(id: Long, title: String, vararg ings: String) = RecipeWithIngredients(
        Recipe(id = id, title = title, category = "Κυρίως πιάτα"),
        ings.mapIndexed { i, n -> Ingredient(id = id * 10 + i, recipeId = id, position = i, name = n) },
    )
}
