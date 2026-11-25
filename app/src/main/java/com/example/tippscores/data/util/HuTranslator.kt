package com.example.tippscores.data.util

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Angol -> magyar fordítás az eszközön (Google ML Kit). A nyelvi modell
 * (kb. 30 MB) az első használatkor töltődik le, utána internet nélkül is
 * működik. Ha a fordítás nem sikerül (pl. még nincs modell és nincs net),
 * az eredeti szöveg marad, és egy perc múlva újra próbálkozik.
 */
object HuTranslator {

    private val cache = object : LinkedHashMap<String, String>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > 800
    }

    private val modelMutex = Mutex()

    @Volatile
    private var modelReady = false

    @Volatile
    private var retryAfter = 0L

    private val translator: Translator by lazy {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.HUNGARIAN)
                .build()
        )
    }

    suspend fun translate(text: String): String {
        val clean = text.trim()
        if (clean.isEmpty()) return text

        HuText.lookup(clean)?.let { return it }
        if (HuText.looksHungarian(clean)) return text

        synchronized(cache) { cache[clean] }?.let { return it }

        if (System.currentTimeMillis() < retryAfter) return text

        return try {
            ensureModel()
            val result = translator.translate(clean).await()
            synchronized(cache) { cache[clean] = result }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            retryAfter = System.currentTimeMillis() + RETRY_DELAY_MS
            text
        }
    }

    private suspend fun ensureModel() {
        if (modelReady) return
        modelMutex.withLock {
            if (modelReady) return
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            modelReady = true
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    private const val RETRY_DELAY_MS = 60_000L
}
