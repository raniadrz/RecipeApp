package com.recipeapp.syntages

import com.recipeapp.syntages.domain.IngredientParser
import com.recipeapp.syntages.domain.WebRecipeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebRecipeParserTest {
    private val graphPage = """
        <html><head><title>Παστίτσιο | Site</title>
        <script type="application/ld+json" class="yoast-schema-graph">
        {"@context":"https://schema.org","@graph":[
          {"@type":"WebPage","name":"Παστίτσιο"},
          {"@type":["Recipe"],"name":"Παστίτσιο της γιαγιάς &amp; μπεσαμέλ",
           "image":[{"@type":"ImageObject","url":"https://example.com/pastitsio.jpg"}],
           "recipeYield":["8","8 μερίδες"],
           "prepTime":"PT30M","cookTime":"PT1H15M",
           "recipeCategory":"Κυρίως πιάτο","keywords":"φούρνος, παιδικό",
           "recipeIngredient":["500 γρ. μακαρόνια Νο2","750 γρ. κιμά μοσχαρίσιο","1 κρεμμύδι","1 λίτρο γάλα"],
           "recipeInstructions":[
             {"@type":"HowToSection","name":"Κιμάς","itemListElement":[
                {"@type":"HowToStep","text":"Σοτάρουμε το κρεμμύδι."},
                {"@type":"HowToStep","text":"Προσθέτουμε τον κιμά και σιγοβράζουμε 30 λεπτά."}]},
             {"@type":"HowToStep","text":"Ψήνουμε στους 180&deg; για 50 λεπτά."}
           ]}
        ]}
        </script></head><body></body></html>
    """.trimIndent()

    @Test fun parsesGraphRecipe() {
        val r = WebRecipeParser.parse(graphPage, "https://example.com/pastitsio")
        assertNotNull(r); r!!
        assertEquals("Παστίτσιο της γιαγιάς & μπεσαμέλ", r.title)
        assertEquals("https://example.com/pastitsio.jpg", r.imageUrl)
        assertEquals(8, r.servings)
        assertEquals(30, r.prepMinutes)
        assertEquals(75, r.cookMinutes)
        assertEquals("Κυρίως πιάτα", r.category)
        assertTrue("Παιδικό" in r.tags)
        assertEquals(4, r.ingredients.size)
        assertEquals(listOf("Σοτάρουμε το κρεμμύδι.", "Προσθέτουμε τον κιμά και σιγοβράζουμε 30 λεπτά.", "Ψήνουμε στους 180° για 50 λεπτά."), r.steps)
        val kima = IngredientParser.parse(r.ingredients[1])!!
        assertEquals(750.0, kima.quantity)
        assertEquals("γρ.", kima.unit)
    }

    @Test fun parsesPlainRecipeWithHtmlInstructions() {
        val html = """<script type='application/ld+json'>{"@context":"http://schema.org","@type":"Recipe",
            "name":"Φασολάδα","image":"https://x.gr/f.jpg","totalTime":"PT2H","recipeYield":"4 άτομα",
            "suitableForDiet":"https://schema.org/VeganDiet","keywords":"νηστίσιμο",
            "recipeIngredient":"500 γρ. φασόλια\n2 καρότα",
            "recipeInstructions":"<p>Μουλιάζουμε τα φασόλια.</p><p>Βράζουμε 1 ώρα.</p>"}</script>"""
        val r = WebRecipeParser.parse(html, "https://x.gr/fasolada")!!
        assertEquals("Φασολάδα", r.title)
        assertEquals(listOf("500 γρ. φασόλια", "2 καρότα"), r.ingredients)
        assertEquals(listOf("Μουλιάζουμε τα φασόλια.", "Βράζουμε 1 ώρα."), r.steps)
        assertEquals(120, r.cookMinutes)
        assertEquals(4, r.servings)
        assertTrue(r.tags.containsAll(listOf("Νηστίσιμο", "Χορτοφαγικό")))
    }

    @Test fun fallsBackToOpenGraph() {
        val html = """<meta property="og:title" content="Κέικ λεμονιού"><meta property="og:image" content="https://x.gr/k.jpg">"""
        val r = WebRecipeParser.parse(html, "https://x.gr/keik")!!
        assertEquals("Κέικ λεμονιού", r.title)
        assertEquals("https://x.gr/k.jpg", r.imageUrl)
        assertTrue(!r.isComplete)
    }

    @Test fun readsHeadingsWhenNoStructuredData() {
        val html = """
            <html><head><meta property="og:title" content="Το παστίτσιο του Άκη | Akis Petretzikis">
            <meta property="og:image" content="https://x.gr/p.jpg"><script>var a = "<li>όχι</li>";</script></head>
            <body><nav><ul><li>Συνταγές</li><li>Εκτέλεση</li></ul></nav>
            <h1>Το παστίτσιο του Άκη</h1>
            <div class="ingredients"><h2>Υλικά</h2>
              <h3>Για τον κιμά</h3>
              <ul><li><span>500 γρ.</span> <span>κιμά μοσχαρίσιο</span></li><li>1 ξερό κρεμμύδι</li></ul>
              <h3>Για τη μπεσαμέλ</h3>
              <ul><li>1 λίτρο γάλα</li><li>100 γρ. βούτυρο</li></ul></div>
            <div><h2>Εκτέλεση</h2><ol>
              <li>Σε μια κατσαρόλα σοτάρουμε το κρεμμύδι για 5 λεπτά.</li>
              <li>Προσθέτουμε τον κιμά και μαγειρεύουμε για 20 λεπτά.</li></ol></div>
            <h2>Σχόλια</h2><p>Πολύ ωραίο φαγητό, το έφτιαξα χθες για όλη την οικογένεια!</p>
            </body></html>
        """.trimIndent()
        val r = WebRecipeParser.parse(html, "https://x.gr/pastitsio")!!
        assertEquals("Το παστίτσιο του Άκη", r.title)
        assertEquals("https://x.gr/p.jpg", r.imageUrl)
        assertEquals(listOf("500 γρ. κιμά μοσχαρίσιο", "1 ξερό κρεμμύδι", "1 λίτρο γάλα", "100 γρ. βούτυρο"), r.ingredients)
        assertEquals(2, r.steps.size)
        assertTrue(r.steps[0].startsWith("Σε μια κατσαρόλα"))
        assertTrue(r.isComplete)
    }

    @Test fun durations() {
        assertEquals(90, WebRecipeParser.minutes("PT1H30M"))
        assertEquals(45, WebRecipeParser.minutes("PT45M"))
        assertEquals(0, WebRecipeParser.minutes("garbage"))
    }
}
