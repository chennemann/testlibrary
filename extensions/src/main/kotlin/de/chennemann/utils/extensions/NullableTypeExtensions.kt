package de.chennemann.utils.extensions

inline fun <T, R> T?.ifPresent(block: (T) -> R?): R? {
    return if (this != null) {
        block(this)
    } else {
        null
    }
}

inline fun <R> R?.orElse(block: () -> R): R {
    return this ?: block()
}