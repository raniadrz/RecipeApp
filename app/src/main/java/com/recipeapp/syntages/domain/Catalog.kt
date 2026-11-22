package com.recipeapp.syntages.domain

object Catalog {
    val categories = listOf(
        "Ορεκτικά", "Σαλάτες", "Σούπες", "Κυρίως πιάτα", "Ζυμαρικά", "Όσπρια",
        "Λαδερά", "Ψάρια & Θαλασσινά", "Πίτες", "Γλυκά", "Ψωμί & Ζύμες", "Πρωινό", "Ροφήματα", "Άλλο",
    )

    val tags = listOf("Νηστίσιμο", "Χορτοφαγικό", "Χωρίς γλουτένη", "Παιδικό", "Γρήγορο")

    val difficulties = listOf("Εύκολη", "Μέτρια", "Δύσκολη")

    val meals = listOf("Πρωινό", "Μεσημεριανό", "Βραδινό")

    fun difficultyLabel(d: Int) = difficulties.getOrElse(d) { difficulties[0] }

    fun minutesLabel(min: Int): String = when {
        min <= 0 -> "—"
        min < 60 -> "$min'"
        min % 60 == 0 -> "${min / 60} ώρ."
        else -> "${min / 60} ώρ. ${min % 60}'"
    }
}
