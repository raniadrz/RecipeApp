package com.recipeapp.syntages.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recipeapp.syntages.data.RecipeWithIngredients
import com.recipeapp.syntages.domain.Catalog
import com.recipeapp.syntages.domain.Units
import com.recipeapp.syntages.ui.appViewModel
import com.recipeapp.syntages.ui.components.EmptyState
import com.recipeapp.syntages.ui.components.RatingBar
import com.recipeapp.syntages.ui.components.RecipePhoto
import com.recipeapp.syntages.util.PhotoStore
import com.recipeapp.syntages.util.ShareTarget
import com.recipeapp.syntages.util.ShareUtils
import androidx.compose.material.icons.filled.SearchOff
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    onBack: (() -> Unit)?,
    onEdit: () -> Unit,
    onCook: () -> Unit,
    onDeleted: () -> Unit,
    insets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
) {
    val vm = appViewModel(key = "detail_$recipeId") { RecipeDetailViewModel(it.app, it.repository, recipeId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var shareDialog by remember { mutableStateOf(false) }

    val data = (state as? DetailState.Ready)?.data

    Scaffold(
        contentWindowInsets = insets,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(data?.recipe?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Πίσω") }
                },
                actions = {
                    if (data != null) {
                        IconButton(onClick = { vm.toggleFavorite(data.recipe.favorite) }) {
                            Icon(
                                if (data.recipe.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Αγαπημένο",
                                tint = if (data.recipe.favorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Επεξεργασία") }
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Περισσότερα") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Αποστολή σε φίλο") },
                                leadingIcon = { Icon(Icons.Filled.Share, null) },
                                onClick = { menu = false; shareDialog = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Διαγραφή") },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { menu = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (val s = state) {
            DetailState.Loading -> Box(Modifier.padding(padding))
            DetailState.Missing -> EmptyState(Icons.Filled.SearchOff, "Η συνταγή δεν βρέθηκε", "Μπορεί να έχει διαγραφεί.", Modifier.padding(padding))
            is DetailState.Ready -> DetailContent(
                data = s.data,
                modifier = Modifier.padding(padding),
                onRate = vm::setRating,
                onSaveNotes = { vm.saveNotes(it); scope.launch { snackbar.showSnackbar("Οι σημειώσεις αποθηκεύτηκαν") } },
                onAddToShopping = { factor ->
                    vm.addToShopping(s.data, factor) { n ->
                        scope.launch { snackbar.showSnackbar("Προστέθηκαν $n υλικά στη λίστα αγορών") }
                    }
                },
                onCook = onCook,
            )
        }
    }

    if (confirmDelete && data != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Διαγραφή συνταγής;") },
            text = { Text("Η συνταγή «${data.recipe.title}» θα διαγραφεί οριστικά.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(data, onDeleted) }) { Text("Διαγραφή") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Ακύρωση") } },
        )
    }
    if (shareDialog && data != null) {
        ShareDialog(
            title = "Αποστολή συνταγής",
            onDismiss = { shareDialog = false },
            onPick = { target ->
                shareDialog = false
                val uri = data.recipe.photoPath?.let { PhotoStore.shareUri(context, it) }
                ShareUtils.share(context, ShareUtils.recipeText(data), target, if (target == ShareTarget.SMS) null else uri)
            },
        )
    }
}

@Composable
fun ShareDialog(title: String, onDismiss: () -> Unit, onPick: (ShareTarget) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                ShareTarget.entries.forEach { t ->
                    TextButton(onClick = { onPick(t) }, modifier = Modifier.fillMaxWidth()) {
                        Text(t.label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ακύρωση") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    data: RecipeWithIngredients,
    modifier: Modifier,
    onRate: (Int) -> Unit,
    onSaveNotes: (String) -> Unit,
    onAddToShopping: (Double) -> Unit,
    onCook: () -> Unit,
) {
    val r = data.recipe
    var servings by rememberSaveable(r.id, r.servings) { mutableIntStateOf(r.servings.coerceAtLeast(1)) }
    val factor = servings.toDouble() / r.servings.coerceAtLeast(1)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            RecipePhoto(
                r.photoPath,
                Modifier.fillMaxWidth().heightIn(max = if (wide) 320.dp else 240.dp)
                    .then(if (r.photoPath == null) Modifier.heightIn(min = 120.dp) else Modifier),
                iconSize = 64.dp,
            )
            Column(Modifier.padding(16.dp)) {
                Text(r.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(r.category, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    AssistChip(onClick = {}, label = { Text("Προετοιμασία ${Catalog.minutesLabel(r.prepMinutes)}") })
                    AssistChip(onClick = {}, label = { Text("Μαγείρεμα ${Catalog.minutesLabel(r.cookMinutes)}") })
                    AssistChip(onClick = {}, label = { Text(Catalog.difficultyLabel(r.difficulty)) })
                    r.tags.forEach { t ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            Text(t, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingBar(r.rating, onRate)
                    if (r.rating == 0) Text("Βαθμολόγησε", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onCook, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 52.dp)) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ξεκίνα το μαγείρεμα", style = MaterialTheme.typography.titleMedium)
                }

                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(0.42f)) {
                            IngredientsSection(data, servings, factor, { servings = it }, onAddToShopping)
                        }
                        Column(Modifier.weight(0.58f)) { StepsSection(r.steps) }
                    }
                } else {
                    IngredientsSection(data, servings, factor, { servings = it }, onAddToShopping)
                    StepsSection(r.steps)
                }
                NotesSection(r.notes, onSaveNotes)
                Spacer(Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
}

@Composable
private fun IngredientsSection(
    data: RecipeWithIngredients,
    servings: Int,
    factor: Double,
    onServings: (Int) -> Unit,
    onAddToShopping: (Double) -> Unit,
) {
    SectionTitle("Υλικά")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Μερίδες", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { if (servings > 1) onServings(servings - 1) }) { Icon(Icons.Filled.Remove, "Λιγότερες") }
        Text("$servings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 24.dp))
        IconButton(onClick = { onServings(servings + 1) }) { Icon(Icons.Filled.Add, "Περισσότερες") }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(vertical = 4.dp)) {
            if (data.ingredients.isEmpty()) Text("Δεν έχουν προστεθεί υλικά.", Modifier.padding(16.dp))
            data.ingredients.forEachIndexed { i, ing ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    val q = Units.display(ing.quantity?.times(factor), ing.unit)
                    Text(
                        q, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.widthIn(min = 80.dp),
                    )
                    Text(ing.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    FilledTonalButton(
        onClick = { onAddToShopping(factor) },
        enabled = data.ingredients.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Icon(Icons.Filled.AddShoppingCart, null)
        Spacer(Modifier.width(8.dp))
        Text("Στη λίστα αγορών")
    }
}

@Composable
private fun StepsSection(steps: List<String>) {
    SectionTitle("Εκτέλεση")
    if (steps.isEmpty()) Text("Δεν έχουν προστεθεί βήματα.")
    steps.forEachIndexed { i, s ->
        Row(Modifier.padding(vertical = 8.dp)) {
            Box(
                Modifier.size(32.dp).clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.fillMaxSize()) {}
                Text("${i + 1}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Text(s, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun NotesSection(notes: String, onSave: (String) -> Unit) {
    var text by rememberSaveable(notes) { mutableStateOf(notes) }
    SectionTitle("Σημειώσεις")
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("π.χ. Την επόμενη φορά λιγότερο αλάτι") },
        minLines = 2,
    )
    LaunchedEffect(notes) { text = notes }
    if (text != notes) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { text = notes }) { Text("Ακύρωση") }
            Button(onClick = { onSave(text) }) { Text("Αποθήκευση") }
        }
    }
}
