package org.cqfn.diktat.ruleset.rules.chapter6.classes

import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings.USE_DATA_CLASS
import org.cqfn.diktat.ruleset.rules.DiktatRule
import org.cqfn.diktat.ruleset.utils.*

import com.pinterest.ktlint.core.ast.ElementType.ABSTRACT_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.CLASS_INITIALIZER
import com.pinterest.ktlint.core.ast.ElementType.DATA_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.ENUM_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.INNER_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.OPEN_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.core.ast.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.SEALED_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_LIST
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

/**
 * This rule checks if class can be made as data class
 */
class DataClassesRule(configRules: List<RulesConfig>) : DiktatRule(
    "data-classes",
    configRules,
    listOf(USE_DATA_CLASS)) {
    override fun logic(node: ASTNode) {
        if (node.elementType == CLASS) {
            handleClass(node)
        }
    }

    private fun handleClass(node: ASTNode) {
        if (node.isDataClass()) {
            return
        }

        if (node.canBeDataClass()) {
            raiseWarn(node)
        }
    }

    // fixme: Need to know types of vars and props to create data class
    private fun raiseWarn(node: ASTNode) {
        USE_DATA_CLASS.warn(configRules, emitWarn, isFixMode, "${(node.psi as KtClass).name}", node.startOffset, node)
    }

    @Suppress(
        "UnsafeCallOnNullableType",
        "FUNCTION_BOOLEAN_PREFIX",
        "ComplexMethod"
    )
    private fun ASTNode.canBeDataClass(): Boolean {
        val isNotPropertyInClassBody = findChildByType(CLASS_BODY)?.let { (it.psi as KtClassBody).properties.isEmpty() } ?: true
        val constructorParametersNames: MutableList<String> = mutableListOf()
        val hasPropertyInConstructor = findChildByType(PRIMARY_CONSTRUCTOR)
            ?.let { constructor ->
                (constructor.psi as KtPrimaryConstructor)
                    .valueParameters
                    .onEach {
                        if (!it.hasValOrVar()) {
                            constructorParametersNames.add(it.name!!)
                        }
                    }
                    .run { isNotEmpty() && all { it.hasValOrVar() } }
            } ?: false
        if (isNotPropertyInClassBody && !hasPropertyInConstructor) {
            return false
        }
        // if parameter of the primary constructor is used in init block then it is hard to refactor this class to data class
        if (constructorParametersNames.isNotEmpty()) {
            val initBlocks = findChildByType(CLASS_BODY)?.getAllChildrenWithType(CLASS_INITIALIZER)
            initBlocks?.forEach { init ->
                val refExpressions = init.findAllDescendantsWithSpecificType(REFERENCE_EXPRESSION)
                if (refExpressions.any { it.text in constructorParametersNames }) {
                    return false
                }
            }
        }
        return hasAppropriateClassBody()
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun ASTNode.hasAppropriateClassBody(): Boolean {
        val classBody = getFirstChildWithType(CLASS_BODY)
        if (hasChildOfType(MODIFIER_LIST)) {
            val list = getFirstChildWithType(MODIFIER_LIST)!!
            return list.getChildren(null)
                .none { it.elementType in badModifiers } &&
                    classBody?.getAllChildrenWithType(FUN)
                        ?.isEmpty()
                        ?: false &&
                    getFirstChildWithType(SUPER_TYPE_LIST) == null
        }
        return classBody?.getAllChildrenWithType(FUN)?.isEmpty() ?: false &&
                getFirstChildWithType(SUPER_TYPE_LIST) == null &&
                // if there is any prop with logic in accessor then don't recommend to convert class to data class
                classBody?.let(::areGoodProps)
                    ?: true
    }

    /**
     * Checks if any property with accessor contains logic in accessor
     */
    private fun areGoodProps(node: ASTNode): Boolean {
        val propertiesWithAccessors = node.getAllChildrenWithType(PROPERTY).filter { it.hasChildOfType(PROPERTY_ACCESSOR) }

        if (propertiesWithAccessors.isNotEmpty()) {
            return propertiesWithAccessors.any {
                val accessors = it.getAllChildrenWithType(PROPERTY_ACCESSOR)

                areGoodAccessors(accessors)
            }
        }

        return true
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun areGoodAccessors(accessors: List<ASTNode>): Boolean {
        accessors.forEach {
            if (it.hasChildOfType(BLOCK)) {
                val block = it.getFirstChildWithType(BLOCK)!!

                return block
                    .getChildren(null)
                    .filter { expr -> expr.psi is KtExpression }
                    .count() <= 1
            }
        }

        return true
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun ASTNode.isDataClass(): Boolean {
        if (hasChildOfType(MODIFIER_LIST)) {
            val list = getFirstChildWithType(MODIFIER_LIST)!!
            return list.getChildren(null).any { it.elementType == DATA_KEYWORD }
        }
        return false
    }

    companion object {
        private val badModifiers = listOf(OPEN_KEYWORD, ABSTRACT_KEYWORD, INNER_KEYWORD, SEALED_KEYWORD, ENUM_KEYWORD)
    }
}
