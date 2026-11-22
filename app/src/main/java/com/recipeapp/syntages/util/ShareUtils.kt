package com.recipeapp.syntages.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.recipeapp.syntages.data.RecipeWithIngredients
import com.recipeapp.syntages.data.ShoppingItem
import com.recipeapp.syntages.domain.Catalog
import com.recipeapp.syntages.domain.Sections
import com.recipeapp.syntages.domain.Units

enum class ShareTarget(val label: String, val pkg: String?) {
    VIBER("Viber", "com.viber.voip"),
    WHATSAPP("WhatsApp", "com.whatsapp"),
    SMS("SMS", null),
    OTHER("Άλλη εφαρμογή…", null),
}

object ShareUtils {

    fun shoppingText(items: List<ShoppingItem>): String {
        val pending = items.filter { !it.checked }
        val sb = StringBuilder("🛒 Λίστα αγορών\n")
        pending.groupBy { it.section.ifBlank { Sections.OTHER } }
            .toSortedMap(compareBy { Sections.order(it) })
            .forEach { (section, list) ->
                sb.append("\n").append(section).append(":\n")
                list.forEach { sb.append("☐ ").append(itemLine(it)).append('\n') }
            }
        if (pending.isEmpty()) sb.append("\n(η λίστα είναι άδεια)")
        return sb.toString().trimEnd()
    }

    fun itemLine(item: ShoppingItem): String {
        val q = Units.display(item.quantity, item.unit)
        return if (q.isBlank()) item.name else "$q ${item.name}"
    }

    fun recipeText(r: RecipeWithIngredients): String {
        val rec = r.recipe
        val sb = StringBuilder()
        sb.append("🍲 ").append(rec.title).append('\n')
        sb.append("Κατηγορία: ").append(rec.category).append('\n')
        sb.append("⏱ Προετοιμασία: ").append(Catalog.minutesLabel(rec.prepMinutes))
            .append(" · Μαγείρεμα: ").append(Catalog.minutesLabel(rec.cookMinutes)).append('\n')
        sb.append("Δυσκολία: ").append(Catalog.difficultyLabel(rec.difficulty))
            .append(" · Μερίδες: ").append(rec.servings).append('\n')
        if (rec.tags.isNotEmpty()) sb.append("🏷 ").append(rec.tags.joinToString(", ")).append('\n')
        sb.append("\n📝 Υλικά\n")
        r.ingredients.forEach {
            val q = Units.display(it.quantity, it.unit)
            sb.append("• ").append(if (q.isBlank()) it.name else "$q ${it.name}").append('\n')
        }
        sb.append("\n👩‍🍳 Εκτέλεση\n")
        rec.steps.forEachIndexed { i, s -> sb.append(i + 1).append(". ").append(s).append('\n') }
        if (rec.notes.isNotBlank()) sb.append("\n💡 Σημειώσεις: ").append(rec.notes).append('\n')
        sb.append("\nΚαλή επιτυχία! 😋")
        return sb.toString()
    }

    fun share(context: Context, text: String, target: ShareTarget, imageUri: Uri? = null) {
        if (target == ShareTarget.SMS) {
            val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", text)
            start(context, sms, "SMS")
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            if (imageUri != null) {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                clipData = ClipData.newRawUri("", imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }
        if (target.pkg != null) {
            send.setPackage(target.pkg)
            start(context, send, target.label)
        } else {
            start(context, Intent.createChooser(send, "Αποστολή με…"), "κοινοποίηση")
        }
    }

    private fun start(context: Context, intent: Intent, label: String) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Η εφαρμογή $label δεν είναι εγκατεστημένη", Toast.LENGTH_LONG).show()
        }
    }
}
