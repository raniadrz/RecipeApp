package com.recipeapp.syntages.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recipeapp.syntages.data.PlanRow
import com.recipeapp.syntages.domain.Catalog
import com.recipeapp.syntages.domain.Text
import com.recipeapp.syntages.ui.appViewModel
import com.recipeapp.syntages.ui.components.RecipePickerDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(onOpenRecipe: (Long) -> Unit) {
    val vm = appViewModel { MealPlanViewModel(it.repository) }
    val start by vm.weekStart.collectAsStateWithLifecycle()
    val entries by vm.entries.collectAsStateWithLifecycle()
    val recipes by vm.recipes.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf<Pair<LocalDate, Int>?>(null) }
    var menu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val fmt = DateTimeFormatter.ofPattern("d MMM", Text.greek)
    val today = LocalDate.now()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Πρόγραμμα γευμάτων") },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Περισσότερα") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Τρέχουσα εβδομάδα") }, onClick = { menu = false; vm.thisWeek() })
                        DropdownMenuItem(
                            text = { Text("Καθαρισμός εβδομάδας") }, enabled = entries.isNotEmpty(),
                            onClick = { menu = false; confirmClear = true },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (entries.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        vm.buildShoppingList { meals, items ->
                            scope.launch { snackbar.showSnackbar("Από $meals γεύματα προστέθηκαν $items υλικά στη λίστα αγορών") }
                        }
                    },
                    icon = { Icon(Icons.Filled.AddShoppingCart, null) },
                    text = { Text("Λίστα αγορών εβδομάδας") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = { vm.shiftWeek(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Προηγούμενη εβδομάδα") }
                Text(
                    "${start.format(fmt)} – ${start.plusDays(6).format(fmt)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.widthIn(min = 160.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                IconButton(onClick = { vm.shiftWeek(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Επόμενη εβδομάδα") }
            }
            val days = (0L..6L).map { start.plusDays(it) }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(days, key = { it.toEpochDay() }) { day ->
                    DayCard(
                        day = day,
                        isToday = day == today,
                        entries = entries.filter { it.epochDay == day.toEpochDay() },
                        onAdd = { meal -> picking = day to meal },
                        onRemove = { vm.remove(it.id) },
                        onOpen = { onOpenRecipe(it.recipeId) },
                    )
                }
            }
        }
    }

    picking?.let { (day, meal) ->
        RecipePickerDialog(
            recipes = recipes,
            title = "${Catalog.meals[meal]} · ${day.dayOfWeek.getDisplayName(TextStyle.FULL, Text.greek)}",
            onPick = { vm.add(day, meal, it.recipe.id); picking = null },
            onDismiss = { picking = null },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Καθαρισμός εβδομάδας;") },
            text = { Text("Θα αφαιρεθούν όλα τα γεύματα αυτής της εβδομάδας από το πρόγραμμα.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearWeek() }) { Text("Καθαρισμός") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Ακύρωση") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayCard(
    day: LocalDate,
    isToday: Boolean,
    entries: List<PlanRow>,
    onAdd: (Int) -> Unit,
    onRemove: (PlanRow) -> Unit,
    onOpen: (PlanRow) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            val name = day.dayOfWeek.getDisplayName(TextStyle.FULL, Text.greek).replaceFirstChar { it.titlecase(Text.greek) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    day.format(DateTimeFormatter.ofPattern("d/M")), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isToday) {
                    Spacer(Modifier.width(8.dp))
                    Text("Σήμερα", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Catalog.meals.forEachIndexed { meal, label ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(104.dp))
                    FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.Center) {
                        entries.filter { it.meal == meal }.forEach { e ->
                            InputChip(
                                selected = false,
                                onClick = { onOpen(e) },
                                label = { Text(e.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp)) },
                                trailingIcon = {
                                    IconButton(onClick = { onRemove(e) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Filled.Close, "Αφαίρεση", Modifier.size(16.dp))
                                    }
                                },
                            )
                        }
                        IconButton(onClick = { onAdd(meal) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Add, "Προσθήκη στο $label")
                        }
                    }
                }
            }
        }
    }
}
