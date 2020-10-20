package org.cqfn.diktat.ruleset.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.core.ast.ElementType.REFERENCE_EXPRESSION
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings.WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR
import org.cqfn.diktat.ruleset.utils.findAllNodesWithSpecificType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtProperty


/**
 * Rule check that never use the name of a variable in the custom getter or setter
 */
class PropertyAccessorFields(private val configRules: List<RulesConfig>) : Rule("getter-setter-fields") {

    private lateinit var emitWarn: ((offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit)
    private var isFixMode: Boolean = false

    override fun visit(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        isFixMode = autoCorrect
        emitWarn = emit

        if (node.elementType == PROPERTY_ACCESSOR)
            checkPropertyAccessor(node)
    }

    private fun checkPropertyAccessor(node: ASTNode) {
        val leftValue = node.treeParent.findChildByType(IDENTIFIER) ?: return
        val isUsedPropertyName = node.findAllNodesWithSpecificType(REFERENCE_EXPRESSION)
                .mapNotNull { it.findChildByType(IDENTIFIER) }
                .any { it.text == leftValue.text && it.treeParent.treeParent.elementType != DOT_QUALIFIED_EXPRESSION }
        val isContainLocalVarSameName = node.findChildByType(BLOCK)
                ?.let { blockNode ->
                    blockNode.getChildren(TokenSet.create(PROPERTY)).none { (it.psi as KtProperty).nameIdentifier?.text == leftValue.text }
                } ?: true
        if (isUsedPropertyName && isContainLocalVarSameName)
            WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR.warn(configRules, emitWarn, isFixMode, node.text, node.startOffset, node)
    }
}
