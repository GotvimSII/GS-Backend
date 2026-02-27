package na.gotvimsii.microservices.recipems

import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.util.*
import na.gotvimsii.microservices.recipems.database.DatabaseFactory
import na.gotvimsii.microservices.recipems.helpers.loadRecipesFromFile
import na.gotvimsii.microservices.recipems.security.configureSecurity
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
    configureSecurity()

    val shouldMigrate = environment.config.propertyOrNull("migrate")?.getString()
    val infoLogger: Logger = LoggerFactory.getLogger(this.javaClass.packageName)

    if (shouldMigrate == "migrate") {
        infoLogger.info("Running with migrations...")
        DatabaseFactory.init(true)
    } else {
        infoLogger.info("Running without migrations...")
        DatabaseFactory.init(false)
    }

    val recipesLocation = environment.config.propertyOrNull("recipes")
    if (recipesLocation != null) loadRecipesFromFile(recipesLocation.getString())

    configureRouting()

    monitor.subscribe(ApplicationStopping) {
        log.info("Stopping application gracefully...")
        DatabaseFactory.close()
        //services.recipeRedis.close()
        services.rateLimitRedis.close()
    }
    monitor.subscribe(ApplicationStopped) {
        log.info("Application stopped.")
    }
//    Runtime.getRuntime().addShutdownHook(
//        Thread {
//            infoLogger.info("JVM shutdown hook registered.")
//        }
//    )
}