package na.gotvimsii.microservices.recipems

import dev.nikdi.redisratelimit.RedisRateLimit
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import na.gotvimsii.common.classes.ApiError
import na.gotvimsii.microservices.recipems.helpers.QueueSocketClient
import na.gotvimsii.microservices.recipems.helpers.RecipeRedisService
import na.gotvimsii.microservices.recipems.helpers.RecipeSearchResult
import na.gotvimsii.microservices.recipems.helpers.RecipeSearchService
import java.util.*

internal fun Application.configureRouting() {
    val recipeRedisService = RecipeRedisService(services.recipeRedis)
    val recipeSearchService = RecipeSearchService(log)
    val socketClient = QueueSocketClient(services.recipeJson)

    suspend fun ApplicationCall.respondWithNextRecipe(requestId: UUID) {
        val recipeDetails = recipeRedisService.getNextStoredId(requestId)
            ?: return this.respond(
                HttpStatusCode.NotFound,
                NoRecipeFound("No recipes were found!")
            )

        val matchedRecipe = recipeSearchService.getRecipeWithIngredients(recipeDetails.first, recipeDetails.second)

        runCatching {
            socketClient.sendRequest(matchedRecipe)
                ?: error("Socket returned null")
        }.onSuccess { formattedRecipe ->
            this.respond(
                HttpStatusCode.OK,
                RecipeResponse(formattedRecipe, requestId)
            )
        }.onFailure {
            this.respond(
                HttpStatusCode.InternalServerError,
                ApiError("An error occurred, please try again!")
            )
        }
    }

    suspend fun storeAndRespond(call: ApplicationCall, requestId: UUID, recipeIds: List<Int>, portions: Int) {
        recipeRedisService.storeObtainedRecipesInRedis(requestId, recipeIds, portions)
        call.respondWithNextRecipe(requestId)
    }

    routing {
        route("/recipes") {
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
                route("/") {
                    install(RedisRateLimit) {
                        rethisInstance = services.rateLimitRedis
                        maxRequests = 30
                        onRateLimited = { call ->
                            call.respond(
                                HttpStatusCode.TooManyRequests,
                                ApiError("Too many requests!")
                            )
                        }
                    }

                    post("by-name") {
                        val request = runCatching { call.receive<NameRequest>() }.getOrElse {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError("No recipe name provided!")
                            )
                        }

                        when (val result = recipeSearchService.searchByName(request.name)) {
                            is RecipeSearchResult.Found -> {
                                val requestUUID = UUID.randomUUID()
                                storeAndRespond(
                                    call,
                                    requestUUID,
                                    result.ids,
                                    request.portions
                                )
                            }

                            is RecipeSearchResult.NotFound -> call.respond(
                                HttpStatusCode.NotFound,
                                NoRecipeFound("No recipes were found!")
                            )

                            is RecipeSearchResult.Error -> call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError(result.message)
                            )
                        }
                    }

                    post("from-ingredients") {
                        val request = runCatching { call.receive<IngredientRequest>() }.getOrElse { cause ->
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError("Invalid request format. 'mode' must be 'all' or 'exact'. ${cause.toString()}")
                            )
                        }

                        when (val result = recipeSearchService.searchByIngredients(request.ingredients, request.mode)) {
                            is RecipeSearchResult.Found -> {
                                val requestUUID = UUID.randomUUID()
                                storeAndRespond(
                                    call,
                                    requestUUID,
                                    result.ids,
                                    request.portions
                                )
                            }

                            is RecipeSearchResult.NotFound -> call.respond(
                                HttpStatusCode.NotFound,
                                NoRecipeFound("No recipes were found!")
                            )

                            is RecipeSearchResult.Error -> call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiError(result.message)
                            )
                        }
                    }

                    post("regenerate/{requestId}") {
                        val requestId = call.pathParameters["requestId"]?.let(UUID::fromString)
                            ?: return@post call.respond(
                                HttpStatusCode.BadRequest,
                                ApiError("No request ID provided!")
                            )

                        call.respondWithNextRecipe(requestId)
                    }
                }
            }
        }
    }
}