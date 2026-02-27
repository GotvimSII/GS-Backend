package na.gotvimsii.microservices.recipems

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import na.gotvimsii.common.classes.serializers.UUIDSerializer
import na.gotvimsii.microservices.recipems.helpers.IngredientMatchMode
import java.util.UUID

@Serializable
data class RecipeName(val name: String)

@Serializable
data class IngredientRequest(
    val ingredients: List<String>,
    val mode: IngredientMatchMode
)

@Serializable
data class RecipeResponse(
    val recipe: MatchedRecipe,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("request_id")
    val requestId: UUID
)

@Serializable
data class MatchedRecipe(
    val id: Int,
    val title: String,
    val preparationTime: Int?,
    val ingredients: String,
    val instructions: String?
)

@Serializable
data class NoRecipeFound(
    val message: String
)