package na.gotvimsii.microservices.recipems.helpers

import io.ktor.server.application.*
import na.gotvimsii.microservices.recipems.database.*
import na.gotvimsii.microservices.recipems.services
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

fun resolveOrCreateIngredient(name: String): Int {
    val normalized = normalize(name)

    val similarityExpr = IngredientAliasesTable.alias.similarity(normalized)

    val existingId = IngredientAliasesTable
        .select(IngredientAliasesTable.ingredientId, similarityExpr)
        .where { similarityExpr greater 0.6 }
        .orderBy(similarityExpr, SortOrder.DESC)
        .limit(1)
        .map { row -> row[IngredientAliasesTable.ingredientId].value }
        .firstOrNull()

    if (existingId != null) return existingId

    val ingredientId = IngredientsTable.insertAndGetId { statement ->
        statement[IngredientsTable.name] = normalized
    }.value

    IngredientAliasesTable.insert { statement ->
        statement[IngredientAliasesTable.alias] = normalized
        statement[IngredientAliasesTable.ingredientId] =
            EntityID(
                ingredientId,
                IngredientsTable
            )
    }

    return ingredientId
}

private fun importRecipes(recipes: List<ParsedRecipe>) {
    transaction {
        for (recipe in recipes) {
            val recipeId = RecipesTable.insertAndGetId { statement ->
                statement[RecipesTable.title] = recipe.title
                statement[RecipesTable.instructions] = recipe.instructions.joinToString(" ")
            }

            recipe.ingredients.forEach { ingredient ->
                val ingredientId = resolveOrCreateIngredient(ingredient.item)

                RecipesIngredientsTable.insertIgnore { statement ->
                    statement[RecipesIngredientsTable.recipeId] = recipeId
                    statement[RecipesIngredientsTable.ingredientId] =
                        EntityID(
                            ingredientId,
                            IngredientsTable
                        )
                    statement[RecipesIngredientsTable.quantity] = ingredient.quantity
                    statement[RecipesIngredientsTable.unit] = ingredient.unit
                    statement[RecipesIngredientsTable.section] = ingredient.section
                }
            }
        }
    }
}

internal fun Application.loadRecipesFromFile(path: String) {
    val json = services.recipeJson

    fun insertAndLog(batch: MutableList<ParsedRecipe>) {
        log.info("Processing batch of ${batch.size} lines")
        log.info("First: ${batch.first().title}")
        log.info("Last: ${batch.last().title}")
        importRecipes(batch)
        batch.clear()
    }

    File(path).bufferedReader().use { reader ->
        val batch = mutableListOf<ParsedRecipe>()

        reader.forEachLine { line ->
            try {
                val recipe = json.decodeFromString<ParsedRecipe>(line)
                batch.add(recipe)
            } catch (ex: Exception) {
                log.error("Error whilst parsing $line", ex)
            }

            if (batch.size >= 500) insertAndLog(batch)
        }

        if (batch.isNotEmpty()) insertAndLog(batch)
    }
}