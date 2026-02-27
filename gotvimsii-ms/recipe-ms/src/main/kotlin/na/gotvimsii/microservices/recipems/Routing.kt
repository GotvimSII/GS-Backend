package na.gotvimsii.microservices.recipems

import dev.nikdi.redisratelimit.RedisRateLimit
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import na.gotvimsii.common.classes.ApiError
import na.gotvimsii.microservices.recipems.helpers.RecipeRedisService
import na.gotvimsii.microservices.recipems.helpers.RecipeSearchResult
import na.gotvimsii.microservices.recipems.helpers.RecipeSearchService
import java.util.*

internal fun Application.configureRouting() {
    val recipeRedisService = RecipeRedisService(services.recipeRedis)
    val recipeSearchService = RecipeSearchService(log)

    suspend fun storeAndRespond(call: ApplicationCall, requestId: UUID, recipeIds: List<Int>) {
        recipeRedisService.storeObtainedRecipesInRedis(requestId, recipeIds)

        val recipeId = recipeRedisService.getNextStoredId(requestId) ?: return call.respond(
            HttpStatusCode.OK,
            NoRecipeFound("No recipes were found!")
        )

        val recipe = recipeSearchService.getRecipeWithIngredients(recipeId)
        call.respond(
            HttpStatusCode.OK,
            RecipeResponse(
                recipe,
                requestId
            )
        )
    }

    routing {
        route("/ping") {
            install(RedisRateLimit) {
                rethisInstance = services.rateLimitRedis
                maxRequests = 5
                onRateLimited = { call ->
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        ApiError("Too many requests!")
                    )
                }
            }

            get { call.respondText("pong :)") }
        }

        authenticate("auth-jwt") {
            route("/recipes") {
                install(RedisRateLimit) {
                    rethisInstance = services.rateLimitRedis
                    maxRequests = 5
                    onRateLimited = { call ->
                        call.respond(
                            HttpStatusCode.TooManyRequests,
                            ApiError("Too many requests!")
                        )
                    }
                }

                get("/by-name") {
                    val name = runCatching { call.receive<RecipeName>() }.getOrElse {
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError("No recipe name provided!")
                        )
                    }.name

                    when (val result = recipeSearchService.searchByName(name)) {
                        is RecipeSearchResult.Found -> {
                            val requestUUID = UUID.randomUUID()
                            storeAndRespond(
                                call,
                                requestUUID,
                                result.ids
                            )
                        }

                        is RecipeSearchResult.NotFound -> call.respond(
                            HttpStatusCode.OK,
                            NoRecipeFound("No recipes were found!")
                        )

                        is RecipeSearchResult.Error -> call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError(result.message)
                        )
                    }
                }

                get("/from-ingredients") {
                    val request = runCatching { call.receive<IngredientRequest>() }.getOrElse { cause ->
                        return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError("Invalid request format. 'mode' must be 'all' or 'exact'. ${cause.toString()}")
                        )
                    }

                    val ingredients = request.ingredients
                    val mode = request.mode

                    when (val result = recipeSearchService.searchByIngredients(ingredients, mode)) {
                        is RecipeSearchResult.Found -> {
                            val requestUUID = UUID.randomUUID()
                            storeAndRespond(
                                call,
                                requestUUID,
                                result.ids
                            )
                        }

                        is RecipeSearchResult.NotFound -> call.respond(
                            HttpStatusCode.OK,
                            NoRecipeFound("No recipes were found!")
                        )

                        is RecipeSearchResult.Error -> call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError(result.message)
                        )
                    }
                }

                get("/regenerate/{requestId}") {
                    val requestId = call.pathParameters["requestId"]?.let(UUID::fromString)
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError("No request ID provided!")
                        )

                    val recipeId = recipeRedisService.getNextStoredId(requestId) ?: return@get call.respond(
                        HttpStatusCode.OK,
                        NoRecipeFound("No recipes were found!")
                    )

                    val recipe = recipeSearchService.getRecipeWithIngredients(recipeId)
                    call.respond(
                        HttpStatusCode.OK,
                        recipe
                    )
                }
            }
        }
    }
}