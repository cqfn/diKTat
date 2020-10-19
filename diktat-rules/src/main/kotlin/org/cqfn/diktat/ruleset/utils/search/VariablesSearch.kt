package org.cqfn.diktat.ruleset.utils.search

import com.pinterest.ktlint.core.ast.ElementType
import org.cqfn.diktat.ruleset.utils.findAllNodesWithSpecificType
import org.cqfn.diktat.ruleset.utils.getDeclarationScope
import org.cqfn.diktat.ruleset.utils.isGoingAfter
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression

/**
 *  @param node - root node of a type File that is used to search all declared properties (variables)
 *  it should be ONLY node of File elementType
 *  @param filterForVariables - condition to filter
 */
abstract class VariablesSearch(val node: ASTNode, private val filterForVariables: (KtProperty) -> Boolean) {

    /**
     * to complete implementation of a search mechanism you need to specify what and how you will search in current scope
     * [this] - scope where to search the usages/assignments/e.t.c of the variable (can be of types KtBlockExpression/KtFile/KtClassBody)
     */
    protected abstract fun KtElement.getAllSearchResults(property: KtProperty): List<KtElement>

    /**
     * method collects all declared variables and it's usages
     *
     * @return a map of a property to it's usages
     */
    fun collectVariables(): Map<KtProperty, List<KtElement>> {
        require(node.elementType == ElementType.FILE) {
            "To collect all variables in a file you need to provide file root node"
        }
        return node
                .findAllNodesWithSpecificType(ElementType.PROPERTY)
                .map { it.psi as KtProperty }
                .filter(filterForVariables)
                .associateWith { it.getSearchResults() }
    }

    @Suppress("UnsafeCallOnNullableType")
    fun KtProperty.getSearchResults(): List<KtElement> {
        return this
                .getDeclarationScope()
                // if declaration scope is not null - then we have found out the block where this variable is stored
                // else - it is a global variable on a file level or a property on the class level
                .let { declarationScope ->
                    // searching in the scope with declaration (in the context)
                    declarationScope?.getAllSearchResults(this)
                    // searching on the class level in class body
                            ?: (this.getParentOfType<KtClassBody>(true)?.getAllSearchResults(this))
                            // searching on the file level
                            ?: (this.getParentOfType<KtFile>(true)!!.getAllSearchResults(this))
                }
    }

    /**
     * filtering object's fields (expressions) that have same name as variable
     */
    protected fun KtNameReferenceExpression.isReferenceToFieldOfObject(): Boolean {
        val expression = this
        return (expression.parent as? KtDotQualifiedExpression)?.run {
            receiverExpression != expression && selectorExpression?.referenceExpression() == expression
        } ?: false
    }

    /**
     * filtering local properties from other context (shadowed) and lambda and function arguments with same name
     *  going through all parent scopes from bottom to top until we will find the scope where the initial variable was declared
     *  all these scopes are on lower level of inheritance that's why if in one of these scopes we will find any
     *  variable declaration with the same name - we will understand that it is usage of another variable
     */
    protected fun isReferenceToOtherVariableWithSameName(expression: KtElement,
                                                         codeBlock: KtElement, property: KtProperty): Boolean {
        return expression.parents
                // getting all block expressions/class bodies/file node from bottom to the top
                // FixMe: Object companion is not resolved properly yet
                .filter { it is KtBlockExpression || it is KtClassBody || it is KtFile }
                // until we reached the block that contains the initial declaration
                .takeWhile { codeBlock != it }
                .any { block ->
                    // this is not the expression that we needed if:
                    //  1) there is a new shadowed declaration for this expression (but the declaration should stay on the previous line!)
                    //  2) or there one of top blocks is a function/lambda that has arguments with the same name
                    // FixMe: in class or a file the declaration can easily go after the usage (by lines of code)
                    block.getChildrenOfType<KtProperty>().any { it.nameAsName == property.nameAsName && expression.node.isGoingAfter(it.node) } ||
                            block.parent
                                    .let { it as? KtFunctionLiteral }
                                    ?.valueParameters
                                    ?.any { it.nameAsName == property.nameAsName }
                            ?: false
                    // FixMe: also see very strange behavior of Kotlin in tests (disabled)
                }
    }
}

/**
 * this is a small workaround in case we don't want to make any custom filter while searching variables
 */
@SuppressWarnings("FunctionOnlyReturningConstant")
fun default(node: KtProperty) = true
