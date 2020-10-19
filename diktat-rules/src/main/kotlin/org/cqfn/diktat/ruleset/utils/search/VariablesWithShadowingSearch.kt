package org.cqfn.diktat.ruleset.utils.search

import com.pinterest.ktlint.core.ast.ElementType
import org.cqfn.diktat.ruleset.utils.findAllNodesWithSpecificType
import org.cqfn.diktat.ruleset.utils.isGoingAfter
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty

class VariablesWithShadowingSearch(fileNode: ASTNode,
                                    filterForVariables: (KtProperty) -> Boolean): VariablesSearch(fileNode, filterForVariables) {

    override fun KtElement.getAllSearchResults(property: KtProperty): List<KtProperty> {
        return this.node.findAllNodesWithSpecificType(ElementType.PROPERTY)
                // filtering out all usages that are declared in the same context but are going before the variable declaration
                .filter { it.isGoingAfter(property.node) }
                .map { it.psi as KtProperty }
                .filter { it.nameAsName == property.nameAsName }
                .filterNot { it == property }
                .filter { shadows ->
                            // to exclude usages of local properties from other context (shadowed) and lambda arguments with same name
                            isReferenceToOtherVariableWithSameName(shadows, this, property)
                }
    }
}

// the default value for filtering condition is always true
fun ASTNode.findAllVariablesWithShadowing(filterForVariables: (KtProperty) -> Boolean = ::default) =
        VariablesWithShadowingSearch(this, filterForVariables).collectVariables()
