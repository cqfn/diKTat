package org.cqfn.diktat.ruleset.rules.chapter3

import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings.COMPLEX_BOOLEAN_EXPRESSION
import org.cqfn.diktat.ruleset.rules.DiktatRule
import org.cqfn.diktat.ruleset.utils.KotlinParser
import org.cqfn.diktat.ruleset.utils.findAllDescendantsWithSpecificType
import org.cqfn.diktat.ruleset.utils.findAllNodesWithCondition
import org.cqfn.diktat.ruleset.utils.findLeafWithSpecificType
import org.cqfn.diktat.ruleset.utils.logicalInfixMethods

import com.bpodgursky.jbool_expressions.Expression
import com.bpodgursky.jbool_expressions.options.ExprOptions
import com.bpodgursky.jbool_expressions.parsers.ExprParser
import com.bpodgursky.jbool_expressions.rules.RulesHelper
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.PARENTHESIZED
import com.pinterest.ktlint.core.ast.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.core.ast.isLeaf
import com.pinterest.ktlint.core.ast.isPartOfComment
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

import java.lang.RuntimeException

/**
 * Rule that checks if the boolean expression can be simplified.
 */
class BooleanExpressionsRule(configRules: List<RulesConfig>) : DiktatRule(
    "boolean-expressions-rule",
    configRules,
    listOf(COMPLEX_BOOLEAN_EXPRESSION)) {
    override fun logic(node: ASTNode) {
        if (node.elementType == CONDITION) {
            checkBooleanExpression(node)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun checkBooleanExpression(node: ASTNode) {
        // This map is used to assign a variable name for every elementary boolean expression. It is required for jbool to operate.
        val mapOfExpressionToChar: HashMap<String, Char> = HashMap()
        val correctedExpression = formatBooleanExpressionAsString(node, mapOfExpressionToChar)
        if (mapOfExpressionToChar.isEmpty()) {
            // this happens, if we haven't found any expressions that can be simplified
            return
        }

        // If there are method calls in conditions
        val expr: Expression<String> = try {
            ExprParser.parse(correctedExpression)
        } catch (exc: RuntimeException) {
            if (exc.message?.startsWith("Unrecognized!") == true) {
                // this comes up if there is an unparsable expression (jbool doesn't have own exception type). For example a.and(b)
                return
            } else {
                throw exc
            }
        }
        val distributiveLawString = checkDistributiveLaw(expr, mapOfExpressionToChar, node)
        val simplifiedExpression = distributiveLawString?.let {
            ExprParser.parse(distributiveLawString)
        }
            ?: RulesHelper.applySet(expr, RulesHelper.demorganRules(), ExprOptions.noCaching())
        if (expr != simplifiedExpression) {
            COMPLEX_BOOLEAN_EXPRESSION.warnAndFix(configRules, emitWarn, isFixMode, "[${node.text}] to [$simplifiedExpression]", node.startOffset, node) {
                fixBooleanExpression(node, simplifiedExpression, mapOfExpressionToChar)
            }
        }
    }

    /**
     * Converts a complex boolean expression into a string representation, mapping each elementary expression to a letter token.
     * These tokens are collected into [mapOfExpressionToChar].
     * For example:
     * ```
     * (a > 5 && b != 2) -> A & B
     * (a > 5 || false) -> A | false
     * (a > 5 || x.foo()) -> A | B
     * ```
     *
     * @param node
     * @param mapOfExpressionToChar a mutable map for expression->token
     * @return formatted string representation of expression
     */
    @Suppress("UnsafeCallOnNullableType", "ForbiddenComment")
    internal fun formatBooleanExpressionAsString(node: ASTNode, mapOfExpressionToChar: HashMap<String, Char>): String {
        val (booleanBinaryExpressions, otherBinaryExpressions) = node.collectElementaryExpressions()
        val logicalExpressions = otherBinaryExpressions.filter {
            // keeping only boolean expressions, keeping things like `a + b < 6` and excluding `a + b`
            (it.psi as KtBinaryExpression).operationReference.text in logicalInfixMethods &&
                    // todo: support xor; for now skip all expressions that are nested in xor
                    it.parents().takeWhile { it != node }.none { (it.psi as? KtBinaryExpression)?.isXorExpression() ?: false }
        }
        // Boolean expressions like `a > 5 && b < 7` or `x.isEmpty() || (y.isNotEmpty())` we convert to individual parts.
        val elementaryBooleanExpressions = booleanBinaryExpressions
            .map { it.psi as KtBinaryExpression }
            .flatMap { listOf(it.left!!.node, it.right!!.node) }
            .map {
                // remove parentheses around expression, if there are any
                (it.psi as? KtParenthesizedExpression)?.expression?.node ?: it
            }
            .filterNot {
                // finally, if parts are binary expressions themselves, they should be present in our lists and we will process them later.
                // `true` and `false` are valid tokens for jBool, so we keep them.
                it.elementType == BINARY_EXPRESSION || it.text == "true" || it.text == "false"
            }
        var characterAsciiCode = 'A'.code  // `A` character in ASCII
        (logicalExpressions + elementaryBooleanExpressions).forEach { expression ->
            mapOfExpressionToChar.computeIfAbsent(expression.textWithoutComments().dropWhile {it == '!' }) {
                // Every elementary expression is assigned a single-letter variable.
                characterAsciiCode++.toChar()
            }
        }
        // Prepare final formatted string
        var correctedExpression = node.textWithoutComments()
        // At first, substitute all elementary expressions with variables
        mapOfExpressionToChar.forEach { (refExpr, char) ->
            correctedExpression = correctedExpression.replace(refExpr, char.toString())
        }
        // jBool library is using & as && and | as ||
        return "(${correctedExpression
            .replace("&&", "&")
            .replace("||", "|")})"
    }

    /**
     * Split the complex expression into elementary parts
     */
    private fun ASTNode.collectElementaryExpressions() = this
        .findAllNodesWithCondition({ astNode ->
            astNode.elementType == BINARY_EXPRESSION &&
                    // filter out boolean conditions in nested lambdas, e.g. `if (foo.filter { a && b })`
                    (astNode == this || astNode.parents().takeWhile { it != this }
                        .all { it.elementType in setOf(BINARY_EXPRESSION, PARENTHESIZED, PREFIX_EXPRESSION) })
        })
        .partition {
            val operationReferenceText = (it.psi as KtBinaryExpression).operationReference.text
            operationReferenceText == "&&" || operationReferenceText == "||"
        }

    private fun ASTNode.textWithoutComments() = findAllNodesWithCondition(withSelf = false, condition = {
        it.isLeaf()
    })
        .filterNot { it.isPartOfComment() }
        .joinToString(separator = "") { it.text }
        .replace("\n", " ")

    private fun fixBooleanExpression(
        node: ASTNode,
        simplifiedExpr: Expression<String>,
        mapOfExpressionToChar: HashMap<String, Char>) {
        var correctKotlinBooleanExpression = simplifiedExpr
            .toString()
            .replace("&", "&&")
            .replace("|", "||")
            .drop(1)  // dropping first (
            .dropLast(1)  // dropping last )
        mapOfExpressionToChar.forEach { (key, value) ->
            correctKotlinBooleanExpression = correctKotlinBooleanExpression.replace(value.toString(), key)
        }
        node.replaceChild(node.firstChildNode, KotlinParser().createNode(correctKotlinBooleanExpression))
    }

    /**
     * Checks if boolean expression can be simplified with distributive law.
     *
     * @return String? null if it cannot be simplified. Simplified string otherwise.
     */
    private fun checkDistributiveLaw(
        expr: Expression<String>,
        mapOfExpressionToChar: HashMap<String, Char>,
        node: ASTNode): String? {
        // checking that expression can be considered as distributive law
        val commonDistributiveOperand = getCommonDistributiveOperand(node, expr.toString(), mapOfExpressionToChar) ?: return null
        val correctSymbolsSequence = mapOfExpressionToChar.values.toMutableList()
        correctSymbolsSequence.remove(commonDistributiveOperand)
        correctSymbolsSequence.add(0, commonDistributiveOperand)
        val expressionsLogicalOperator = expr.toString().first { it == '&' || it == '|' }
        // we return expression depending on second operator
        return returnNeededDistributiveExpression(expressionsLogicalOperator, correctSymbolsSequence)
    }

    /**
     * Returns correct result string in distributive law
     */
    private fun returnNeededDistributiveExpression(firstLogicalOperator: Char, symbols: List<Char>): String {
        val secondSymbol = if (firstLogicalOperator == '&') '|' else '&'  // this is used to alter symbols
        val resultString = StringBuilder()
        symbols.forEachIndexed { index, symbol ->
            if (index == 0) {
                resultString.append("$symbol $firstLogicalOperator (")
            } else {
                resultString.append("$symbol $secondSymbol ")
            }
        }
        // remove last space and last operate
        return StringBuilder(resultString.dropLast(2)).append(")").toString()
    }

    /**
     * Method that checks that the expression can be simplified by distributive law.
     * Distributive law - A && B || A && C -> A && (B || C) or (A || B) && (A || C) -> A || (B && C)
     *
     * @return common operand for distributed law
     */
    private fun getCommonDistributiveOperand(
        node: ASTNode,
        expression: String,
        mapOfExpressionToChar: HashMap<String, Char>): Char? {
        val operationSequence = expression.filter { it == '&' || it == '|' }
        val numberOfOperationReferences = operationSequence.length
        // There should be three operands and three operation references in order to consider the expression
        // Moreover the operation references between operands should alternate.
        if (mapOfExpressionToChar.size < DISTRIBUTIVE_LAW_MIN_EXPRESSIONS ||
                numberOfOperationReferences < DISTRIBUTIVE_LAW_MIN_OPERATIONS ||
                !isSequenceAlternate(operationSequence)) {
            return null
        }
        return if (operationSequence.first() == '&') {
            getCommonOperand(expression, '|', '&')
        } else {
            // this is done for excluding A || B && A || C without parenthesis.
            val parenthesizedExpressions = node.findAllNodesWithCondition({ it.elementType == PARENTHESIZED })
            parenthesizedExpressions.forEach {
                it.findLeafWithSpecificType(OPERATION_REFERENCE) ?: run {
                    return null
                }
            }
            getCommonOperand(expression, '&', '|')
        }
    }

    private fun isSequenceAlternate(seq: String) = seq.zipWithNext().all { it.first != it.second }

    /**
     * This method returns common operand in distributive law.
     * We need common operand for special case, when the first expression is not common.
     * For example: (some != null && a) || (a && c) || (a && d). When the expressions are mapped to `char`s, `some != null` points to `A` character
     */
    private fun getCommonOperand(
        expression: String,
        firstSplitDelimiter: Char,
        secondSplitDelimiter: Char): Char? {
        val expressions = expression.split(firstSplitDelimiter)
        val listOfPairs: MutableList<List<String>> = mutableListOf()
        expressions.forEach { expr ->
            listOfPairs.add(expr.filterNot { it == ' ' || it == '(' || it == ')' }.split(secondSplitDelimiter))
        }
        val firstOperands = listOfPairs.first()
        listOfPairs.removeFirst()
        return when {
            listOfPairs.all { it.contains(firstOperands.first()) } -> firstOperands.first().first()
            listOfPairs.all { it.contains(firstOperands.last()) } -> firstOperands.last().first()
            else -> null
        }
    }

    private fun KtBinaryExpression.isXorExpression() = operationReference.text == "xor"

    companion object {
        const val DISTRIBUTIVE_LAW_MIN_EXPRESSIONS = 3
        const val DISTRIBUTIVE_LAW_MIN_OPERATIONS = 3
    }
}
