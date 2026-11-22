package com.recipeapp.syntages.ui.recipes

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipeapp.syntages.data.Ingredient
import com.recipeapp.syntages.data.Recipe
import com.recipeapp.syntages.data.RecipeRepository
import com.recipeapp.syntages.domain.Catalog
import com.recipeapp.syntages.domain.IngredientParser
import com.recipeapp.syntages.domain.Sections
import com.recipeapp.syntages.domain.Text
import com.recipeapp.syntages.domain.Units
import com.recipeapp.syntages.domain.WebRecipe
import com.recipeapp.syntages.util.PhotoStore
import com.recipeapp.syntages.util.RecipeImporter
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class IngredientDraft(name: String = "", qty: String = "", unit: String = "", section: String = "", sectionManual: Boolean = false) {
    val key: Long = counter.incrementAndGet()
    var name by mutableStateOf(name)
    var qty by mutableStateOf(qty)
    var unit by mutableStateOf(unit)
    var section by mutableStateOf(section)
    var sectionManual by mutableStateOf(sectionManual)

    fun updateName(n: String) {
        name = n
        if (!sectionManual) section = Sections.guess(n)
    }

    companion object { private val counter = AtomicLong() }
}

class StepDraft(text: String = "") {
    val key: Long = counter.incrementAndGet()
    var text by mutableStateOf(text)
    companion object { private val counter = AtomicLong() }
}

class RecipeEditViewModel(private val app: Application, private val repo: RecipeRepository, val id: Long) : ViewModel() {
    var loaded by mutableStateOf(id == 0L)
        private set
    private var original: Recipe? = null

    var title by mutableStateOf("")
    var category by mutableStateOf(Catalog.categories[3])
    var prep by mutableStateOf("")
    var cook by mutableStateOf("")
    var servings by mutableStateOf("4")
    var difficulty by mutableStateOf(0)
    val tags = mutableStateListOf<String>()
    var photo by mutableStateOf<String?>(null)
        private set
    var photoBusy by mutableStateOf(false)
        private set
    val ingredients = mutableStateListOf<IngredientDraft>()
    val steps = mutableStateListOf<StepDraft>()
    var titleError by mutableStateOf(false)

    // ---- Εισαγωγή από internet ----
    var importing by mutableStateOf(false)
        private set
    var importError by mutableStateOf<String?>(null)
    var importMessage by mutableStateOf<String?>(null)
    private var sourceUrl: String? = null
    var autoImportDone = false

    /** Όταν το σάιτ μπλοκάρει την απευθείας λήψη, ανοίγουμε τη σελίδα σε ενσωματωμένο browser. */
    var browserImportUrl by mutableStateOf<String?>(null)
        private set
    private var partial: WebRecipe? = null

    /** Φωτογραφίες που δημιουργήθηκαν σε αυτή την επεξεργασία (για καθαρισμό αν δεν αποθηκευτούν). */
    private val newPhotos = mutableSetOf<String>()
    private var saved = false

    init {
        if (id != 0L) {
            viewModelScope.launch {
                repo.getRecipe(id)?.let { r ->
                    val rec = r.recipe
                    original = rec
                    title = rec.title
                    category = rec.category
                    prep = if (rec.prepMinutes > 0) rec.prepMinutes.toString() else ""
                    cook = if (rec.cookMinutes > 0) rec.cookMinutes.toString() else ""
                    servings = rec.servings.toString()
                    difficulty = rec.difficulty
                    tags.addAll(rec.tags)
                    photo = rec.photoPath
                    r.ingredients.forEach {
                        ingredients += IngredientDraft(it.name, Text.formatQuantity(it.quantity), it.unit, it.section, sectionManual = true)
                    }
                    rec.steps.forEach { steps += StepDraft(it) }
                }
                if (ingredients.isEmpty()) ingredients += IngredientDraft()
                if (steps.isEmpty()) steps += StepDraft()
                loaded = true
            }
        } else {
            ingredients += IngredientDraft()
            steps += StepDraft()
        }
    }

