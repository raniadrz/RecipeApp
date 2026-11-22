package com.recipeapp.syntages

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.recipeapp.syntages.ui.SyntagesApp
import com.recipeapp.syntages.ui.theme.SyntagesTheme
import com.recipeapp.syntages.util.RecipeImporter

class MainActivity : ComponentActivity() {
    /** Link που ήρθε με «Κοινοποίηση» από άλλη εφαρμογή (π.χ. Chrome). */
    private val sharedUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            SyntagesTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SyntagesApp(sharedUrl = sharedUrl.value, onSharedUrlHandled = { sharedUrl.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val url = RecipeImporter.extractUrl(intent.getStringExtra(Intent.EXTRA_TEXT))
        if (url != null) sharedUrl.value = url
        else Toast.makeText(this, "Δεν βρέθηκε link συνταγής σε ό,τι κοινοποίησες", Toast.LENGTH_LONG).show()
    }
}
