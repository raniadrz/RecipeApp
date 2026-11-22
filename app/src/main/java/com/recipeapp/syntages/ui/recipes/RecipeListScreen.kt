package com.recipeapp.syntages.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recipeapp.syntages.domain.Catalog
import com.recipeapp.syntages.ui.appViewModel
import com.recipeapp.syntages.ui.components.EmptyState
import com.recipeapp.syntages.ui.components.RatingBar
import com.recipeapp.syntages.ui.components.RecipePhoto

/**
 * Αρχική οθόνη συνταγών. Σε στενές οθόνες (κινητό) δείχνει μόνο τη λίστα· σε φαρδιές
 * (foldable ανοιχτό, tablet, οριζόντια) δείχνει λίστα + λεπτομέρειες δίπλα-δίπλα.
 */
@Composable
fun RecipesRoute(
    onOpenRecipe: (Long) -> Unit,
    onAdd: () -> Unit,
    onImportUrl: (String) -> Unit,
    onEdit: (Long) -> Unit,
    onCook: (Long) -> Unit,
) {
    var selected by rememberSaveable { mutableLongStateOf(0L) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 700.dp
        if (twoPane) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(0.45f).fillMaxHeight()) {
                    RecipeListScreen(onOpen = { selected = it }, onAdd = onAdd, onImportUrl = onImportUrl, selectedId = selected, compactGrid = true)
                }
                VerticalDivider()
                Box(Modifier.weight(0.55f).fillMaxHeight()) {
                    if (selected != 0L) {
                        RecipeDetailScreen(
                            recipeId = selected,
                            onBack = null,
                            onEdit = { onEdit(selected) },
                            onCook = { onCook(selected) },
                            onDeleted = { selected = 0L },
                            insets = WindowInsets(0, 0, 0, 0),
                        )
                    } else {
                        EmptyState(Icons.Filled.MenuBook, "Διάλεξε μια συνταγή", "Οι λεπτομέρειες θα εμφανιστούν εδώ.")
                    }
                }
            }
        } else {
            RecipeListScreen(onOpen = onOpenRecipe, onAdd = onAdd, onImportUrl = onImportUrl, selectedId = 0L, compactGrid = false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(onOpen: (Long) -> Unit, onAdd: () -> Unit, onImportUrl: (String) -> Unit, selectedId: Long, compactGrid: Boolean) {
    val vm = appViewModel { RecipeListViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var webSearch by rememberSaveable { mutableStateOf(false) }
    val f = state.filters

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Οι Συνταγές μου") },
                actions = {
                    IconButton(onClick = { webSearch = true }) {
                        Icon(Icons.Filled.TravelExplore, contentDescription = "Βρες συνταγή στο internet")
                    }
                    IconButton(onClick = vm::toggleFavorites) {
                        Icon(
                            if (f.favoritesOnly) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Αγαπημένα",
                            tint = if (f.favoritesOnly) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        BadgedBox(badge = { if (f.active) Badge() }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Φίλτρα")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Νέα συνταγή") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = f.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Τι φτιάχνω με… (π.χ. κοτόπουλο)") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (f.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Filled.Clear, "Καθαρισμός") }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                supportingText = if (f.query.isNotEmpty() && !f.query.contains(',')) {
                    { Text("Συμβουλή: χώρισε υλικά με κόμμα, π.χ. «κοτόπουλο, πατάτες»") }
                } else null,
            )
            if (showFilters) FiltersPanel(state, vm)

            when {
                !state.loaded -> Unit
                state.total == 0 -> EmptyState(
                    Icons.Filled.MenuBook,
                    "Δεν υπάρχουν συνταγές ακόμα",
                    "Πάτησε «Νέα συνταγή» για να γράψεις την πρώτη σου, ή πρόσθεσε μερικά παραδείγματα για να δεις πώς δουλεύει η εφαρμογή.",
                ) {
                    OutlinedButton(onClick = vm::addSamples) { Text("Πρόσθεσε παραδείγματα") }
                }
                state.hits.isEmpty() -> EmptyState(
                    Icons.Filled.SearchOff, "Δεν βρέθηκε συνταγή",
                    "Δοκίμασε άλλη λέξη ή καθάρισε τα φίλτρα.",
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = { webSearch = true }) {
                            Icon(Icons.Filled.TravelExplore, null); Spacer(Modifier.width(8.dp)); Text("Ψάξε στο internet")
                        }
                        TextButton(onClick = { vm.setQuery(""); vm.clearFilters() }) { Text("Καθαρισμός φίλτρων") }
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (compactGrid) 260.dp else 300.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.hits, key = { it.item.recipe.id }) { hit ->
                        RecipeCard(
                            hit = hit,
                            selected = hit.item.recipe.id == selectedId,
                            onClick = { onOpen(hit.item.recipe.id) },
                            onFavorite = { vm.toggleFavorite(hit.item.recipe.id, hit.item.recipe.favorite) },
                        )
                    }
                }
            }
        }
    }
    if (webSearch) WebSearchDialog(f.query, onDismiss = { webSearch = false }, onImportUrl = onImportUrl)
}

