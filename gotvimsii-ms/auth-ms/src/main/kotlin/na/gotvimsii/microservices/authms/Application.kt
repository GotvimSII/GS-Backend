package na.gotvimsii.microservices.authms

import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.util.*
import na.gotvimsii.microservices.authms.database.DatabaseFactory
import na.gotvimsii.microservices.authms.helpers.AppServices
import na.gotvimsii.microservices.authms.helpers.configureServices
import na.gotvimsii.microservices.authms.security.configureSecurity
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    EngineMain.main(args)
}

val Application.services: AppServices
    get() = attributes[AttributeKey("AppServices")]

fun Application.module() {
    configureServices()
    configureSerialization()
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
    configureRouting()

    monitor.subscribe(ApplicationStopping) {
        log.info("Stopping application gracefully...")
        DatabaseFactory.close()
    }
    monitor.subscribe(ApplicationStopped) {
        services.sessionClient.close()
        services.redis.close()
        log.info("Application stopped.")
    }
    Runtime.getRuntime().addShutdownHook(
        Thread {
            infoLogger.info("JVM shutdown hook registered.")
        }
    )
}