package com.aniflow.domain.controlplane.models

enum class SearchField {
    Anime,
    Season,
    Episode,
    Uploader,
    ReleaseGroup,
    Resolution,
    Codec,
    Audio,
    Subtitle,
    Source,
    Language,
    Year,
    Size,
    Seeders,
    DateRange,
    Provider
}

enum class ComparisonOperator {
    Equals,
    NotEquals,
    GreaterThan,
    GreaterThanOrEqual,
    LessThan,
    LessThanOrEqual,
    Contains,
    StartsWith,
    InList,
    NotInList
}

sealed interface SearchExpressionNode

data class ComparisonExpression(
    val field: SearchField,
    val operator: ComparisonOperator,
    val value: String
) : SearchExpressionNode

data class AndExpression(
    val children: List<SearchExpressionNode>
) : SearchExpressionNode

data class OrExpression(
    val children: List<SearchExpressionNode>
) : SearchExpressionNode

data class NotExpression(
    val child: SearchExpressionNode
) : SearchExpressionNode

/**
 * Advanced Search Expression (Section 12, 13).
 * Structured AST for expressing complex queries without hardcoded string concatenation.
 */
data class SearchExpression(
    val root: SearchExpressionNode,
    val name: String? = null
) {
    /**
     * Compiles the AST into an explainable textual query representation.
     */
    fun toQueryString(): String {
        return compileNode(root)
    }

    private fun compileNode(node: SearchExpressionNode): String {
        return when (node) {
            is ComparisonExpression -> "${node.field.name} ${operatorSymbol(node.operator)} \"${node.value}\""
            is AndExpression -> node.children.joinToString(" AND ", prefix = "(", postfix = ")") { compileNode(it) }
            is OrExpression -> node.children.joinToString(" OR ", prefix = "(", postfix = ")") { compileNode(it) }
            is NotExpression -> "NOT (${compileNode(node.child)})"
        }
    }

    private fun operatorSymbol(op: ComparisonOperator): String {
        return when (op) {
            ComparisonOperator.Equals -> "="
            ComparisonOperator.NotEquals -> "!="
            ComparisonOperator.GreaterThan -> ">"
            ComparisonOperator.GreaterThanOrEqual -> ">="
            ComparisonOperator.LessThan -> "<"
            ComparisonOperator.LessThanOrEqual -> "<="
            ComparisonOperator.Contains -> "CONTAINS"
            ComparisonOperator.StartsWith -> "STARTS_WITH"
            ComparisonOperator.InList -> "IN"
            ComparisonOperator.NotInList -> "NOT IN"
        }
    }
}
