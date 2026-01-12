package na.gotvimsii.microservices.authms

import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import na.gotvimsii.microservices.authms.database.DatabaseFactory
import na.gotvimsii.microservices.authms.security.configureSecurity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val jsonModule = Json {
        prettyPrint = true
        isLenient = true
    }

    configureSerialization(jsonModule)

    val shouldMigrate = environment.config.propertyOrNull("migrate")?.getString()
    val infoLogger: Logger = LoggerFactory.getLogger(this.javaClass.packageName.toString())

    if (shouldMigrate == "migrate") {
        infoLogger.info("Running with migrations...")
        DatabaseFactory.init(true)
    } else {
        infoLogger.info("Running without migrations...")
        DatabaseFactory.init(false)
    }

    configureSecurity()
    configureRouting(jsonModule)

    monitor.subscribe(ApplicationStopping) {
        log.info("Stopping application gracefully...")
        DatabaseFactory.close()
    }
    monitor.subscribe(ApplicationStopped) {
        log.info("Application stopped.")
    }
    Runtime.getRuntime().addShutdownHook(
        Thread {
            infoLogger.info("JVM shutdown hook registered.")
        }
    )
}