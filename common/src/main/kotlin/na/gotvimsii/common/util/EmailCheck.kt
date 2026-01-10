package na.gotvimsii.common.util

fun String.isEmail(): Boolean {
    val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    return emailRegex.matches(this)
}

fun String.isNotEmail(): Boolean = !isEmail()