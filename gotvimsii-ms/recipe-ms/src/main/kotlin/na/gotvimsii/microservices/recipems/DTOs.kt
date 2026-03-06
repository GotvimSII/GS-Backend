package na.gotvimsii.microservices.recipems

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import na.gotvimsii.common.classes.serializers.UUIDSerializer
import na.gotvimsii.microservices.recipems.helpers.IngredientMatchMode
import java.util.*

@Serializable
data class NameRequest(
    val name: String,
    val portions: Int
)

@Serializable
data class IngredientRequest(
    val ingredients: List<String>,
    val mode: IngredientMatchMode,
    val portions: Int
)

@Serializable
data class RecipeResponse(
    val recipe: FoundRecipe,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("request_id")
    val requestId: UUID
)

@Serializable
data class MatchedRecipe(
    val id: Int,
    val title: String,
    val portions: Int,
    val preparationTime: Int?,
    val ingredients: String,
    val instructions: String?
)

@Serializable
data class NoRecipeFound(
    val message: String
)

@Serializable
data class RecipeIngredient(
    val name: String,
    val quantity: Double?,
    val unit: String?,
    val section: String? = null
)

@Serializable
data class RecipeInstruction(
    val order: Int,
    val text: String
)

@Serializable
data class FoundRecipe(
    val title: String,
    val portions: Int,
    val ingredients: List<RecipeIngredient>,
    val instructions: List<RecipeInstruction>,
    @SerialName("preparation_time") val preparationTime: Int? = null
)