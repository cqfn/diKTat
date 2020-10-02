package org.cqfn.diktat.ruleset.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.BODY
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.IF
import com.pinterest.ktlint.core.ast.ElementType.THEN
import com.pinterest.ktlint.core.ast.ElementType.WHEN
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.parent
import org.cqfn.diktat.common.config.rules.RuleConfiguration
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.getRuleConfig
import org.cqfn.diktat.ruleset.constants.Warnings.NESTED_BLOCK
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtClass

class NestedFunctionBlock(private val configRules: List<RulesConfig>) : Rule("nested-block") {

    companion object {
        private const val MAX_NESTED_BLOCK_COUNT = 4L
    }

    private lateinit var emitWarn: ((offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit)
    private var isFixMode: Boolean = false

    override fun visit(node: ASTNode,
                       autoCorrect: Boolean,
                       emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        emitWarn = emit
        isFixMode = autoCorrect

        val configuration = NestedBlockConfiguration(
                configRules.getRuleConfig(NESTED_BLOCK)?.configuration ?: mapOf()
        )

        if (node.elementType == FUN)
            checkFunForNestedBlocks(node, configuration.maxNestedBlockQuantity)
    }

    private fun checkFunForNestedBlocks(node: ASTNode, maxNestedBlockCount: Long) {
        val classParentNode = node.parent({it.elementType == CLASS})
        if (classParentNode != null && (classParentNode.psi as KtClass).isLocal) return
        node.getChildren(TokenSet.create(BLOCK)).forEach {
            dfsBlock(it, node,1, maxNestedBlockCount)
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun dfsBlock(node: ASTNode, initialNode: ASTNode,  blockCount: Int, maxNestedBlockCount: Long) {
        if (blockCount > maxNestedBlockCount) {
            NESTED_BLOCK.warn(configRules, emitWarn, isFixMode, initialNode.findChildByType(IDENTIFIER)!!.text,
                    initialNode.startOffset, initialNode)
            return
        } else {
            findBlocks(node).forEach {
                dfsBlock(it, initialNode,blockCount + 1, maxNestedBlockCount)
            }
        }
    }

    private fun findBlocks(node: ASTNode): List<ASTNode> {
        val result = mutableListOf<ASTNode>()
        node.getChildren(null).forEach {
            when (it.elementType) {
                IF -> Pair(it.findChildByType(THEN)?.findChildByType(BLOCK), it.findChildByType(ELSE)?.findChildByType(BLOCK))
                WHEN -> Pair(it, null)
                WHEN_ENTRY -> Pair(it.findChildByType(BLOCK), null)
                FUN -> Pair(it.findChildByType(BLOCK), null)
                else -> Pair(it.findChildByType(BODY)?.findChildByType(BLOCK), null)
            }.let { pair ->
                pair.let {
                    pair.first?.let { it1 -> result.add(it1) }
                    pair.second?.let { it2 -> result.add(it2) }
                }
            }
        }
        return result
    }

    class NestedBlockConfiguration(config: Map<String, String>) : RuleConfiguration(config) {
        val maxNestedBlockQuantity = config["maxNestedBlockQuantity"]?.toLong() ?: MAX_NESTED_BLOCK_COUNT
    }
}
