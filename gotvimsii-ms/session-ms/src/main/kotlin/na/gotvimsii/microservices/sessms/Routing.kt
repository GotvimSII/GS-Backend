package na.gotvimsii.microservices.sessms

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import na.gotvimsii.common.classes.*
import na.gotvimsii.microservices.sessms.database.SessionTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*
import kotlin.uuid.ExperimentalUuidApi

//TL TODO - get rid of user-facing messages; return enums or just plain codes;

fun Application.configureRouting() {
    routing {
        get("/ping") {
            call.respondText("pong :)")
        }

        post("/new") {
            val request = call.receiveNullable<NewSessionRequest>()
                ?: return@post call.respond(HttpStatusCode.BadRequest)

            try {
                val id = transaction {
                    SessionTable.insertAndGetId { record ->
                        record[userId] = request.userId
                        record[refreshTokenHash] = request.refreshTokenHash.toByteArray()
                        record[createdAt] = Instant.now()
                        record[updatedAt] = Instant.now()
                        record[expiresAt] = request.expiresAt
                        record[ipAddress] = request.ipAddress
                        record[userAgent] = request.userAgent
                        record[isRevoked] = false
                    }
                }

                call.respond(
                    HttpStatusCode.Created,
                    SessionID(id.value)
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiError(e.message ?: e.localizedMessage)
                )
                e.printStackTrace()
            }
        }

        patch("/{sessionId}") {
            val sessionId = call.pathParameters["sessionId"]?.let(UUID::fromString) ?: return@patch call.respond(
                HttpStatusCode.BadRequest,
                ApiError("No session ID provided!")
            )

            val request = runCatching { call.receive<UpdateSessionRequest>() }.getOrElse {
                return@patch call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("Invalid request provided!")
                )
            }

            try {
                val updated = transaction {
                    SessionTable.update({ SessionTable.id eq sessionId }) { record ->
                        record[refreshTokenHash] = request.refreshTokenHash.toByteArray()
                        record[updatedAt] = Instant.now()
                        record[expiresAt] = request.expiresAt
                        record[ipAddress] = request.ipAddress
                        record[userAgent] = request.userAgent
                    }
                }

                if (updated != 0) call.respond(
                    HttpStatusCode.OK,
                    ApiSuccess("Session updated.")
                ) else call.respond(
                    HttpStatusCode.NotFound,
                    ApiError("Session not found!")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiError(e.message ?: e.localizedMessage)
                )
                e.printStackTrace()
            }
        }

        delete("/{sessionId}") {
            val sessionId = call.pathParameters["sessionId"]?.let(UUID::fromString) ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                ApiError("No session ID provided!")
            )

            try {
                val revoked = transaction {
                    SessionTable.update(where = { SessionTable.id eq sessionId }) { record ->
                        record[isRevoked] = true
                        record[updatedAt] = Instant.now()
                        record[expiresAt] = Instant.now()
                    }
                }

                if (revoked != 0) call.respond(
                    HttpStatusCode.OK,
                    ApiSuccess("Session revoked.")
                ) else call.respond(
                    HttpStatusCode.NotFound,
                    ApiError("Session not found!")
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiError(e.message ?: e.localizedMessage)
                )
                e.printStackTrace()
            }
        }

        route("/by-user") {
            get("/{userId}") {
                val userId = call.pathParameters["userId"]?.let(UUID::fromString) ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("No user ID provided!")
                )

                try {
                    val sessionIds = transaction {
                        SessionTable.select(
                            column = SessionTable.id
                        ).where {
                            SessionTable.userId eq userId
                        }.map { record ->
                            SessionID(
                                id = record[SessionTable.id].value
                            )
                        }
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        sessionIds
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError(e.message ?: e.localizedMessage)
                    )
                    e.printStackTrace()
                }
            }

            delete("/{userId}") {
                val userId = call.pathParameters["userId"]?.let(UUID::fromString) ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiError("No user ID provided!")
                )

                try {
                    val revoked = transaction {
                        SessionTable.update({ SessionTable.userId eq userId }) { record ->
                            record[isRevoked] = true
                            record[updatedAt] = Instant.now()
                            record[expiresAt] = Instant.now()
                        }
                    }

                    if (revoked != 0) call.respond(
                        HttpStatusCode.OK,
                        ApiSuccess("All sessions revoked.")
                    ) else call.respond(
                        HttpStatusCode.NotFound,
                        ApiError("No active sessions found!")
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiError(e.message ?: e.localizedMessage)
                    )
                    e.printStackTrace()
                }
            }
        }
    }
}
