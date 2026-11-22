package com.recipeapp.syntages.domain

object Units {
    /** "" σημαίνει τεμάχια (π.χ. «3 κρεμμύδια»). */
    val choices = listOf("", "γρ.", "κιλό", "ml", "λίτρο", "κ.σ.", "κ.γ.", "φλιτζ.", "σκελίδα", "ματσάκι", "πρέζα", "φέτα", "κουτί", "συσκ.")

    fun label(unit: String) = if (unit.isEmpty()) "τεμ." else unit

    private val aliases: Map<String, String> = buildMap {
        listOf("τεμ", "τεμ.", "τεμαχια", "τεμαχιο", "τμχ", "τμχ.", "pcs").forEach { put(it, "") }
        listOf("g", "gr", "gr.", "γρ", "γρ.", "γραμμαρια", "γραμμαριο", "γραμ.", "γραμμ.").forEach { put(it, "γρ.") }
        listOf("kg", "kg.", "κιλο", "κιλα", "κ.").forEach { put(it, "κιλό") }
        listOf("ml", "ml.", "μλ", "μλ.", "mL").forEach { put(it, "ml") }
        listOf("l", "lt", "lt.", "λιτρο", "λιτρα", "λιτ.", "λ.").forEach { put(it, "λίτρο") }
        listOf("κσ", "κ.σ.", "κ.σ", "κουταλια", "κουταλιεσ").forEach { put(it, "κ.σ.") }
        listOf("κγ", "κ.γ.", "κ.γ", "κουταλακι", "κουταλακια").forEach { put(it, "κ.γ.") }
        listOf("φλιτζανι", "φλιτζανια", "φλ.", "φλιτζ.", "φλιτζ").forEach { put(it, "φλιτζ.") }
        listOf("σκελιδα", "σκελιδεσ").forEach { put(it, "σκελίδα") }
        listOf("ματσακι", "ματσακια", "ματσο").forEach { put(it, "ματσάκι") }
        listOf("πρεζα", "πρεζεσ").forEach { put(it, "πρέζα") }
        listOf("φετεσ").forEach { put(it, "φέτα") }
        listOf("κουτι", "κουτια", "κονσερβα", "κονσερβεσ").forEach { put(it, "κουτί") }
        listOf("συσκ.", "συσκ", "συσκευασια", "συσκευασιεσ", "πακετο", "πακετα").forEach { put(it, "συσκ.") }
    }

    /** Επιστρέφει την κανονική μονάδα ή null αν η λέξη δεν είναι μονάδα. */
    fun canonical(token: String): String? {
        if (token in choices && token.isNotEmpty()) return token
        return aliases[Text.normalize(token)]
    }

    fun normalizeUnit(unit: String): String = canonical(unit) ?: unit.trim()

    /** Μετατροπή σε βασική μονάδα για άθροιση (κιλό→γρ., λίτρο→ml). */
    fun toBase(unit: String, quantity: Double?): Pair<String, Double?> {
        val u = normalizeUnit(unit)
        return when (u) {
            "κιλό" -> "γρ." to quantity?.times(1000)
            "λίτρο" -> "ml" to quantity?.times(1000)
            else -> u to quantity
        }
    }

    /** «1500 γρ.» → «1,5 κιλά», «3» → «3». */
    fun display(quantity: Double?, unit: String): String {
        val u = normalizeUnit(unit)
        if (quantity == null) return if (u.isEmpty()) "" else u
        return when {
            u == "γρ." && quantity >= 1000 -> {
                val k = quantity / 1000; "${Text.formatQuantity(k)} ${if (k == 1.0) "κιλό" else "κιλά"}"
            }
            u == "ml" && quantity >= 1000 -> {
                val l = quantity / 1000; "${Text.formatQuantity(l)} ${if (l == 1.0) "λίτρο" else "λίτρα"}"
            }
            u == "κιλό" && quantity != 1.0 -> "${Text.formatQuantity(quantity)} κιλά"
            u == "λίτρο" && quantity != 1.0 -> "${Text.formatQuantity(quantity)} λίτρα"
            u.isEmpty() -> Text.formatQuantity(quantity)
            else -> "${Text.formatQuantity(quantity)} $u"
        }
    }
}
