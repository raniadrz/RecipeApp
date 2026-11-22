package com.recipeapp.syntages.ui.cooking

import android.app.Application
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recipeapp.syntages.data.RecipeRepository
import com.recipeapp.syntages.data.RecipeWithIngredients
import com.recipeapp.syntages.domain.Text
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CookingViewModel(private val app: Application, repo: RecipeRepository, id: Long) : ViewModel() {
    val recipe: StateFlow<RecipeWithIngredients?> = repo.observeRecipe(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Συνολική διάρκεια χρονομέτρου σε ms. */
    var durationMs by mutableLongStateOf(5 * 60_000L)
        private set
    var remainingMs by mutableLongStateOf(5 * 60_000L)
        private set
    var running by mutableStateOf(false)
        private set
    var finished by mutableStateOf(false)
        private set

    val checkedIngredients = mutableStateOf(setOf<Long>())

    private var endAt = 0L
    private var job: Job? = null
    private var ringtone: Ringtone? = null

    fun toggleIngredient(id: Long) {
        val s = checkedIngredients.value
        checkedIngredients.value = if (id in s) s - id else s + id
    }

    fun setMinutes(min: Int) {
        stopAlarm()
        pause()
        durationMs = min.coerceIn(1, 600) * 60_000L
        remainingMs = durationMs
        finished = false
    }

    fun adjust(deltaMin: Int) {
        val newRemaining = (remainingMs + deltaMin * 60_000L).coerceAtLeast(0)
        if (running) endAt += deltaMin * 60_000L
        remainingMs = newRemaining
        if (newRemaining > durationMs) durationMs = newRemaining
        if (newRemaining == 0L && running) onFinished()
    }

    fun startPause() {
        if (running) pause() else start()
    }

    private fun start() {
        if (remainingMs <= 0) remainingMs = durationMs
        stopAlarm()
        finished = false
        endAt = SystemClock.elapsedRealtime() + remainingMs
        running = true
        job?.cancel()
        job = viewModelScope.launch {
            while (isActive) {
                val left = endAt - SystemClock.elapsedRealtime()
                if (left <= 0) {
                    remainingMs = 0
                    onFinished()
                    break
                }
                remainingMs = left
                delay(200)
            }
        }
    }

    private fun pause() {
        job?.cancel()
        running = false
    }

    fun reset() {
        pause()
        stopAlarm()
        finished = false
        remainingMs = durationMs
    }

    private fun onFinished() {
        job?.cancel()
        running = false
        finished = true
        playAlarm()
    }

    private fun playAlarm() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(app, uri)?.apply {
                audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                play()
            }
        }
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (app.getSystemService(VibratorManager::class.java)).defaultVibrator
            } else {
                @Suppress("DEPRECATION") app.getSystemService(Vibrator::class.java)
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 300, 600, 300, 600), -1))
        }
        // Σταματά αυτόματα μετά από 30''
        viewModelScope.launch { delay(30_000); stopAlarm() }
    }

    fun stopAlarm() {
        runCatching { ringtone?.stop() }
        ringtone = null
    }

    fun dismissFinished() {
        stopAlarm()
        finished = false
        remainingMs = durationMs
    }

    override fun onCleared() {
        stopAlarm()
    }

    companion object {
        private val minutesRegex = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:-\\s*\\d+\\s*)?(λεπτ|΄|'|min)")
        private val hoursRegex = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:-\\s*\\d+\\s*)?(ωρ|ωρα|ωρεσ|h\\b)")
        private val halfHour = Regex("μιση ωρα|μισαωρο")

        /** Βρίσκει χρόνο μέσα στο κείμενο ενός βήματος («Ψήσε για 40 λεπτά» → 40). */
        fun detectMinutes(step: String): Int? {
            val n = Text.normalize(step)
            minutesRegex.find(n)?.let { return it.groupValues[1].replace(',', '.').toDoubleOrNull()?.toInt()?.takeIf { m -> m in 1..600 } }
            hoursRegex.find(n)?.let { return it.groupValues[1].replace(',', '.').toDoubleOrNull()?.let { h -> (h * 60).toInt() }?.takeIf { m -> m in 1..600 } }
            if (halfHour.containsMatchIn(n)) return 30
            return null
        }
    }
}
