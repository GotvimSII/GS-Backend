package na.gotvimsii.microservices.recipems.database

/**
 * These are used for reading the scraped and formatted recipes and inserting them into the database.
 * They are not used to transfer the formatted recipe.
 */

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParsedIngredient(
    val item: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val section: String? = null
)

@Serializable
data class ParsedRecipe(
    val title: String,
    val ingredients: List<ParsedIngredient>,
    val instructions: List<String>,
    @SerialName("preparation_time") val preparationTime: Int? = null
)