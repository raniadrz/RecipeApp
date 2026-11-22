package com.recipeapp.syntages.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.recipeapp.syntages.util.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupRecipe(val recipe: Recipe, val ingredients: List<Ingredient>)

@Serializable
data class BackupPlan(val epochDay: Long, val meal: Int, val recipeUuid: String)

@Serializable
data class BackupFile(
    val app: String = "syntages",
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val recipes: List<BackupRecipe> = emptyList(),
    val shopping: List<ShoppingItem> = emptyList(),
    val plan: List<BackupPlan> = emptyList(),
)

data class ImportResult(val recipesAdded: Int, val recipesSkipped: Int, val planAdded: Int, val shoppingAdded: Int)

/**
 * Αντίγραφο ασφαλείας: ένα αρχείο .zip με το recipes.json και τις φωτογραφίες.
 * Η εισαγωγή προσθέτει μόνο συνταγές που δεν υπάρχουν ήδη (δεν σβήνει τίποτα).
 */
class BackupManager(private val context: Context, private val db: AppDatabase) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private val safeName = Regex("^[A-Za-z0-9_.-]{1,100}$")

    suspend fun export(target: Uri): Int = withContext(Dispatchers.IO) {
        val recipes = db.recipeDao().getAll()
        val uuidById = recipes.associate { it.recipe.id to it.recipe.uuid }
        val backup = BackupFile(
            recipes = recipes.map { BackupRecipe(it.recipe, it.ingredients) },
            shopping = db.shoppingDao().getAll(),
            plan = db.mealPlanDao().getAll().mapNotNull { e ->
                uuidById[e.recipeId]?.let { BackupPlan(e.epochDay, e.meal, it) }
            },
        )
        val out = context.contentResolver.openOutputStream(target, "wt") ?: error("Δεν ήταν δυνατό να ανοίξει το αρχείο")
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("recipes.json"))
            zip.write(json.encodeToString(backup).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            recipes.mapNotNull { it.recipe.photoPath }.distinct().forEach { name ->
                val f = PhotoStore.file(context, name)
                if (f.exists()) {
                    zip.putNextEntry(ZipEntry("photos/$name"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        recipes.size
    }

    suspend fun import(source: Uri): ImportResult = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "import_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            var jsonText: String? = null
            val input = context.contentResolver.openInputStream(source) ?: error("Δεν ήταν δυνατό να ανοίξει το αρχείο")
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when {
                        entry.isDirectory -> Unit
                        entry.name == "recipes.json" -> jsonText = zip.readBytes().toString(Charsets.UTF_8)
                        entry.name.startsWith("photos/") -> {
                            val name = entry.name.removePrefix("photos/")
                            if (safeName.matches(name)) File(tmp, name).outputStream().use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                }
            }
            val text = jsonText ?: error("Το αρχείο δεν είναι αντίγραφο ασφαλείας της εφαρμογής")
            val backup = json.decodeFromString<BackupFile>(text)

            db.withTransaction {
                val recipeDao = db.recipeDao()
                val existing = recipeDao.allUuids().toSet()
                var added = 0
                var skipped = 0
                for (br in backup.recipes) {
                    if (br.recipe.uuid in existing) { skipped++; continue }
                    var photo = br.recipe.photoPath
                    if (photo != null) {
                        val src = File(tmp, photo)
                        if (safeName.matches(photo) && src.exists()) {
                            val dst = PhotoStore.file(context, photo)
                            if (!dst.exists()) src.copyTo(dst)
                        } else photo = null
                    }
                    recipeDao.save(br.recipe.copy(id = 0, photoPath = photo), br.ingredients)
                    added++
                }
                var planAdded = 0
                val planDao = db.mealPlanDao()
                val currentPlan = planDao.getAll().map { Triple(it.epochDay, it.meal, it.recipeId) }.toMutableSet()
                for (p in backup.plan) {
                    val rid = recipeDao.idForUuid(p.recipeUuid) ?: continue
                    if (currentPlan.add(Triple(p.epochDay, p.meal, rid))) {
                        planDao.insert(MealPlanEntry(epochDay = p.epochDay, meal = p.meal, recipeId = rid)); planAdded++
                    }
                }
                var shoppingAdded = 0
                val shoppingDao = db.shoppingDao()
                if (shoppingDao.getAll().isEmpty() && backup.shopping.isNotEmpty()) {
                    shoppingDao.insertAll(backup.shopping.map { it.copy(id = 0) })
                    shoppingAdded = backup.shopping.size
                }
                ImportResult(added, skipped, planAdded, shoppingAdded)
            }
        } finally {
            tmp.deleteRecursively()
        }
    }
}
