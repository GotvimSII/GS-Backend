package na.gotvimsii.common.util

import io.github.cdimascio.dotenv.dotenv

object Environment {
    private val dotenv = dotenv {
        val file = System.getProperty("env.file")
            ?: System.getenv("ENV_FILE")
            ?: ".env"
        directory = System.getProperty("user.dir")
        filename = file
        ignoreIfMissing = true
    }
    
    operator fun get(key: String): String {
        return dotenv[key] ?: throw NoEnvironmentValueException("$key not set in .env!")
    }
}