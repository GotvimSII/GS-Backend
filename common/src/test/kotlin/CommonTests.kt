import kotlinx.serialization.json.Json
import na.gotvimsii.common.classes.serializers.UUIDSerializer
import na.gotvimsii.common.util.Environment
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonTests {

    @Test
    fun testEnvironment() {
        val envValue = "TEST_VALUE"
        assertEquals(Environment["TEST_VALUE"], envValue)
    } // set via run window

    @Test
    fun testCustomSerializationStandalone() {
        val json = Json {
            isLenient = true
            prettyPrint = true
        }
        val original = UUID.randomUUID()

        val encoded = json.encodeToString(serializer = UUIDSerializer, value = original)
        val decoded = json.decodeFromString(deserializer = UUIDSerializer, string = encoded)

        assertEquals(original, decoded, "JSON doesn't work as intended...")
    }
}