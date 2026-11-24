package com.example.tippscores.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage

/**
 * Egységes képbetöltés: ha nincs URL, vagy a kép nem tölt be, a
 * [fallback] tartalom jelenik meg (logó helyett emoji, fotó helyett
 * mezszám). A Coil a sikeres képeket memóriában és lemezen is tárolja.
 */
@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    fallback: @Composable () -> Unit
) {
    if (url.isNullOrBlank()) {
        fallback()
        return
    }

    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = { fallback() },
        error = { fallback() }
    )
}
