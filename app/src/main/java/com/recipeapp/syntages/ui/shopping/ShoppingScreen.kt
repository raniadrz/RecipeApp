package com.recipeapp.syntages.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recipeapp.syntages.data.ShoppingItem
import com.recipeapp.syntages.domain.Sections
import com.recipeapp.syntages.domain.Text
import com.recipeapp.syntages.domain.Units
import com.recipeapp.syntages.ui.appViewModel
import com.recipeapp.syntages.ui.components.EmptyState
import com.recipeapp.syntages.ui.recipes.Dropdown
import com.recipeapp.syntages.ui.recipes.ShareDialog
import com.recipeapp.syntages.util.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen() {
    val vm = appViewModel { ShoppingViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Λίστα αγορών") },
                actions = {
                    IconButton(onClick = { share = true }, enabled = state.items.any { !it.checked }) {
                        Icon(Icons.Filled.Share, "Αποστολή λίστας")
                    }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Περισσότερα") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Διαγραφή τσεκαρισμένων") },
                            leadingIcon = { Icon(Icons.Filled.DeleteSweep, null) },
                            enabled = state.checkedCount > 0,
                            onClick = { menu = false; vm.clearChecked() },
                        )
                        DropdownMenuItem(
                            text = { Text("Άδειασμα λίστας") },
                            enabled = state.items.isNotEmpty(),
                            onClick = { menu = false; confirmClear = true },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                val submit = {
                    if (input.isNotBlank()) { vm.addManual(input); input = "" }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Πρόσθεσε είδος, π.χ. 2 λίτρα γάλα") },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = submit, enabled = input.isNotBlank()) { Icon(Icons.Filled.Add, "Προσθήκη") }
            }
            if (state.items.isNotEmpty()) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Αγοράστηκαν ${state.checkedCount} από ${state.items.size}",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    LinearProgressIndicator(
                        progress = { state.checkedCount / state.items.size.toFloat() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (state.loaded && state.items.isEmpty()) {
                EmptyState(
                    Icons.Filled.ShoppingCart, "Η λίστα είναι άδεια",
                    "Πρόσθεσε υλικά από μια συνταγή («Στη λίστα αγορών»), από το εβδομαδιαίο πρόγραμμα, ή γράψε κάτι εδώ πάνω.",
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(320.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.groups, key = { it.first }) { (section, list) ->
                        SectionCard(section, list, onToggle = vm::toggle, onEdit = { editing = it })
                    }
                }
            }
        }
    }

    if (share) {
        ShareDialog("Αποστολή λίστας", onDismiss = { share = false }) { target ->
            share = false
            ShareUtils.share(context, ShareUtils.shoppingText(state.items), target)
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Άδειασμα λίστας;") },
            text = { Text("Θα διαγραφούν όλα τα είδη της λίστας.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearAll() }) { Text("Άδειασμα") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Ακύρωση") } },
        )
    }
    editing?.let { item ->
        EditItemDialog(
            item = item,
            onDismiss = { editing = null },
            onSave = { vm.update(it); editing = null },
            onDelete = { vm.delete(item); editing = null },
        )
    }
}

@Composable
private fun SectionCard(section: String, items: List<ShoppingItem>, onToggle: (ShoppingItem) -> Unit, onEdit: (ShoppingItem) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                section, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().clickable { onEdit(item) }.padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = item.checked, onCheckedChange = { onToggle(item) })
                    Column(Modifier.weight(1f)) {
                        val q = Units.display(item.quantity, item.unit)
                        Text(
                            buildString { if (q.isNotBlank()) append(q).append("  "); append(item.name) },
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        if (item.sources.isNotBlank()) {
                            Text(item.sources, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditItemDialog(item: ShoppingItem, onDismiss: () -> Unit, onSave: (ShoppingItem) -> Unit, onDelete: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var qty by remember { mutableStateOf(Text.formatQuantity(item.quantity)) }
    var unit by remember { mutableStateOf(item.unit) }
    var section by remember { mutableStateOf(item.section.ifBlank { Sections.OTHER }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Επεξεργασία είδους") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Είδος") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        qty, { qty = it.take(8) }, label = { Text("Ποσότητα") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                    )
                    val opts = if (unit in Units.choices) Units.choices else Units.choices + unit
                    Dropdown("Μονάδα", unit, opts, { Units.label(it) }, { unit = it }, Modifier.weight(1f))
                }
                Dropdown("Τμήμα", section, Sections.all, { it }, { section = it }, Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val (u, q) = Units.toBase(unit, Text.parseQuantity(qty))
                    onSave(item.copy(name = name.trim(), quantity = q, unit = u, section = section))
                },
            ) { Text("Αποθήκευση") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Διαγραφή", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Ακύρωση") }
            }
        },
    )
}
