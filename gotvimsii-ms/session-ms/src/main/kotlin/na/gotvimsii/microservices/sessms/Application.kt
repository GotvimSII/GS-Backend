package na.gotvimsii.microservices.sessms

import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.util.*
import na.gotvimsii.microservices.sessms.database.DatabaseFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    EngineMain.main(args)
}

val Application.services: AppServices
    get() = attributes[AttributeKey("AppServices")]

fun Application.module() {
    configureServices()

    val shouldMigrate = environment.config.propertyOrNull("migrate")?.getString()
    val infoLogger: Logger = LoggerFactory.getLogger(this.javaClass.packageName.toString())

    if (shouldMigrate == "migrate") {
        infoLogger.info("Running with migrations...")
        DatabaseFactory.init(true)
    } else {
        infoLogger.info("Running without migrations...")
        DatabaseFactory.init(false)
    }

    configureSerialization()
    configureRouting()

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