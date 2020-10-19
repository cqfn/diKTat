package org.cqfn.diktat.ruleset.rules.identifiers

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.lineNumber
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings.LOCAL_VARIABLE_EARLY_DECLARATION
import org.cqfn.diktat.ruleset.utils.containsOnlyConstants
import org.cqfn.diktat.ruleset.utils.getDeclarationScope
import org.cqfn.diktat.ruleset.utils.lastLineNumber
import org.cqfn.diktat.ruleset.utils.search.findAllVariablesWithUsages
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset

/**
 * This rule checks that local variables are declared close to the point where they are first used.
 * Current algorithm assumes that scopes are always `BLOCK`s.
 * 1. Warns if there are statements between variable declaration and it's first usage
 * 2. It is allowed to declare variables in outer scope compared to usage scope. It could be useful to store state, e.g. between loop iterations.
 *
 * Current limitations due to usage of AST only:
 * * Only properties without initialization or initialized with expressions based on constants are supported.
 * * Properties initialized with constructor calls cannot be distinguished from method call and are no supported.
 */
class LocalVariablesRule(private val configRules: List<RulesConfig>) : Rule("local-variables") {
    companion object {
        private var functionInitializers = listOf(
                "emptyList", "emptySet", "emptyMap", "emptyArray", "emptySequence",
                "listOf", "setOf", "mapOf", "arrayOf", "arrayListOf",
                "mutableListOf", "mutableSetOf", "mutableMapOf",
                "linkedMapOf", "linkedSetOf"
        )
    }

    private lateinit var emitWarn: ((offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit)
    private var isFixMode: Boolean = false

    override fun visit(node: ASTNode,
                       autoCorrect: Boolean,
                       emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        emitWarn = emit
        isFixMode = autoCorrect

        if (node.elementType == FILE) {
            // collect all local properties and associate with corresponding references
            val propertiesToUsages = collectLocalPropertiesWithUsages(node)

            // find all usages which include more than one property
            val multiPropertyUsages = groupPropertiesByUsages(propertiesToUsages)

            multiPropertyUsages
                    .forEach { (statement, properties) ->
                        handleConsecutiveDeclarations(statement, properties)
                    }

            propertiesToUsages
                    .filterNot { it.key in multiPropertyUsages.values.flatten() }
                    .forEach { handleLocalProperty(it.key, it.value) }
        }
    }

    private fun collectLocalPropertiesWithUsages(node: ASTNode) = node
            .findAllVariablesWithUsages { propertyNode ->
                propertyNode.isLocal && propertyNode.name != null && propertyNode.parent is KtBlockExpression &&
                        (propertyNode.isVar && propertyNode.initializer == null ||
                        (propertyNode.initializer?.containsOnlyConstants() ?: false) ||
                        (propertyNode.initializer as? KtCallExpression).isWhitelistedMethod())
            }

            .filterNot { it.value.isEmpty() }

    private fun groupPropertiesByUsages(propertiesToUsages: Map<KtProperty, List<KtElement>>) = propertiesToUsages
            .mapValues { (property, usages) ->
                getFirstUsageStatementOrBlock(usages, property.getDeclarationScope())
            }
            .map { it.value to it.key }
            .groupByTo(mutableMapOf(), { it.first }) { it.second }
            .filter { it.value.size > 1 }
            .toMap<PsiElement, List<KtProperty>>()

    @Suppress("UnsafeCallOnNullableType")
    private fun handleLocalProperty(property: KtElement, usages: List<KtElement>) {
        require(property is KtProperty)
        val declarationScope = property.getDeclarationScope()

        val firstUsageStatementLine = getFirstUsageStatementOrBlock(usages, declarationScope).node.lineNumber()!!
        val firstUsage = usages.minBy { it.node.lineNumber()!! }!!
        checkLineNumbers(property, firstUsageStatementLine, firstUsageLine = firstUsage.node.lineNumber()!!)
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun handleConsecutiveDeclarations(statement: PsiElement, properties: List<KtProperty>) {
        // need to check that properties are declared consecutively with only maybe empty lines
        properties
                .sortedBy { it.node.lineNumber()!! }
                .zip(properties.size - 1 downTo 0)
                .forEach { (property, offset) ->
                    checkLineNumbers(property, statement.node.lineNumber()!!, offset)
                }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun checkLineNumbers(property: KtProperty, firstUsageStatementLine: Int, offset: Int = 0, firstUsageLine: Int? = null) {
        val numLinesToSkip = property
                .siblings(forward = true, withItself = false)
                .takeWhile { it is PsiWhiteSpace || it.node.isPartOfComment() }
                .let { siblings -> siblings.last().node.lastLineNumber()!! - siblings.first().node.lineNumber()!! - 1 }

        if (firstUsageStatementLine - numLinesToSkip != property.node.lastLineNumber()!! + 1 + offset) {
            LOCAL_VARIABLE_EARLY_DECLARATION.warn(configRules, emitWarn, isFixMode,
                    warnMessage(property.name!!, property.node.lineNumber()!!, firstUsageLine
                            ?: firstUsageStatementLine), property.startOffset, property.node)
        }
    }

    /**
     * Returns the [KtBlockExpression] with which a property should be compared.
     * @return either the line on which the property is used if it is first used in the same scope, or the block in the same scope as declaration
     */
    @Suppress("UnsafeCallOnNullableType")
    private fun getFirstUsageStatementOrBlock(usages: List<KtElement>, declarationScope: KtBlockExpression?): PsiElement {
        val firstUsage = usages.minBy { it.node.lineNumber()!! }!!
        val firstUsageScope = firstUsage.getParentOfType<KtBlockExpression>(true)

        return if (firstUsageScope == declarationScope) {
            // property is first used in the same scope where it is declared, we check line of statement where it is first used
            firstUsage
                    .parents
                    .find { it.parent == declarationScope }!!
        } else {
            // first usage is in deeper block compared to declaration, need to check how close is declaration to the first line of the block
            usages.minBy { it.node.lineNumber()!! }!!
                    .parentsWithSelf
                    .find { it.parent == declarationScope }!!
        }
    }

    private fun KtCallExpression?.isWhitelistedMethod() =
            this?.run {
                (referenceExpression() as KtNameReferenceExpression).getReferencedName() in functionInitializers &&
                        valueArguments.isEmpty()
            } ?: false

    private fun warnMessage(name: String, declared: Int, used: Int) = "<$name> is declared on line <$declared> and is used for the first time on line <$used>"
}
