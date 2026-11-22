package com.recipeapp.syntages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import com.recipeapp.syntages.data.RecipeWithIngredients
import com.recipeapp.syntages.domain.Text
import com.recipeapp.syntages.util.PhotoStore

@Composable
fun RatingBar(rating: Int, onRate: ((Int) -> Unit)?, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    Row(modifier) {
        for (i in 1..5) {
            val filled = i <= rating
            val icon = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline
            val tint = if (filled) Color(0xFFF2A900) else MaterialTheme.colorScheme.outline
            if (onRate != null) {
                IconButton(onClick = { onRate(if (rating == i) 0 else i) }, modifier = Modifier.size(size + 12.dp)) {
                    Icon(icon, contentDescription = "$i αστέρια", tint = tint, modifier = Modifier.size(size))
                }
            } else {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
            }
        }
    }
}

@Composable
fun RecipePhoto(photoPath: String?, modifier: Modifier = Modifier, iconSize: Dp = 48.dp) {
    val context = LocalContext.current
    if (photoPath != null) {
        AsyncImage(
            model = PhotoStore.file(context, photoPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.RestaurantMenu, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
        Text(
            text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        action()
    }
}

/** Διάλογος επιλογής συνταγής με αναζήτηση. */
@Composable
fun RecipePickerDialog(recipes: List<RecipeWithIngredients>, title: String, onPick: (RecipeWithIngredients) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = Text.normalize(query)
    val filtered = recipes.filter { q.isBlank() || Text.normalize(it.recipe.title).contains(q) || Text.normalize(it.recipe.category).contains(q) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    placeholder = { Text("Αναζήτηση…") }, modifier = Modifier.fillMaxWidth(),
                )
                if (recipes.isEmpty()) {
                    Text("Δεν υπάρχουν ακόμη συνταγές.", modifier = Modifier.padding(top = 16.dp))
                }
                LazyColumn(Modifier.heightIn(max = 400.dp).padding(top = 8.dp)) {
                    items(filtered, key = { it.recipe.id }) { r ->
                        ListItem(
                            headlineContent = { Text(r.recipe.title) },
                            supportingContent = { Text(r.recipe.category) },
                            leadingContent = {
                                RecipePhoto(r.recipe.photoPath, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), iconSize = 20.dp)
                            },
                            modifier = Modifier.clickable { onPick(r) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ακύρωση") } },
    )
}
