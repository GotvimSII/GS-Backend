package na.gotvimsii.microservices.recipems.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object RecipesTable : IntIdTable(name = "recipes") {
    val title = text("title").index()
    val instructions = text("instructions").nullable()
    val preparationTime = integer("preparation_time").nullable()
    val titleVector = tsvector("title_vector")
}

object IngredientsTable : IntIdTable(name = "ingredients") {
    val name = varchar("name", 255).nullable()
}

object IngredientAliasesTable : Table(name = "ingredient_aliases") {
    val alias = varchar("alias", 255).uniqueIndex()
    val ingredientId = reference("ingredient_id", IngredientsTable.id)
    override val primaryKey = PrimaryKey(alias)
}

object RecipesIngredientsTable : Table(name = "recipes_ingredients") {
    val recipeId = reference("recipe_id", RecipesTable.id)
    val ingredientId = reference("ingredient_id", IngredientsTable.id)
    val quantity = double("quantity").nullable()
    val unit = varchar("unit", 32).nullable()
    val section = varchar("section", 96).nullable()

    override val primaryKey = PrimaryKey(recipeId, ingredientId)
}