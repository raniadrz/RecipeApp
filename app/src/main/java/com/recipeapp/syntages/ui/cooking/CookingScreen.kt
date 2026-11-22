package com.recipeapp.syntages.ui.cooking

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recipeapp.syntages.data.RecipeWithIngredients
import com.recipeapp.syntages.domain.Units
import com.recipeapp.syntages.ui.appViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CookingScreen(recipeId: Long, onClose: () -> Unit) {
    val vm = appViewModel(key = "cook_$recipeId") { CookingViewModel(it.app, it.repository, recipeId) }
    val recipe by vm.recipe.collectAsStateWithLifecycle()

    // Η οθόνη μένει ανοιχτή όσο είσαι σε λειτουργία μαγειρέματος.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val r = recipe
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(r?.recipe?.title ?: "", maxLines = 1) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Έξοδος") } },
            )
        },
    ) { padding ->
        if (r == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val steps = r.recipe.steps
        val pageCount = steps.size + 1 // σελίδα 0 = υλικά
        val pager = rememberPagerState(pageCount = { pageCount })
        val scope = rememberCoroutineScope()
        val currentStep = if (pager.currentPage == 0) null else steps.getOrNull(pager.currentPage - 1)
        val suggested = currentStep?.let { CookingViewModel.detectMinutes(it) }

        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val wide = maxWidth >= 700.dp
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(0.62f).fillMaxHeight()) {
                        Pager(r, vm, pager, Modifier.weight(1f))
                        PagerControls(pager.currentPage, pageCount,
                            onPrev = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                            onNext = { if (pager.currentPage == pageCount - 1) onClose() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        )
                    }
                    Column(
                        Modifier.weight(0.38f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        TimerCard(vm, suggested, large = true)
                        if (pager.currentPage > 0) IngredientsCard(r, vm)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Pager(r, vm, pager, Modifier.weight(1f))
                    TimerCard(vm, suggested, large = false, modifier = Modifier.padding(horizontal = 12.dp))
                    PagerControls(pager.currentPage, pageCount,
                        onPrev = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                        onNext = { if (pager.currentPage == pageCount - 1) onClose() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    )
                }
            }
        }
    }

    if (vm.finished) {
        AlertDialog(
            onDismissRequest = vm::dismissFinished,
            icon = { Icon(Icons.Filled.Timer, null) },
            title = { Text("Ο χρόνος τελείωσε!") },
            text = { Text("Έλεγξε το φαγητό σου.") },
            confirmButton = { Button(onClick = vm::dismissFinished) { Text("Εντάξει") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Pager(r: RecipeWithIngredients, vm: CookingViewModel, pager: androidx.compose.foundation.pager.PagerState, modifier: Modifier) {
    val steps = r.recipe.steps
    HorizontalPager(state = pager, modifier = modifier.fillMaxWidth()) { page ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        ) {
            if (page == 0) {
                Text("Ετοίμασε τα υλικά", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Τσέκαρε ό,τι έχεις έτοιμο. Σύρε δεξιά ή πάτα «Επόμενο» για να ξεκινήσεις.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                IngredientChecklist(r, vm, large = true)
            } else {
                Text(
                    "Βήμα $page από ${steps.size}",
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    steps[page - 1],
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 36.sp),
                )
            }
        }
    }
}

@Composable
private fun IngredientChecklist(r: RecipeWithIngredients, vm: CookingViewModel, large: Boolean) {
    val checked = vm.checkedIngredients.value
    r.ingredients.forEach { ing ->
        val done = ing.id in checked
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { vm.toggleIngredient(ing.id) }.padding(vertical = if (large) 4.dp else 0.dp),
        ) {
            Checkbox(checked = done, onCheckedChange = { vm.toggleIngredient(ing.id) })
            val q = Units.display(ing.quantity, ing.unit)
            Text(
                if (q.isBlank()) ing.name else "$q  ${ing.name}",
                style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun IngredientsCard(r: RecipeWithIngredients, vm: CookingViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp)) {
            Text("Υλικά", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            IngredientChecklist(r, vm, large = false)
        }
    }
}

@Composable
private fun PagerControls(page: Int, count: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        LinearProgressIndicator(
            progress = { if (count <= 1) 1f else page / (count - 1).toFloat() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPrev, enabled = page > 0, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Προηγούμενο")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Text(if (page == count - 1) "Τέλος!" else if (page == 0) "Ξεκίνα" else "Επόμενο")
                Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerCard(vm: CookingViewModel, suggested: Int?, large: Boolean, modifier: Modifier = Modifier) {
    val totalSec = (vm.remainingMs + 999) / 1000
    val time = "%02d:%02d".format(totalSec / 60, totalSec % 60)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (vm.running) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    time,
                    fontSize = if (large) 56.sp else 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = vm::startPause, modifier = Modifier.size(52.dp)) {
                    Icon(if (vm.running) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (vm.running) "Παύση" else "Έναρξη")
                }
                IconButton(onClick = vm::reset) { Icon(Icons.Filled.Replay, "Επαναφορά") }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (suggested != null) {
                    AssistChip(onClick = { vm.setMinutes(suggested) }, label = { Text("⏱ $suggested' από το βήμα") })
                }
                TextButton(onClick = { vm.adjust(-1) }) { Text("−1'") }
                TextButton(onClick = { vm.adjust(1) }) { Text("+1'") }
                listOf(5, 10, 15, 30).forEach { m -> TextButton(onClick = { vm.setMinutes(m) }) { Text("$m'") } }
            }
        }
    }
}