@Composable
private fun WebSearchDialog(initial: String, onDismiss: () -> Unit, onImportUrl: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var text by rememberSaveable { mutableStateOf(initial) }
    val link = com.recipeapp.syntages.util.RecipeImporter.extractUrl(text)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.TravelExplore, null) },
        title = { Text("Βρες συνταγή στο internet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Τι θες να μαγειρέψεις; ή επικόλλησε link") },
                    placeholder = { Text("π.χ. παστίτσιο") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "1. Πάτα «Αναζήτηση» και διάλεξε μια συνταγή στον browser.\n" +
                        "2. Πάτα «Κοινοποίηση» (⋮ ή το εικονίδιο ⤴) → «Οι Συνταγές μου».\n" +
                        "3. Η συνταγή συμπληρώνεται αυτόματα — ελέγχεις και αποθηκεύεις.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    onDismiss()
                    if (link != null) onImportUrl(link)
                    else runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(com.recipeapp.syntages.util.RecipeImporter.googleSearchUrl(text.trim())))
                        )
                    }
                },
            ) { Text(if (link != null) "Εισαγωγή" else "Αναζήτηση") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ακύρωση") } },
    )
}

@Composable
private fun FiltersPanel(state: RecipeListState, vm: RecipeListViewModel) {
    val f = state.filters
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        FilterRow("Κατηγορία") {
            items(Catalog.categories.size) { i ->
                val c = Catalog.categories[i]
                FilterChip(selected = f.category == c, onClick = { vm.setCategory(c) }, label = { Text(c) })
            }
        }
        FilterRow("Ετικέτες") {
            items(Catalog.tags.size) { i ->
                val t = Catalog.tags[i]
                FilterChip(selected = t in f.tags, onClick = { vm.toggleTag(t) }, label = { Text(t) })
            }
        }
        FilterRow("Δυσκολία") {
            items(Catalog.difficulties.size) { i ->
                FilterChip(selected = f.difficulty == i, onClick = { vm.setDifficulty(i) }, label = { Text(Catalog.difficulties[i]) })
            }
            if (f.active) item { TextButton(onClick = vm::clearFilters) { Text("Καθαρισμός") } }
        }
    }
}

@Composable
private fun FilterRow(label: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Text(
        label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun RecipeCard(hit: RecipeHit, selected: Boolean, onClick: () -> Unit, onFavorite: () -> Unit) {
    val r = hit.item.recipe
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box {
            RecipePhoto(r.photoPath, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                IconButton(onClick = onFavorite, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (r.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Αγαπημένο",
                        tint = if (r.favorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(r.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(
                    "${Catalog.minutesLabel(r.prepMinutes + r.cookMinutes)} · ${Catalog.difficultyLabel(r.difficulty)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (r.rating > 0) RatingBar(r.rating, null, size = 14.dp)
            }
            if (r.tags.isNotEmpty()) {
                Text(
                    r.tags.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (hit.matchedIngredients.isNotEmpty()) {
                Text(
                    "Περιέχει: " + hit.matchedIngredients.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(4.dp)), maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
