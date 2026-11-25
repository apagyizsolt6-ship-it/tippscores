package com.example.tippscores.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import com.example.tippscores.data.util.HuText
import com.example.tippscores.data.util.HuTranslator

/**
 * Az angol nyelvű adatot (StatPal szövegek) magyarra fordítja.
 * A szótárban szereplő kifejezések azonnal magyarul jelennek meg, a
 * hosszabb szövegek az eredetivel indulnak, és a fordítás megérkeztekor
 * cserélődnek.
 */
@Composable
fun rememberHu(text: String?): String? {
    val initial = text?.let { HuText.lookup(it.trim()) ?: it }

    val state = produceState(initialValue = initial, key1 = text) {
        value = if (text.isNullOrBlank()) text else HuTranslator.translate(text)
    }

    return state.value
}
