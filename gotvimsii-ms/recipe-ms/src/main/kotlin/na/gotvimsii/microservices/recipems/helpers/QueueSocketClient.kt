package na.gotvimsii.microservices.recipems.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import na.gotvimsii.common.util.Environment
import na.gotvimsii.microservices.recipems.FoundRecipe
import na.gotvimsii.microservices.recipems.MatchedRecipe
import org.newsclub.net.unix.AFUNIXSocket
import org.newsclub.net.unix.AFUNIXSocketAddress
import kotlin.io.path.Path

const val UNIX_SOCKET_ADDRESS = "UNIX_SOCKET_ADDRESS"
const val SOCKET_ARGS = "SOCKET_ARGS"

sealed class SocketResponse {
    data class Ok(val payload: String) : SocketResponse()
    data class Err(val raw: String) : SocketResponse()
}

class QueueSocketClient(
    private val json: Json
) {
    private val socketFile = Path(Environment[UNIX_SOCKET_ADDRESS])
    private val programArgs = Environment[SOCKET_ARGS]

    suspend fun sendRequest(recipe: MatchedRecipe): FoundRecipe? = withContext(Dispatchers.IO) {
        AFUNIXSocket.newInstance().use { socket ->
            socket.connect(AFUNIXSocketAddress.of(socketFile), 5000)

            val recipeString = json.encodeToString(recipe)
            val requestString = StringBuilder().apply {
                append("run\n")
                append(recipeString.replace('\n', ' ') + '\n')
                append(programArgs)
                append("END\n")
            }.toString()
            val writer = socket.outputStream.bufferedWriter(Charsets.UTF_8)
            writer.write(requestString)
            writer.flush()

            val inStream = socket.inputStream
            val buf = ByteArray(4096)
            val result = StringBuilder()
            var read: Int
            while (inStream.read(buf).also { read = it } != -1) {
                val chunk = String(buf, 0, read)
                result.append(chunk)
            }

            handleResponse(result.toString())
        }
    }

    private fun parseSocketResponse(raw: String): SocketResponse {
        val trimmed = raw.trim()
        val lastNewline = trimmed.lastIndexOf('\n')

        if (lastNewline == -1) throw IllegalArgumentException("Unexpected format: $trimmed")

        val payload = trimmed.substring(0, lastNewline)
        return when (val tag = trimmed.substring(lastNewline + 1).trim()) {
            "ok" -> SocketResponse.Ok(payload)
            "err" -> SocketResponse.Err(payload)
            else -> throw IllegalArgumentException("Unknown tag: '$tag' bytes: ${tag.map { it.code }}")
        }
    }

    private fun handleResponse(raw: String): FoundRecipe? {
        return when (val response = parseSocketResponse(raw)) {
            is SocketResponse.Ok -> json.decodeFromString<FoundRecipe>(response.payload)
            is SocketResponse.Err -> {
                println("Server returned error: ${response.raw}")
                null
            }
        }
    }
}