    fun toggleTag(t: String) { if (t in tags) tags.remove(t) else tags.add(t) }

    fun addIngredient() { ingredients += IngredientDraft() }
    fun removeIngredient(d: IngredientDraft) { ingredients.remove(d) }

    fun addParsedIngredients(text: String) {
        val parsed = IngredientParser.parseLines(text)
        ingredients.removeAll { it.name.isBlank() && it.qty.isBlank() }
        parsed.forEach {
            ingredients += IngredientDraft(it.name, Text.formatQuantity(it.quantity), it.unit, Sections.guess(it.name))
        }
        if (ingredients.isEmpty()) ingredients += IngredientDraft()
    }

    fun addStep() { steps += StepDraft() }
    fun removeStep(s: StepDraft) { steps.remove(s) }
    fun moveStep(s: StepDraft, delta: Int) {
        val i = steps.indexOf(s)
        val j = i + delta
        if (i < 0 || j !in steps.indices) return
        steps.removeAt(i)
        steps.add(j, s)
    }

    fun addParsedSteps(text: String) {
        val lines = text.lines().map { it.replace(Regex("^\\s*(\\d+[.)]|[•\\-*])\\s*"), "").trim() }.filter { it.isNotEmpty() }
        steps.removeAll { it.text.isBlank() }
        lines.forEach { steps += StepDraft(it) }
        if (steps.isEmpty()) steps += StepDraft()
    }

    fun setPhotoFromUri(uri: Uri, deleteSource: java.io.File? = null) {
        photoBusy = true
        viewModelScope.launch {
            val name = PhotoStore.import(app, uri)
            deleteSource?.delete()
            if (name != null) {
                // Αν η προηγούμενη φωτογραφία ήταν νέα (μη αποθηκευμένη), σβήσ' την.
                photo?.let { if (it in newPhotos) { PhotoStore.delete(app, it); newPhotos.remove(it) } }
                newPhotos += name
                photo = name
            }
            photoBusy = false
        }
    }

    fun importFromUrl(rawUrl: String) {
        val url = RecipeImporter.extractUrl(rawUrl.trim()) ?: run {
            importError = "Αυτό δεν μοιάζει με link. Αντίγραψε τη διεύθυνση της σελίδας (ξεκινά με https://)."
            return
        }
        importing = true
        importError = null
        partial = null
        viewModelScope.launch {
            runCatching { RecipeImporter.load(url) }
                .onSuccess {
                    if (it.isComplete) { apply(it); importing = false }
                    else { partial = it; browserImportUrl = url } // ίσως τα υλικά φορτώνονται με JavaScript
                }
                .onFailure { e ->
                    val blocked = e is com.recipeapp.syntages.util.HttpStatusException && e.code in setOf(401, 403, 405, 406, 429, 503)
                    val notFound = e is com.recipeapp.syntages.util.HttpStatusException && (e.code == 404 || e.code == 410)
                    if (notFound) {
                        importError = "Η σελίδα δεν βρέθηκε (σφάλμα ${e.code}). Έλεγξε ότι το link είναι σωστό."
                        importing = false
                    } else if (blocked || e !is java.net.UnknownHostException) {
                        browserImportUrl = url
                    } else {
                        importError = "Δεν υπάρχει σύνδεση στο internet."
                        importing = false
                    }
                }
        }
    }

    /** Αποτέλεσμα από τον ενσωματωμένο browser (html == null: ακυρώθηκε ή δεν βρέθηκε συνταγή). */
    fun onBrowserResult(html: String?, finalUrl: String?) {
        val url = browserImportUrl ?: return
        browserImportUrl = null
        val parsed = html?.let { com.recipeapp.syntages.domain.WebRecipeParser.parse(it, url) }
        val best = when {
            parsed?.isComplete == true -> parsed
            partial != null -> partial
            else -> parsed
        }
        partial = null
        if (best == null) {
            importError = "Δεν βρέθηκε συνταγή σε αυτή τη σελίδα. Δοκίμασε άλλη σελίδα ή γράψε τη συνταγή με «Επικόλληση»."
            importing = false
            return
        }
        viewModelScope.launch {
            apply(best, referer = finalUrl ?: url)
            importing = false
        }
    }

