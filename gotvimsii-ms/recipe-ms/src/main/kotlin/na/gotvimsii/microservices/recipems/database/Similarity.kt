package na.gotvimsii.microservices.recipems.database

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.Function

class Similarity(
    val expr: Expression<String>,
    val other: Expression<String>
) : Function<Double>(DoubleColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("similarity(")
        append(expr)
        append(", ") // using a version with space for when debugging is needed
        append(other)
        append(')')
    }
}

fun Expression<String>.similarity(other: String) = Similarity(this, stringLiteral(other))
fun Expression<String>.similarity(other: Expression<String>) = Similarity(this, other)
// two versions because why not