package com.recipeapp.syntages.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Συνταγή όπως διαβάστηκε από μια ιστοσελίδα. */
data class WebRecipe(
    val title: String,
    val imageUrl: String?,
    val ingredients: List<String>,
    val steps: List<String>,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val servings: Int?,
    val category: String?,
    val tags: List<String>,
    val sourceUrl: String,
) {
    val isComplete get() = ingredients.isNotEmpty() || steps.isNotEmpty()
}

/**
 * Διαβάζει τη συνταγή από το HTML μιας σελίδας. Τα περισσότερα σάιτ συνταγών έχουν μέσα
 * στη σελίδα τη συνταγή σε μορφή schema.org/Recipe (JSON-LD) για τη Google — αυτή χρησιμοποιούμε.
 */
object WebRecipeParser {
    private val ldRegex = Regex(
        "<script[^>]*type\\s*=\\s*[\"']?application/ld\\+json[\"']?[^>]*>(.*?)</script>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(html: String, url: String): WebRecipe? {
        for (m in ldRegex.findAll(html)) {
            val raw = m.groupValues[1].trim().removePrefix("<!--").removeSuffix("-->").trim()
            val el = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: continue
            val obj = findRecipe(el, 0) ?: continue
            return fromJsonLd(obj, url)
        }
        // Χωρίς δομημένα δεδομένα: κρατάμε τουλάχιστον τίτλο και φωτογραφία.
        val title = meta(html, "og:title")
            ?: Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)?.groupValues?.get(1)
        val t = title?.let { clean(it) }?.takeIf { it.isNotBlank() } ?: return null
        return WebRecipe(t, meta(html, "og:image")?.let { decodeEntities(it) }, emptyList(), emptyList(), 0, 0, null, null, emptyList(), url)
    }

    private fun meta(html: String, prop: String): String? {
        val a = Regex("<meta[^>]+(?:property|name)\\s*=\\s*[\"']$prop[\"'][^>]*content\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        val b = Regex("<meta[^>]+content\\s*=\\s*[\"']([^\"']*)[\"'][^>]*(?:property|name)\\s*=\\s*[\"']$prop[\"']", RegexOption.IGNORE_CASE)
        return (a.find(html) ?: b.find(html))?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }

    private fun isRecipe(o: JsonObject): Boolean = when (val t = o["@type"]) {
        is JsonPrimitive -> t.content.equals("Recipe", true)
        is JsonArray -> t.any { it is JsonPrimitive && it.content.equals("Recipe", true) }
        else -> false
    }

    private fun findRecipe(el: JsonElement, depth: Int): JsonObject? {
        if (depth > 6) return null
        return when (el) {
            is JsonArray -> el.firstNotNullOfOrNull { findRecipe(it, depth + 1) }
            is JsonObject -> if (isRecipe(el)) el else
                listOf("@graph", "mainEntity", "mainEntityOfPage", "itemListElement", "item")
                    .firstNotNullOfOrNull { k -> el[k]?.let { findRecipe(it, depth + 1) } }
            else -> null
        }
    }

    private fun str(el: JsonElement?): String? = when (el) {
        null, JsonNull -> null
        is JsonPrimitive -> el.content
        is JsonArray -> el.firstNotNullOfOrNull { str(it) }
        is JsonObject -> str(el["text"]) ?: str(el["name"]) ?: str(el["url"]) ?: str(el["@id"])
    }

    private fun strings(el: JsonElement?): List<String> = when (el) {
        null, JsonNull -> emptyList()
        is JsonPrimitive -> listOf(el.content)
        is JsonArray -> el.flatMap { strings(it) }
        is JsonObject -> listOfNotNull(str(el))
    }

    private fun fromJsonLd(o: JsonObject, url: String): WebRecipe {
        val ingredients = strings(o["recipeIngredient"] ?: o["ingredients"])
            .flatMap { splitLines(it) }
            .filter { it.isNotBlank() }
        val steps = instructions(o["recipeInstructions"])
        val keywords = strings(o["keywords"]).flatMap { it.split(',') } + strings(o["suitableForDiet"]) + strings(o["recipeCategory"])
        val image = when (val img = o["image"]) {
            is JsonObject -> str(img["url"]) ?: str(img["contentUrl"])
            is JsonArray -> img.firstNotNullOfOrNull { if (it is JsonObject) str(it["url"]) else str(it) }
            else -> str(img)
        }
        var prep = minutes(str(o["prepTime"]))
        var cook = minutes(str(o["cookTime"]))
        val total = minutes(str(o["totalTime"]))
        if (prep == 0 && cook == 0 && total > 0) cook = total
        if (prep == 0 && total > cook) prep = total - cook
        return WebRecipe(
            title = clean(str(o["name"]) ?: str(o["headline"]) ?: ""),
            imageUrl = image?.let { decodeEntities(it).trim() }?.takeIf { it.startsWith("http") },
            ingredients = ingredients,
            steps = steps,
            prepMinutes = prep,
            cookMinutes = cook,
            servings = servings(o["recipeYield"] ?: o["yield"]),
            category = mapCategory(strings(o["recipeCategory"]).joinToString(" ") + " " + (str(o["name"]) ?: "")),
            tags = mapTags(keywords.joinToString(" ")),
            sourceUrl = url,
        )
    }

    private fun instructions(el: JsonElement?): List<String> = when (el) {
        null, JsonNull -> emptyList()
        is JsonPrimitive -> splitLines(el.content)
        is JsonArray -> el.flatMap { instructions(it) }
        is JsonObject -> {
            val list = el["itemListElement"]
            if (list != null) instructions(list)
            else splitLines(str(el["text"]) ?: str(el["name"]) ?: "")
        }
    }.filter { it.isNotBlank() }

    private fun splitLines(s: String): List<String> =
        clean(s).split('\n').map { it.replace(Regex("^\\s*(\\d+[.)]|[•\\-*])\\s+"), "").trim() }.filter { it.isNotEmpty() }

    private val duration = Regex("^P(?:(\\d+)D)?(?:T(?:(\\d+(?:\\.\\d+)?)H)?(?:(\\d+(?:\\.\\d+)?)M)?(?:\\d+(?:\\.\\d+)?S)?)?$", RegexOption.IGNORE_CASE)

    /** «PT1H30M» → 90 */
    fun minutes(iso: String?): Int {
        val m = iso?.trim()?.let { duration.find(it) } ?: return 0
        val d = m.groupValues[1].toDoubleOrNull() ?: 0.0
        val h = m.groupValues[2].toDoubleOrNull() ?: 0.0
        val min = m.groupValues[3].toDoubleOrNull() ?: 0.0
        return (d * 1440 + h * 60 + min).toInt().coerceIn(0, 10_000)
    }

    private fun servings(el: JsonElement?): Int? {
        val all = strings(el)
        for (s in all) Regex("\\d+").find(s)?.value?.toIntOrNull()?.takeIf { it in 1..100 }?.let { return it }
        return null
    }

    private val categoryRules = listOf(
        "Σαλάτες" to listOf("σαλατ", "salad"),
        "Σούπες" to listOf("σουπ", "soup"),
        "Γλυκά" to listOf("γλυκ", "επιδορπ", "κεικ", "τουρτ", "μπισκοτ", "dessert", "cake", "cookie"),
        "Ζυμαρικά" to listOf("ζυμαρικ", "μακαρον", "σπαγγετ", "pasta"),
        "Όσπρια" to listOf("οσπρ", "φακεσ", "φασολαδ", "ρεβιθ", "φαβα"),
        "Λαδερά" to listOf("λαδερ"),
        "Ψάρια & Θαλασσινά" to listOf("ψαρ", "θαλασσιν", "fish", "seafood"),
        "Πίτες" to listOf("πιτα", "πιτεσ", "pie"),
        "Ψωμί & Ζύμες" to listOf("ψωμ", "ζυμεσ", "bread"),
        "Πρωινό" to listOf("πρωιν", "breakfast"),
        "Ροφήματα" to listOf("ροφημ", "smoothie", "drink"),
        "Ορεκτικά" to listOf("ορεκτικ", "μεζ", "appetizer", "starter"),
        "Κυρίως πιάτα" to listOf("κυριω", "main"),
    )

    fun mapCategory(text: String): String? {
        val n = Text.normalize(text)
        return categoryRules.firstOrNull { (_, keys) -> keys.any { n.contains(it) } }?.first
    }

    fun mapTags(text: String): List<String> {
        val n = Text.normalize(text)
        val tags = mutableListOf<String>()
        if (n.contains("νηστισ")) tags += "Νηστίσιμο"
        if (listOf("χορτοφαγ", "vegan", "vegetarian").any { n.contains(it) }) tags += "Χορτοφαγικό"
        if (listOf("χωρισ γλουτενη", "glutenfree", "gluten free", "gluten-free").any { n.contains(it) }) tags += "Χωρίς γλουτένη"
        if (listOf("παιδ", "kids").any { n.contains(it) }) tags += "Παιδικό"
        return tags
    }

    /** Αφαιρεί HTML και αποκωδικοποιεί entities. Κρατά τις αλλαγές γραμμής. */
    fun clean(s: String): String {
        val withBreaks = s
            .replace(Regex("<\\s*br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</\\s*(p|li|div|h\\d)\\s*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
        return decodeEntities(withBreaks)
            .replace('\u00A0', ' ')
            .split('\n')
            .joinToString("\n") { it.replace(Regex("[ \\t\\r]+"), " ").trim() }
            .trim()
    }

    private val named = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "frac12" to "½", "frac14" to "¼", "frac34" to "¾", "deg" to "°", "rsquo" to "’", "lsquo" to "‘",
        "rdquo" to "”", "ldquo" to "“", "laquo" to "«", "raquo" to "»", "ndash" to "–", "mdash" to "—", "hellip" to "…",
    )

    fun decodeEntities(s: String): String {
        var out = s
        repeat(2) { // μερικά σάιτ κάνουν διπλή κωδικοποίηση (&amp;amp;)
            out = Regex("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+\\d*);").replace(out) { m ->
                val code = m.groupValues[1]
                when {
                    code.startsWith("#x") || code.startsWith("#X") -> code.substring(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
                    code.startsWith("#") -> code.substring(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
                    else -> named[code.lowercase()] ?: m.value
                }
            }
        }
        return out
    }
}
