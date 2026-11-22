package com.recipeapp.syntages.domain

import java.text.Normalizer
import java.util.Locale

object Text {
    val greek: Locale = Locale("el", "GR")
    private val marks = "\\p{M}+".toRegex()
    private val spaces = "\\s+".toRegex()

    /** Πεζά, χωρίς τόνους/διαλυτικά, ς→σ, χωρίς διπλά κενά. */
    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(greek), Normalizer.Form.NFD)
            .replace(marks, "")
            .replace('ς', 'σ')
            .replace(spaces, " ")
            .trim()

    private val stopPrefixes = listOf("μεγαλ", "μικρ", "μετρι", "ψιλοκομμ", "κομμεν", "τριμμεν", "ωριμ", "περιπου")
    private val endings = listOf("ιεσ", "ια", "εσ", "οι", "οσ", "ου", "ων", "ασ", "α", "ε", "η", "ι", "ο", "υ", "σ")

    private fun stem(word: String): String {
        if (word == "πιπερι") return "πιπερι#" // μαύρο πιπέρι ≠ πιπεριά
        for (e in endings) {
            if (word.endsWith(e) && word.length - e.length >= 3) return word.dropLast(e.length)
        }
        return word
    }

    /** Κλειδί σύγκρισης ώστε «κρεμμύδι» και «Κρεμμύδια» να θεωρούνται ίδιο υλικό. */
    fun matchKey(name: String): String {
        val words = normalize(name).split(' ').filter { it.isNotBlank() }
        val kept = words.filter { w -> stopPrefixes.none { w.startsWith(it) } }.ifEmpty { words }
        return kept.joinToString(" ") { stem(it) }
    }

    fun formatQuantity(q: Double?): String {
        if (q == null) return ""
        val rounded = Math.round(q * 100) / 100.0
        return if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else String.format(greek, "%.2f", rounded).trimEnd('0').trimEnd(',', '.')
    }

    private val fractionChars = mapOf('½' to 0.5, '¼' to 0.25, '¾' to 0.75, '⅓' to 1.0 / 3, '⅔' to 2.0 / 3)

    /** Δέχεται «2», «1,5», «1.5», «1/2», «1 1/2», «½». */
    fun parseQuantity(input: String): Double? {
        val s = input.trim()
        if (s.isEmpty()) return null
        if (s.length == 1 && s[0] in fractionChars) return fractionChars[s[0]]
        val mixed = Regex("^(\\d+)\\s+(\\d+)\\s*/\\s*(\\d+)$").find(s)
        if (mixed != null) {
            val (w, n, d) = mixed.destructured
            return if (d.toDouble() == 0.0) null else w.toDouble() + n.toDouble() / d.toDouble()
        }
        val frac = Regex("^(\\d+)\\s*/\\s*(\\d+)$").find(s)
        if (frac != null) {
            val (n, d) = frac.destructured
            return if (d.toDouble() == 0.0) null else n.toDouble() / d.toDouble()
        }
        val withFrac = Regex("^(\\d+)\\s*([½¼¾⅓⅔])$").find(s)
        if (withFrac != null) return withFrac.groupValues[1].toDouble() + fractionChars.getValue(withFrac.groupValues[2][0])
        return s.replace(',', '.').toDoubleOrNull()
    }
}