    private suspend fun apply(w: WebRecipe, referer: String? = w.sourceUrl) {
        if (w.title.isNotBlank()) title = w.title
        w.category?.let { category = it }
        if (w.prepMinutes > 0) prep = w.prepMinutes.toString()
        if (w.cookMinutes > 0) cook = w.cookMinutes.toString()
        w.servings?.let { servings = it.toString() }
        w.tags.forEach { if (it !in tags) tags += it }
        if (w.ingredients.isNotEmpty()) {
            ingredients.clear()
            w.ingredients.mapNotNull { IngredientParser.parse(it) }.forEach {
                ingredients += IngredientDraft(it.name, Text.formatQuantity(it.quantity), it.unit, Sections.guess(it.name))
            }
            if (ingredients.isEmpty()) ingredients += IngredientDraft()
        }
        if (w.steps.isNotEmpty()) {
            steps.clear()
            w.steps.forEach { steps += StepDraft(it) }
        }
        sourceUrl = w.sourceUrl
        importMessage = if (w.isComplete)
            "Η συνταγή συμπληρώθηκε! Έλεγξέ τη και πάτα «Αποθήκευση»."
        else
            "Βρέθηκε μόνο ο τίτλος — η σελίδα δεν έχει τα υλικά σε μορφή που διαβάζεται. Συμπλήρωσέ τα με «Επικόλληση»."
        w.imageUrl?.let { img ->
            photoBusy = true
            val name = RecipeImporter.downloadImage(app, img, referer)
            if (name != null) {
                photo?.let { if (it in newPhotos) { PhotoStore.delete(app, it); newPhotos.remove(it) } }
                newPhotos += name
                photo = name
            }
            photoBusy = false
        }
    }

    fun removePhoto() {
        photo?.let { if (it in newPhotos) { PhotoStore.delete(app, it); newPhotos.remove(it) } }
        photo = null
    }

    fun save(onSaved: (Long) -> Unit) {
        if (title.isBlank()) { titleError = true; return }
        val ings = ingredients.filter { it.name.isNotBlank() }.map {
            Ingredient(
                name = it.name.trim(),
                quantity = Text.parseQuantity(it.qty),
                unit = Units.normalizeUnit(it.unit),
                section = it.section.ifBlank { Sections.guess(it.name) },
            )
        }
        val base = original ?: Recipe(title = "", category = category)
        val recipe = base.copy(
            title = title.trim(),
            category = category,
            prepMinutes = prep.filter(Char::isDigit).toIntOrNull() ?: 0,
            cookMinutes = cook.filter(Char::isDigit).toIntOrNull() ?: 0,
            servings = (servings.filter(Char::isDigit).toIntOrNull() ?: 4).coerceIn(1, 100),
            difficulty = difficulty,
            tags = Catalog.tags.filter { it in tags },
            photoPath = photo,
            steps = steps.map { it.text.trim() }.filter { it.isNotEmpty() },
            notes = if (base.notes.isBlank() && sourceUrl != null) "Πηγή: $sourceUrl" else base.notes,
        )
        viewModelScope.launch {
            val newId = repo.saveRecipe(recipe, ings)
            saved = true
            val old = original?.photoPath
            if (old != null && old != photo) PhotoStore.delete(app, old)
            newPhotos.remove(photo)
            newPhotos.forEach { PhotoStore.delete(app, it) }
            newPhotos.clear()
            onSaved(newId)
        }
    }

    override fun onCleared() {
        if (!saved) newPhotos.forEach { PhotoStore.delete(app, it) }
    }
}
