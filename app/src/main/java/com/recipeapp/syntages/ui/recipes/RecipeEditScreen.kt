package com.recipeapp.syntages.ui.recipes

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.recipeapp.syntages.domain.Catalog
import com.recipeapp.syntages.domain.Sections
import com.recipeapp.syntages.domain.Units
import com.recipeapp.syntages.ui.appViewModel
import com.recipeapp.syntages.ui.components.RecipePhoto
import com.recipeapp.syntages.util.PhotoStore
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeEditScreen(recipeId: Long, importUrl: String? = null, onDone: (savedId: Long?) -> Unit) {
    val vm = appViewModel(key = "edit_$recipeId") { RecipeEditViewModel(it.app, it.repository, recipeId) }
    val context = LocalContext.current
    var linkDialog by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(importUrl) {
        if (!importUrl.isNullOrBlank() && !vm.autoImportDone) {
            vm.autoImportDone = true
            vm.importFromUrl(importUrl)
        }
    }
    var confirmExit by remember { mutableStateOf(false) }
    var pasteIngredients by remember { mutableStateOf(false) }
    var pasteSteps by remember { mutableStateOf(false) }
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = cameraFile
        if (path != null) {
            val f = File(path)
            if (ok && f.exists() && f.length() > 0) vm.setPhotoFromUri(Uri.fromFile(f), deleteSource = f) else f.delete()
        }
        cameraFile = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setPhotoFromUri(uri)
    }

    BackHandler { confirmExit = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recipeId == 0L) "Νέα συνταγή" else "Επεξεργασία") },
                navigationIcon = { IconButton(onClick = { confirmExit = true }) { Icon(Icons.Filled.Close, "Κλείσιμο") } },
                actions = {
                    Button(onClick = { vm.save { onDone(it) } }, enabled = vm.loaded, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Αποθήκευση")
                    }
                },
            )
        },
    ) { padding ->
        if (!vm.loaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize().imePadding()) {
            val wide = maxWidth >= 720.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (recipeId == 0L) ImportCard(vm) { linkDialog = true }
                // ---- Βασικά στοιχεία + φωτογραφία ----
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(0.4f)) {
                            PhotoPicker(vm, onCamera = {
                                val (f, uri) = PhotoStore.newCameraTarget(context)
                                cameraFile = f.absolutePath
                                camera.launch(uri)
                            }, onGallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                        }
                        Column(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(12.dp)) { BasicFields(vm) }
                    }
                } else {
                    PhotoPicker(vm, onCamera = {
                        val (f, uri) = PhotoStore.newCameraTarget(context)
                        cameraFile = f.absolutePath
                        camera.launch(uri)
                    }, onGallery = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    BasicFields(vm)
                }

                Label("Ετικέτες")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Catalog.tags.forEach { t ->
                        FilterChip(selected = t in vm.tags, onClick = { vm.toggleTag(t) }, label = { Text(t) })
                    }
                }

                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            IngredientsEditor(vm) { pasteIngredients = true }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StepsEditor(vm) { pasteSteps = true }
                        }
                    }
                } else {
                    IngredientsEditor(vm) { pasteIngredients = true }
                    StepsEditor(vm) { pasteSteps = true }
                }
                Spacer(Modifier.size(80.dp))
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Έξοδος χωρίς αποθήκευση;") },
            text = { Text("Οι αλλαγές που έκανες θα χαθούν.") },
            confirmButton = { TextButton(onClick = { confirmExit = false; onDone(null) }) { Text("Έξοδος") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Συνέχεια επεξεργασίας") } },
        )
    }
    if (linkDialog) {
        LinkDialog(
            onDismiss = { linkDialog = false },
            onImport = { linkDialog = false; vm.importFromUrl(it) },
        )
    }
    vm.importMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.importMessage = null },
            title = { Text("Εισαγωγή από internet") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { vm.importMessage = null }) { Text("Εντάξει") } },
        )
    }
    if (pasteIngredients) {
        PasteDialog(
            title = "Γρήγορη προσθήκη υλικών",
            hint = "Ένα υλικό σε κάθε γραμμή, π.χ.\n500 γρ. κιμάς\n2 κρεμμύδια\n1 κ.σ. ζάχαρη\nαλάτι",
            onDismiss = { pasteIngredients = false },
            onConfirm = { vm.addParsedIngredients(it); pasteIngredients = false },
        )
    }
    if (pasteSteps) {
        PasteDialog(
            title = "Γρήγορη προσθήκη βημάτων",
            hint = "Ένα βήμα σε κάθε γραμμή",
            onDismiss = { pasteSteps = false },
            onConfirm = { vm.addParsedSteps(it); pasteSteps = false },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun PhotoPicker(vm: RecipeEditViewModel, onCamera: () -> Unit, onGallery: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp))) {
            RecipePhoto(vm.photo, Modifier.fillMaxSize(), iconSize = 56.dp)
            if (vm.photoBusy) CircularProgressIndicator(Modifier.align(Alignment.Center))
            if (vm.photo != null) {
                IconButton(onClick = vm::removePhoto, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Delete, "Αφαίρεση φωτογραφίας", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onCamera, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Κάμερα")
            }
            FilledTonalButton(onClick = onGallery, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("Συλλογή")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicFields(vm: RecipeEditViewModel) {
    OutlinedTextField(
        value = vm.title,
        onValueChange = { vm.title = it; vm.titleError = false },
        label = { Text("Τίτλος συνταγής *") },
        isError = vm.titleError,
        supportingText = if (vm.titleError) { { Text("Γράψε έναν τίτλο") } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    Dropdown(
        label = "Κατηγορία",
        value = vm.category,
        options = Catalog.categories,
        display = { it },
        onSelect = { vm.category = it },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("Προετοιμ. (λεπτά)", vm.prep, { vm.prep = it }, Modifier.weight(1f))
        NumberField("Μαγείρ. (λεπτά)", vm.cook, { vm.cook = it }, Modifier.weight(1f))
        NumberField("Μερίδες", vm.servings, { vm.servings = it }, Modifier.weight(0.7f))
    }
    Text("Δυσκολία", style = MaterialTheme.typography.labelLarge)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Catalog.difficulties.forEachIndexed { i, d ->
            SegmentedButton(
                selected = vm.difficulty == i,
                onClick = { vm.difficulty = i },
                shape = SegmentedButtonDefaults.itemShape(i, Catalog.difficulties.size),
            ) { Text(d) }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(4)) },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
fun <T> Dropdown(label: String, value: T, options: List<T>, display: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(display(value), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(display(o)) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}

@Composable
private fun SmallDropdown(value: String, options: List<String>, display: (String) -> String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { open = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
            Text(display(value), maxLines = 1, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(display(o)) }, onClick = { onSelect(o); open = false }) }
        }
    }
}

@Composable
private fun IngredientsEditor(vm: RecipeEditViewModel, onPaste: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Label("Υλικά")
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onPaste) { Icon(Icons.Filled.ContentPaste, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Επικόλληση") }
    }
    vm.ingredients.forEach { d ->
        androidx.compose.runtime.key(d.key) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = d.qty,
                            onValueChange = { d.qty = it.take(8) },
                            label = { Text("Ποσ.") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.widthIn(max = 84.dp),
                        )
                        OutlinedTextField(
                            value = d.name,
                            onValueChange = { d.updateName(it) },
                            label = { Text("Υλικό") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { vm.removeIngredient(d) }) { Icon(Icons.Filled.Delete, "Διαγραφή υλικού") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val unitOptions = if (d.unit in Units.choices) Units.choices else Units.choices + d.unit
                        SmallDropdown(d.unit, unitOptions, { Units.label(it) }, { d.unit = it })
                        SmallDropdown(
                            d.section.ifBlank { Sections.OTHER }, Sections.all, { it },
                            { d.section = it; d.sectionManual = true }, Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
    }
    OutlinedButton(onClick = vm::addIngredient, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Προσθήκη υλικού")
    }
}

@Composable
private fun StepsEditor(vm: RecipeEditViewModel, onPaste: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Label("Εκτέλεση")
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onPaste) { Icon(Icons.Filled.ContentPaste, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Επικόλληση") }
    }
    vm.steps.forEachIndexed { i, s ->
        androidx.compose.runtime.key(s.key) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${i + 1}.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, end = 6.dp).widthIn(min = 24.dp),
                )
                OutlinedTextField(
                    value = s.text,
                    onValueChange = { s.text = it },
                    placeholder = { Text("Περιγραφή βήματος") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 2,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                )
                Column {
                    IconButton(onClick = { vm.moveStep(s, -1) }, enabled = i > 0, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, "Πάνω")
                    }
                    IconButton(onClick = { vm.moveStep(s, 1) }, enabled = i < vm.steps.lastIndex, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, "Κάτω")
                    }
                    IconButton(onClick = { vm.removeStep(s) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, "Διαγραφή βήματος", Modifier.size(20.dp))
                    }
                }
            }
        }
    }
    OutlinedButton(onClick = vm::addStep, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Προσθήκη βήματος")
    }
}

@Composable
private fun PasteDialog(title: String, hint: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text(hint) },
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Προσθήκη") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ακύρωση") } },
    )
}

@Composable
private fun ImportCard(vm: RecipeEditViewModel, onLink: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Βρήκες τη συνταγή στο internet;", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Βάλε το link και θα συμπληρωθούν αυτόματα τίτλος, φωτογραφία, υλικά και βήματα. " +
                    "Ή, από τον browser, πάτα «Κοινοποίηση» → «Οι Συνταγές μου».",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (vm.importing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Διαβάζω τη συνταγή…")
                }
            } else {
                Button(onClick = onLink) {
                    Icon(Icons.Filled.Link, null); Spacer(Modifier.width(6.dp)); Text("Εισαγωγή από link")
                }
            }
            vm.importError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun LinkDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var url by remember {
        val clip = clipboard.getText()?.text
        mutableStateOf(com.recipeapp.syntages.util.RecipeImporter.extractUrl(clip) ?: "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Εισαγωγή από link") },
        text = {
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                label = { Text("Διεύθυνση σελίδας") },
                placeholder = { Text("https://…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onImport(url) }, enabled = url.isNotBlank()) { Text("Εισαγωγή") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ακύρωση") } },
    )
}
