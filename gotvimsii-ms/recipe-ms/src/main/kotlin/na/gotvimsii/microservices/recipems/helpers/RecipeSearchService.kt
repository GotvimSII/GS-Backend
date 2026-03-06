package na.gotvimsii.microservices.recipems.helpers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import na.gotvimsii.microservices.recipems.MatchedRecipe
import na.gotvimsii.microservices.recipems.database.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.Logger

sealed class RecipeSearchResult {
    data class Found(val ids: List<Int>) : RecipeSearchResult()
    data class Error(val message: String) : RecipeSearchResult()
    object NotFound : RecipeSearchResult()
}

@Serializable
enum class IngredientMatchMode {
    @SerialName("all")
    CONTAINS_ALL,

    @SerialName("exact")
    EXACT_MATCH
}

class RecipeSearchService(
    private val log: Logger
) {
    fun searchByName(name: String): RecipeSearchResult {
        val normalized = normalize(name)

        return try {
            transaction {
                val exact = exactNameQuery(normalized)

                if (exact.isNotEmpty()) {
                    log.info("Found exact matches: ${exact.joinToString(", ")}")
                    return@transaction RecipeSearchResult.Found(exact)
                }

                val fullText = fullTextSearchQuery(normalized)

                if (fullText.isNotEmpty()) {
                    log.info("Found full text searches: ${fullText.joinToString(", ")}")
                    return@transaction RecipeSearchResult.Found(fullText)
                }

                val similar = trigramSimilarityQuery(normalized)

                if (similar.isNotEmpty()) {
                    log.info("Found similar searches: ${similar.joinToString(", ")}")
                    RecipeSearchResult.Found(similar)
                } else RecipeSearchResult.NotFound
            }
        } catch (e: Exception) {
            e.printStackTrace()
            RecipeSearchResult.Error(e.toString())
        }
    }

    fun searchByIngredients(ingredients: List<String>, matchMode: IngredientMatchMode): RecipeSearchResult {
        return try {
            transaction {
                val ingredientIds = resolveIngredients(ingredients)

                if (ingredientIds.isEmpty()) return@transaction RecipeSearchResult.NotFound

                val results = when (matchMode) {
                    IngredientMatchMode.EXACT_MATCH -> exactIngredientMatchQuery(ingredientIds)
                    IngredientMatchMode.CONTAINS_ALL -> containsAllIngredientsQuery(ingredientIds)
                }

                if (results.isNotEmpty()) {
                    log.info("Found results: ${results.joinToString(", ")}")
                    RecipeSearchResult.Found(results)
                } else RecipeSearchResult.NotFound
            }
        } catch (e: Exception) {
            e.printStackTrace()
            RecipeSearchResult.Error(e.toString())
        }
    }

    fun getRecipeWithIngredients(recipeId: Int, portions: Int): MatchedRecipe = transaction {
        val quantity = coalesce(
            concat(
                RecipesIngredientsTable.quantity.castToText().nullIf(stringLiteral("")),
                stringLiteral(" ")
            ),
            stringLiteral("")
        )

        val unit = coalesce(
            concat(
                RecipesIngredientsTable.unit.castToText().nullIf(stringLiteral("")),
                stringLiteral(" ")
            ),
            stringLiteral("")
        )

        val ingredient = concat(quantity, unit, IngredientsTable.name.castTo(TextColumnType()))

        val ingredientsAgg = ingredient.stringAgg(", ")

        RecipesTable
            .innerJoin(RecipesIngredientsTable)
            .innerJoin(IngredientsTable)
            .select(
                RecipesTable.id,
                RecipesTable.title,
                RecipesTable.preparationTime,
                ingredientsAgg,
                RecipesTable.instructions
            ).where { RecipesTable.id eq recipeId }
            .groupBy(RecipesTable.id)
            .map { row ->
                MatchedRecipe(
                    id = row[RecipesTable.id].value,
                    title = row[RecipesTable.title],
                    portions = portions,
                    preparationTime = row[RecipesTable.preparationTime],
                    ingredients = row[ingredientsAgg],
                    instructions = row[RecipesTable.instructions]
                )
            }
            .first()
    }

    private fun exactNameQuery(name: String): List<Int> {
        return RecipesTable
            .select(RecipesTable.id)
            .where { RecipesTable.title.lowerCase() eq name }
            .orderBy(RecipesTable.id, SortOrder.DESC)
            .map { row -> row[RecipesTable.id].value }
    }

    private fun fullTextSearchQuery(name: String): List<Int> {
        val tsQuery = plainToTsQuery("simple", name)
        val rankExpr = RecipesTable.titleVector.tsRank(tsQuery)

        return RecipesTable
            .select(RecipesTable.id, rankExpr)
            .where { RecipesTable.titleVector.matches(tsQuery) }
            .orderBy(rankExpr, SortOrder.DESC)
            .map { row -> row[RecipesTable.id].value }
    }

    private fun trigramSimilarityQuery(name: String): List<Int> {
        val similarityExpr = RecipesTable.title.similarity(name)

        return RecipesTable
            .select(RecipesTable.id, similarityExpr)
            .where { similarityExpr greater 0.35 }
            .orderBy(similarityExpr, SortOrder.DESC)
            .map { row -> row[RecipesTable.id].value }
    }

    private fun resolveIngredients(ingredientNames: List<String>): List<Int> = ingredientNames.map(::resolveOrCreateIngredient)

    private fun containsAllIngredientsQuery(ingredientIds: List<Int>): List<Int> {
        val countExpression = RecipesIngredientsTable.ingredientId.countDistinct()

        return RecipesIngredientsTable
            .select(RecipesIngredientsTable.recipeId, countExpression)
            .where { RecipesIngredientsTable.ingredientId inList ingredientIds }
            .groupBy(RecipesIngredientsTable.recipeId)
            .having { countExpression eq ingredientIds.size.toLong() }
            .orderBy(countExpression, SortOrder.DESC)
            .map { row -> row[RecipesIngredientsTable.recipeId].value }
    }

    private fun exactIngredientMatchQuery(ingredientIds: List<Int>): List<Int> {
        val candidates = containsAllIngredientsQuery(ingredientIds)
        if (candidates.isEmpty()) return emptyList()

        val totalCount = RecipesIngredientsTable.ingredientId.countDistinct()

        return RecipesIngredientsTable
            .select(RecipesIngredientsTable.recipeId, totalCount)
            .where { RecipesIngredientsTable.recipeId inList candidates }
            .groupBy(RecipesIngredientsTable.recipeId)
            .having { totalCount eq ingredientIds.size.toLong() }
            .map { row -> row[RecipesIngredientsTable.recipeId].value }
    }
}