package na.gotvimsii.microservices.recipems.helpers

internal fun normalize(text: String) = text
    .lowercase()
    .replace(Regex("[^\\p{L}\\s]"), "")
    .replace(Regex("\\s+"), " ")
    .trim()