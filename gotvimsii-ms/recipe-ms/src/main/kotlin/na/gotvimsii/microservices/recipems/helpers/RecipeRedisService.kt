package na.gotvimsii.microservices.recipems.helpers

import eu.vendeli.rethis.ReThis
import eu.vendeli.rethis.command.generic.expire
import eu.vendeli.rethis.command.list.lPop
import eu.vendeli.rethis.command.list.rPush
import eu.vendeli.rethis.shared.request.common.UpdateStrategyOption
import java.util.*
import kotlin.time.Duration.Companion.seconds

class RecipeRedisService(
    private val redis: ReThis
) {
    suspend fun storeObtainedRecipesInRedis(request: UUID, recipeIds: List<Int>) {
        redis.transaction {
            val key = "search:$request"
            for (id in recipeIds) {
                rPush(key, id.toString())
            }
            expire(key, 1800.seconds, UpdateStrategyOption.NX)
        }
    }

    suspend fun getNextStoredId(request: UUID): Int? {
        val idString = redis.lPop("search:$request") ?: return null
        return idString.toInt()
    }
}