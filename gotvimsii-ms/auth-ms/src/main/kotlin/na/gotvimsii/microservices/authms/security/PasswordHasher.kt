package na.gotvimsii.microservices.authms.security

import de.mkammerer.argon2.Argon2Factory
import na.gotvimsii.common.util.Environment

const val A2_PARALLELISM = "A2_PARALLELISM"
const val A2_MEMORY = "A2_MEMORY"
const val A2_ITERATIONS = "A2_ITERATIONS"

object PasswordHasher {
    private val parallelism = Environment[A2_PARALLELISM].toInt()
    private val memory = Environment[A2_MEMORY].toInt()
    private val iterations = Environment[A2_ITERATIONS].toInt()

    private val argon2 = Argon2Factory.create()

    fun hash(rawPassword: String): String? =
        argon2.hash(iterations, memory, parallelism, rawPassword.toCharArray())

    fun matches(rawPassword: String, hashedPassword: String): Boolean =
        argon2.verify(hashedPassword, rawPassword.toCharArray())
}