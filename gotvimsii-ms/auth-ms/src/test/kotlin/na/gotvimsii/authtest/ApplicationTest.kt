package na.gotvimsii.authtest

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import na.gotvimsii.common.classes.ApiError
import na.gotvimsii.common.classes.serializers.UUIDSerializer
import na.gotvimsii.microservices.authms.RegistrationCredentials
import na.gotvimsii.microservices.authms.module
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    val email = "nikdi@sigma.mail"
    val username = "nikdi"
    val password = "longpassw0rd"

    val json = Json {
        prettyPrint = true
        isLenient = true
    }

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        client.get("/ping").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun testSerializationInAppEnvironment() = testApplication {
        val randomId = UUID.randomUUID()

        application {
            this.install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                json(json)
            }
            this.routing {
                this.get("/serialization/test") {
                    call.respond(HttpStatusCode.OK, json.encodeToString(UUIDSerializer, randomId))
                }
            }
        }

        client = createClient {
            this.install(ContentNegotiation) {
                json(json)
            }
        }

        client.get("/serialization/test").apply {
            assertEquals(json.encodeToString(UUIDSerializer, randomId), this.bodyAsText())
        }
    }

    @Test
    fun testRegistration() = testApplication {
        application {
            module()
        }
        client = createClient {
            this.install(ContentNegotiation) {
                json(json)
            }
        }

        client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(RegistrationCredentials(email, username, password))
        }.apply {
            assertEquals(HttpStatusCode.Created, this.status)
        }
    }

    @Test
    fun testShortPasswordRegistration() = testApplication {
        application {
            module()
        }

        client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        val shortPassword = "sh0rt"

        client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(RegistrationCredentials(email, username, shortPassword))
        }.apply {
            assertEquals(ApiError("Password must be at least 10 characters long!"), this.body<ApiError>())
        }
    }

    @Test
    fun testInvalidEmailRegistration() = testApplication {
        application {
            module()
        }

        client = createClient {
            this.install(ContentNegotiation) {
                json(json)
            }
        }

        val invalidEmail = "bad@mail@tld"

        client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(RegistrationCredentials(invalidEmail, username, password))
        }.apply {
            assertEquals(ApiError("Email format is invalid!"), this.body<ApiError>())
        }
    }
}