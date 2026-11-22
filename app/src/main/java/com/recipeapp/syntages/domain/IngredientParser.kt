package com.recipeapp.syntages.domain

data class ParsedIngredient(val name: String, val quantity: Double?, val unit: String)

object IngredientParser {
    private val leading = Regex("^\\s*(\\d+\\s+\\d+\\s*/\\s*\\d+|\\d+\\s*/\\s*\\d+|\\d+(?:[.,]\\d+)?\\s*[½¼¾⅓⅔]?|[½¼¾⅓⅔])\\s*(.*)$")
    private val bullets = Regex("^[\\s•\\-*–·]+")

    /** «500 γρ. κιμάς» → (κιμάς, 500, γρ.) · «2 κρεμμύδια» → (κρεμμύδια, 2, "") · «αλάτι» → (αλάτι, null, "") */
    fun parse(line: String): ParsedIngredient? {
        val clean = line.replace(bullets, "").trim()
        if (clean.isEmpty()) return null
        val m = leading.find(clean)
        if (m == null) return ParsedIngredient(capitalizeNone(clean), null, "")
        val qty = Text.parseQuantity(m.groupValues[1])
        var rest = m.groupValues[2].trim()
        var unit = ""
        val firstSpace = rest.indexOf(' ')
        val firstToken = if (firstSpace < 0) rest else rest.substring(0, firstSpace)
        val canon = Units.canonical(firstToken)
        if (canon != null && firstSpace > 0) {
            unit = canon
            rest = rest.substring(firstSpace + 1).trim()
            val n = Text.normalize(rest)
            if (unit == "κ.σ." && n.startsWith("τησ σουπασ")) rest = rest.substring("της σούπας".length).trim()
            if (unit == "κ.γ." && n.startsWith("του γλυκου")) rest = rest.substring("του γλυκού".length).trim()
            if (Text.normalize(rest).startsWith("απο ")) rest = rest.substring(4).trim()
        }
        if (rest.isEmpty()) return null
        return ParsedIngredient(rest, qty, unit)
    }

    private fun capitalizeNone(s: String) = s

    fun parseLines(text: String): List<ParsedIngredient> = text.lines().mapNotNull { parse(it) }
}
