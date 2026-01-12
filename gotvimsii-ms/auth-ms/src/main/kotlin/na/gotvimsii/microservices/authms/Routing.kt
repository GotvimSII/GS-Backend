package na.gotvimsii.microservices.authms

import eu.vendeli.rethis.ReThis
import eu.vendeli.rethis.command.generic.del
import eu.vendeli.rethis.command.serde.get
import eu.vendeli.rethis.command.serde.set
import eu.vendeli.rethis.command.string.getDel
import eu.vendeli.rethis.shared.request.string.SetExpire
import eu.vendeli.rethis.types.common.RespVer
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import na.gotvimsii.common.classes.*
import na.gotvimsii.common.util.Environment
import na.gotvimsii.common.util.isNotEmail
import na.gotvimsii.microservices.authms.database.UserEntity
import na.gotvimsii.microservices.authms.database.UserTable
import na.gotvimsii.microservices.authms.security.JwtProvider
import na.gotvimsii.microservices.authms.security.PasswordHasher
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.security.MessageDigest
import java.sql.SQLException
import java.time.Instant
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

fun Application.configureRouting(jsonModule: Json) {
    val sessionClient = HttpClient(Java) {
        this.defaultRequest {
            url("http://session-ms:7241")
        }
        this.install(ContentNegotiation) {
            json(jsonModule)
        }
    }

    val rethis = ReThis(host = "auth-redis", port = 6379, protocol = RespVer.V3) {
        auth(
            password = Environment["REDIS_PASS"].toCharArray(),
            username = Environment["REDIS_USER"]
        )
    }

    routing {
        swaggerUI("/docs", "openapi/documentation.v1.yaml")

        get("/ping") {
            call.respondText("pong :)")
        }

        post("/register") {
            val userCredentials = runCatching { call.receive<RegistrationCredentials>() }.getOrElse {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("Invalid or malformed request format!")
                )
            }

            val email = userCredentials.email
            val username = userCredentials.username
            val password = userCredentials.password

            if (username.isBlank()) return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiError("Username cannot be blank or empty!")
            )
            if (email.isNotEmail()) return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiError("Email format is invalid!")
            )
            if (password.length < 10) return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiError("Password must be at least 10 characters long!")
            )

            val hashed = PasswordHasher.hash(password) ?: return@post call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("Server error!")
            )

            val userId = UUID.randomUUID()

            try {
                transaction {
                    UserEntity.new(userId) {
                        this.email = email
                        this.username = username
                        this.passwordHash = hashed
                    }
                }

                call.respond(
                    HttpStatusCode.Created,
                    ApiSuccess("Created!")
                )

            } catch (e: SQLException) {
                when (e.sqlState) {
                    "23514" -> {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiError("Email format is invalid!")
                        )
                        return@post
                    }

                    "23505" -> {
                        val constraintName =
                            e.message?.substringAfter("violates unique constraint \"")
                                ?.substringBefore("\"")

                        when (constraintName) {
                            "users_email_key" -> {
                                call.respond(
                                    HttpStatusCode.Conflict,
                                    ApiError("Email is already taken!")
                                )
                                return@post
                            }

                            "users_username_key" -> {
                                call.respond(
                                    HttpStatusCode.Conflict,
                                    ApiError("Username is already taken!")
                                )
                                return@post
                            }
                        }
                    }

                    else -> {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiError(e.message ?: e.localizedMessage)
                        )
                        e.printStackTrace()
                    }
                }
            }
        }

        post("/login") {
            val loginCredentials = runCatching { call.receive<LoginCredentials>() }.getOrElse {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("Invalid or malformed request format!")
                )
            }

            val email = loginCredentials.email
            val username = loginCredentials.username
            val password = loginCredentials.password

            val user = transaction {
                UserEntity.find { (UserTable.email eq email) or (UserTable.username eq username) }.singleOrNull()
            } ?: return@post call.respond(
                HttpStatusCode.NotFound,
                ApiError("User not found!")
            )

            val matches = PasswordHasher.matches(password, user.passwordHash)

            if (!matches) return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiError("Invalid password!")
            )

            val refreshToken = JwtProvider.makeRefreshToken(user.id.value)
            val expiresAt = Instant.now().plusMillis(JwtProvider.REFRESH_TOKEN_LIFETIME)
            val refreshHashBytes = MessageDigest.getInstance("SHA-256").digest(refreshToken.toByteArray())
            val refreshHash = refreshHashBytes.joinToString("") { "%02x".format(it) }

            val sessionRequest = NewSessionRequest(
                userId = user.id.value,
                refreshTokenHash = refreshHash,
                expiresAt = expiresAt,
                ipAddress = call.request.origin.remoteAddress,
                userAgent = call.request.userAgent() // these two are SUPER easy to "hijack", but it's still something
            )

            try {
                val result = sessionClient.post("/new") {
                    contentType(ContentType.Application.Json)
                    setBody(sessionRequest)
                }

                if (result.status == HttpStatusCode.Created) {
                    val sessionId = result.body<SessionID>()

                    val redisInformation = RedisSessionEntry(
                        sessionId = sessionId.id,
                        userId = user.id.value,
                        expiresAt = expiresAt
                    )

                    rethis.transaction {
                        set(
                            "refresh:$refreshHash",
                            redisInformation,
                            RedisSessionEntry.serializer(),
                            SetExpire.Px(JwtProvider.REFRESH_TOKEN_LIFETIME.milliseconds)
                        )
                        set(
                            "session:${sessionId.id}",
                            RefreshTokenHash(refreshHash),
                            RefreshTokenHash.serializer(),
                            SetExpire.Px(JwtProvider.REFRESH_TOKEN_LIFETIME.milliseconds)
                        )
                    }

                    val accessToken = JwtProvider.makeAccessToken(user.id.value)

                    call.respond(
                        HttpStatusCode.OK,
                        LoginResponse(accessToken, refreshToken, sessionId.id)
                    )
                } else call.respond(result.status, result.bodyAsText())
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiError(e.message ?: e.localizedMessage)
                )
                e.printStackTrace()
            }
        }

        post("/refresh") {
            val refreshRequest = runCatching { call.receive<RefreshRequest>() }.getOrElse {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("Invalid or malformed request format!")
                )
            }

            try {
                val oldRefreshToken = refreshRequest.refreshToken
                val sessionId = refreshRequest.sessionId

                val decoded = JwtProvider.verifier().verify(oldRefreshToken) // TODO match the `catch` messages to be more indicative of what went wrong

                val tokenType = decoded.claims["type"]?.asString() ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("No type claim!")
                )

                if (tokenType != "refresh") return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("Not a refresh token!")
                )

                val tokenUserId = decoded.claims["userId"]?.asString() ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("No userId claim!")
                )
                val userId = UUID.fromString(tokenUserId)

                val oldRefreshHashBytes = MessageDigest.getInstance("SHA-256").digest(oldRefreshToken.toByteArray())
                val oldRefreshHash = oldRefreshHashBytes.joinToString("") { "%02x".format(it) }

                val redisEntry = rethis.get("refresh:$oldRefreshHash", RedisSessionEntry.serializer())
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        ApiError("Refresh token was changed, please log in again!")
                    )

                if (redisEntry.userId != userId || redisEntry.sessionId != sessionId) return@post call.respond(
                    HttpStatusCode.Forbidden,
                    ApiError("You are not authorized to perform this action!")
                )

                val newRefreshToken = JwtProvider.makeRefreshToken(userId)
                val expiresAt = Instant.now().plusMillis(JwtProvider.REFRESH_TOKEN_LIFETIME)
                val newRefreshHashBytes = MessageDigest.getInstance("SHA-256").digest(newRefreshToken.toByteArray())
                val newRefreshHash = newRefreshHashBytes.joinToString("") { "%02x".format(it) }

                val updateSessionRequest = UpdateSessionRequest(
                    refreshTokenHash = newRefreshHash,
                    expiresAt = expiresAt,
                    ipAddress = call.request.origin.remoteAddress,
                    userAgent = call.request.userAgent()
                )

                val result = sessionClient.patch("/$sessionId") {
                    contentType(ContentType.Application.Json)
                    setBody(updateSessionRequest)
                }

                if (result.status == HttpStatusCode.OK) {
                    val redisInformation = RedisSessionEntry(
                        sessionId = sessionId,
                        userId = userId,
                        expiresAt = expiresAt
                    )

                    rethis.transaction {
                        del("refresh:$oldRefreshHash")
                        set(
                            "refresh:$newRefreshHash",
                            redisInformation,
                            RedisSessionEntry.serializer(),
                            SetExpire.Px(JwtProvider.REFRESH_TOKEN_LIFETIME.milliseconds)
                        )
                        set(
                            "session:$sessionId",
                            RefreshTokenHash(newRefreshHash),
                            RefreshTokenHash.serializer(),
                            SetExpire.Px(JwtProvider.REFRESH_TOKEN_LIFETIME.milliseconds)
                        )
                    }

                    val newAccessToken = JwtProvider.makeAccessToken(userId)

                    call.respond(
                        HttpStatusCode.OK,
                        RefreshResponse(newAccessToken, newRefreshToken)
                    )
                } else call.respond(result.status, result.bodyAsText())
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiError(e.message ?: e.localizedMessage)
                )
                e.printStackTrace()
            }
        }

        authenticate("auth-jwt") {
            post("/logout") {
                val sessionId = runCatching { call.receive<SessionID>().id }.getOrElse {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiError("No session ID provided!")
                    )
                }

                val userId = call.principal<AuthUser>()!!.userId

                try {
                    val refreshHash = rethis.get(
                        "session:$sessionId",
                        RefreshTokenHash.serializer()
                    ) ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                        ApiError("Session doesn't exist!")
                    )

                    val redisEntry = rethis.get(
                        "refresh:${refreshHash.refreshTokenHash}",
                        RedisSessionEntry.serializer()
                    ) ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                        ApiError("Session is no longer valid!")
                    )

                    if (redisEntry.userId != userId) return@post call.respond(
                        HttpStatusCode.Forbidden,
                        ApiError("You are not authorized to perform this action!")
                    )

                    val result = sessionClient.delete("/$sessionId") {
                        contentType(ContentType.Application.Json)
                    }

                    rethis.transaction {
                        del("session:$sessionId")
                        del("refresh:${refreshHash.refreshTokenHash}")
                    }

                    if (result.status == HttpStatusCode.OK) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiSuccess("Logged out.")
                        )
                    } else {
                        call.respond(
                            result.status,
                            result.body<ApiError>()
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError(e.message ?: e.localizedMessage)
                    )
                    e.printStackTrace()
                }
            }

            post("/logout/all") {
                val userId = call.principal<AuthUser>()!!.userId

                try {
                    val result = sessionClient.get("/by-user/$userId") {
                        contentType(ContentType.Application.Json)
                    }

                    if (result.status == HttpStatusCode.OK) {
                        sessionClient.delete("/by-user/$userId")

                        val sessionIds = result.body<List<SessionID>>().map { it.id }

                        for (sessionId in sessionIds) {
                            val redisRefreshToken = rethis.getDel("session:$sessionId") ?: continue

                            val refreshTokenHash = jsonModule.decodeFromString(
                                RefreshTokenHash.serializer(),
                                redisRefreshToken
                            ).refreshTokenHash

                            rethis.del("refresh:$refreshTokenHash")
                        }

                        call.respond(
                            HttpStatusCode.OK,
                            ApiSuccess("Logged out everywhere.")
                        )
                    } else {
                        call.respond(
                            result.status,
                            result.body<ApiError>()
                        )
                    }
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError(e.message ?: e.localizedMessage)
                    )
                }
            }
        }
    }
}
