package com.recipeapp.syntages.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recipeapp.syntages.RecipeApplication
import com.recipeapp.syntages.data.BackupManager
import com.recipeapp.syntages.data.SampleData
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as RecipeApplication).container
    val backup = remember { BackupManager(context.applicationContext, container.database) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Pair<String, String>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            message = runCatching { backup.export(uri) }.fold(
                { "Έτοιμο!" to "Αποθηκεύτηκαν $it συνταγές στο αρχείο. Κράτησέ το σε ασφαλές μέρος (π.χ. Google Drive ή υπολογιστή)." },
                { "Σφάλμα" to "Η εξαγωγή απέτυχε: ${it.message}" },
            )
            busy = false
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            message = runCatching { backup.import(uri) }.fold(
                { r ->
                    "Η εισαγωγή ολοκληρώθηκε" to buildString {
                        append("Νέες συνταγές: ${r.recipesAdded}")
                        if (r.recipesSkipped > 0) append("\nΥπήρχαν ήδη: ${r.recipesSkipped}")
                        if (r.planAdded > 0) append("\nΓεύματα προγράμματος: ${r.planAdded}")
                        if (r.shoppingAdded > 0) append("\nΕίδη λίστας αγορών: ${r.shoppingAdded}")
                    }
                },
                { "Σφάλμα" to "Δεν ήταν δυνατή η εισαγωγή. Βεβαιώσου ότι διάλεξες αρχείο αντιγράφου ασφαλείας της εφαρμογής.\n(${it.message})" },
            )
            busy = false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Ρυθμίσεις") }) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Αντίγραφο ασφαλείας", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Οι συνταγές αποθηκεύονται μόνο σε αυτό το κινητό. Κάνε εξαγωγή σε αρχείο για να μη τις χάσεις " +
                                "αν αλλάξεις κινητό. Στο νέο κινητό, εγκατέστησε την εφαρμογή και πάτα «Εισαγωγή».",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = { exportLauncher.launch("syntages-${LocalDate.now()}.zip") },
                            enabled = !busy, modifier = Modifier.fillMaxWidth(),
                        ) { Icon(Icons.Filled.Upload, null); Spacer(Modifier.width(8.dp)); Text("Εξαγωγή σε αρχείο") }
                        FilledTonalButton(
                            onClick = { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                            enabled = !busy, modifier = Modifier.fillMaxWidth(),
                        ) { Icon(Icons.Filled.Download, null); Spacer(Modifier.width(8.dp)); Text("Εισαγωγή από αρχείο") }
                        Text(
                            "Η εισαγωγή προσθέτει μόνο τις συνταγές που δεν υπάρχουν ήδη — δεν σβήνει τίποτα.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (busy) Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Παρακαλώ περίμενε…")
                        }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Παραδείγματα", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Πρόσθεσε 3 έτοιμες συνταγές για να δοκιμάσεις την εφαρμογή.", style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = {
                            scope.launch { SampleData.insert(container.repository); message = "Έτοιμο!" to "Προστέθηκαν 3 συνταγές." }
                        }, enabled = !busy) { Text("Πρόσθεσε παραδείγματα") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Οι Συνταγές μου · έκδοση 1.2\nΔουλεύει χωρίς internet (εκτός από την εισαγωγή από link). Όλα τα δεδομένα μένουν στη συσκευή σου.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    message?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("Εντάξει") } },
        )
    }
}
