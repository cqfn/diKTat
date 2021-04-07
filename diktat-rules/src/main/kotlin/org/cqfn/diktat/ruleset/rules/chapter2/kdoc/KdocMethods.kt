package org.cqfn.diktat.ruleset.rules.chapter2.kdoc

import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.getCommonConfiguration
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_TRIVIAL_KDOC_ON_FUNCTION
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_WITHOUT_PARAM_TAG
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_WITHOUT_RETURN_TAG
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_WITHOUT_THROWS_TAG
import org.cqfn.diktat.ruleset.constants.Warnings.MISSING_KDOC_ON_FUNCTION
import org.cqfn.diktat.ruleset.rules.DiktatRule
import org.cqfn.diktat.ruleset.utils.KotlinParser
import org.cqfn.diktat.ruleset.utils.appendNewlineMergingWhiteSpace
import org.cqfn.diktat.ruleset.utils.findAllDescendantsWithSpecificType
import org.cqfn.diktat.ruleset.utils.findChildAfter
import org.cqfn.diktat.ruleset.utils.getBodyLines
import org.cqfn.diktat.ruleset.utils.getFilePath
import org.cqfn.diktat.ruleset.utils.getFirstChildWithType
import org.cqfn.diktat.ruleset.utils.getIdentifierName
import org.cqfn.diktat.ruleset.utils.getRootNode
import org.cqfn.diktat.ruleset.utils.hasChildOfType
import org.cqfn.diktat.ruleset.utils.hasKnownKdocTag
import org.cqfn.diktat.ruleset.utils.hasTestAnnotation
import org.cqfn.diktat.ruleset.utils.insertTagBefore
import org.cqfn.diktat.ruleset.utils.isAccessibleOutside
import org.cqfn.diktat.ruleset.utils.isGetterOrSetter
import org.cqfn.diktat.ruleset.utils.isLocatedInTest
import org.cqfn.diktat.ruleset.utils.isOverridden
import org.cqfn.diktat.ruleset.utils.isStandardMethod
import org.cqfn.diktat.ruleset.utils.kDocTags
import org.cqfn.diktat.ruleset.utils.parameterNames
import org.cqfn.diktat.ruleset.utils.splitPathToDirs

import com.pinterest.ktlint.core.ast.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.CALLABLE_REFERENCE_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.COLLECTION_LITERAL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.COLON
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.KDOC_SECTION
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TAG_NAME
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TEXT
import com.pinterest.ktlint.core.ast.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.THROW
import com.pinterest.ktlint.core.ast.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.WHEN_CONDITION_WITH_EXPRESSION
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.util.stream.Collectors
import java.io.IOException

/**
 * This rule checks that whenever the method has arguments, return value, can throw exceptions,
 * KDoc block should contain `@param`, `@return`, `@throws`.
 * For `@return` check methods with explicit return type are supported and methods with inferred return
 * type are supported the following way: they should either declare return type `Unit` or have `@return` tag.
 * Currently only `throw` keyword from this methods body is supported for `@throws` check.
 */
@Suppress("ForbiddenComment")
class KdocMethods(configRules: List<RulesConfig>) : DiktatRule(
    "kdoc-methods",
    configRules,
    listOf(KDOC_TRIVIAL_KDOC_ON_FUNCTION, KDOC_WITHOUT_PARAM_TAG, KDOC_WITHOUT_RETURN_TAG,
        KDOC_WITHOUT_THROWS_TAG, MISSING_KDOC_ON_FUNCTION)) {
    /**
     * @param node
     * @param autoCorrect
     * @param emit
     */
    override fun logic(node: ASTNode) {
        if (node.elementType == FUN && node.getFirstChildWithType(MODIFIER_LIST).isAccessibleOutside() && !node.isOverridden()) {
            val config = configRules.getCommonConfiguration()
            val path = node.getRootNode().getFilePath()

            val dir = Paths.get(path).toAbsolutePath().toString().substringBeforeLast('\\')
            val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:.*$path")
            val pathList: List<Path?> = find(dir, matcher)
            val filePath = if (pathList.size == 1) pathList[0].toString() else ""

            val isTestMethod = node.hasTestAnnotation() || isLocatedInTest(filePath.splitPathToDirs(), config.testAnchors)
            if (!isTestMethod && !node.isStandardMethod() && !node.isSingleLineGetterOrSetter()) {
                checkSignatureDescription(node)
            }
        } else if (node.elementType == KDOC_SECTION) {
            checkKdocBody(node)
        }
    }

    private fun find(searchDirectory: String, matcher: PathMatcher): List<Path?> {
        try {
            Files.walk(Paths.get(searchDirectory)).use { files ->
                return files
                    .filter { path -> matcher.matches(path) }
                    .collect(Collectors.toList())
            }
        } catch (e: IOException) {
            return listOf()
        }
    }

    @Suppress("UnsafeCallOnNullableType", "AVOID_NULL_CHECKS")
    private fun checkSignatureDescription(node: ASTNode) {
        val kdoc = node.getFirstChildWithType(KDOC)
        val kdocTags = kdoc?.kDocTags()
        val name = node.getIdentifierName()!!.text

        val (missingParameters, kDocMissingParameters) = getMissingParameters(node, kdocTags)

        val explicitlyThrownExceptions = getExplicitlyThrownExceptions(node)
        val missingExceptions = explicitlyThrownExceptions
            .minus(kdocTags
                ?.filter { it.knownTag == KDocKnownTag.THROWS }
                ?.mapNotNull { it.getSubjectName() }
                ?.toSet() ?: emptySet()
            )

        val paramCheckFailed = (missingParameters.isNotEmpty() && !node.isSingleLineGetterOrSetter()) || kDocMissingParameters.isNotEmpty()
        val returnCheckFailed = hasReturnCheckFailed(node, kdocTags)
        val throwsCheckFailed = missingExceptions.isNotEmpty()

        val anyTagFailed = paramCheckFailed || returnCheckFailed || throwsCheckFailed
        // if no tag failed, we have too little information to suggest KDoc - it would just be empty
        if (kdoc == null && anyTagFailed) {
            addKdocTemplate(node, name, missingParameters, explicitlyThrownExceptions, returnCheckFailed)
        } else if (kdoc == null) {
            MISSING_KDOC_ON_FUNCTION.warn(configRules, emitWarn, false, name, node.startOffset, node)
        } else {
            if (paramCheckFailed) {
                handleParamCheck(node, kdoc, missingParameters, kDocMissingParameters, kdocTags)
            }
            if (returnCheckFailed) {
                handleReturnCheck(node, kdoc, kdocTags)
            }
            if (throwsCheckFailed) {
                handleThrowsCheck(node, kdoc, missingExceptions)
            }
        }
    }

    @Suppress("TYPE_ALIAS")
    private fun getMissingParameters(node: ASTNode, kdocTags: Collection<KDocTag>?): Pair<List<String?>, List<KDocTag>> {
        val parameterNames = node.parameterNames()
        val kdocParamList = kdocTags?.filter { it.knownTag == KDocKnownTag.PARAM && it.getSubjectName() != null }
        return if (parameterNames.isEmpty()) {
            Pair(emptyList(), kdocParamList ?: emptyList())
        } else if (kdocParamList != null && kdocParamList.isNotEmpty()) {
            Pair(parameterNames.minus(kdocParamList.map { it.getSubjectName() }), kdocParamList.filter { it.getSubjectName() !in parameterNames })
        } else {
            Pair(parameterNames.toList(), emptyList())
        }
    }

    private fun hasReturnCheckFailed(node: ASTNode, kdocTags: Collection<KDocTag>?): Boolean {
        if (node.isSingleLineGetterOrSetter()) {
            return false
        }

        val explicitReturnType = node.findChildAfter(COLON, TYPE_REFERENCE)
        val hasExplicitNotUnitReturnType = explicitReturnType != null && explicitReturnType.text != "Unit"
        val hasExplicitUnitReturnType = explicitReturnType != null && explicitReturnType.text == "Unit"
        val isFunWithExpressionBody = expressionBodyTypes.any { node.hasChildOfType(it) }
        val hasReturnKdoc = kdocTags != null && kdocTags.hasKnownKdocTag(KDocKnownTag.RETURN)
        return (hasExplicitNotUnitReturnType || isFunWithExpressionBody && !hasExplicitUnitReturnType) && !hasReturnKdoc
    }

    private fun getExplicitlyThrownExceptions(node: ASTNode): Set<String> {
        val codeBlock = node.getFirstChildWithType(BLOCK)
        val throwKeywords = codeBlock?.findAllDescendantsWithSpecificType(THROW)
        return throwKeywords
            ?.map { it.psi as KtThrowExpression }
            ?.mapNotNull { it.thrownExpression?.referenceExpression()?.text }
            ?.toSet()
            ?: emptySet()
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun handleParamCheck(node: ASTNode,
                                 kdoc: ASTNode?,
                                 missingParameters: Collection<String?>,
                                 kdocMissingParameters: List<KDocTag>,
                                 kdocTags: Collection<KDocTag>?) {
        kdocMissingParameters.forEach {
            KDOC_WITHOUT_PARAM_TAG.warn(configRules, emitWarn, false,
                "${it.getSubjectName()} param isn't present in argument list", it.node.startOffset,
                it.node)
        }
        if (missingParameters.isNotEmpty()) {
            KDOC_WITHOUT_PARAM_TAG.warnAndFix(configRules, emitWarn, isFixMode,
                "${node.getIdentifierName()!!.text} (${missingParameters.joinToString()})", node.startOffset, node) {
                val beforeTag = kdocTags?.find { it.knownTag == KDocKnownTag.RETURN }
                    ?: kdocTags?.find { it.knownTag == KDocKnownTag.THROWS }
                missingParameters.forEach {
                    kdoc?.insertTagBefore(beforeTag?.node) {
                        addChild(LeafPsiElement(KDOC_TAG_NAME, "@param"))
                        addChild(PsiWhiteSpaceImpl(" "))
                        addChild(LeafPsiElement(KDOC_TEXT, it))
                    }
                }
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun handleReturnCheck(node: ASTNode,
                                  kdoc: ASTNode?,
                                  kdocTags: Collection<KDocTag>?
    ) {
        KDOC_WITHOUT_RETURN_TAG.warnAndFix(configRules, emitWarn, isFixMode, node.getIdentifierName()!!.text,
            node.startOffset, node) {
            val beforeTag = kdocTags?.find { it.knownTag == KDocKnownTag.THROWS }
            kdoc?.insertTagBefore(beforeTag?.node) {
                addChild(LeafPsiElement(KDOC_TAG_NAME, "@return"))
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun handleThrowsCheck(node: ASTNode,
                                  kdoc: ASTNode?,
                                  missingExceptions: Collection<String>
    ) {
        KDOC_WITHOUT_THROWS_TAG.warnAndFix(configRules, emitWarn, isFixMode,
            "${node.getIdentifierName()!!.text} (${missingExceptions.joinToString()})", node.startOffset, node) {
            missingExceptions.forEach {
                kdoc?.insertTagBefore(null) {
                    addChild(LeafPsiElement(KDOC_TAG_NAME, "@throws"))
                    addChild(LeafPsiElement(KDOC_TEXT, " "))
                    addChild(LeafPsiElement(KDOC_TEXT, it))
                }
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun addKdocTemplate(node: ASTNode,
                                name: String,
                                missingParameters: Collection<String?>,
                                explicitlyThrownExceptions: Collection<String>,
                                returnCheckFailed: Boolean
    ) {
        MISSING_KDOC_ON_FUNCTION.warnAndFix(configRules, emitWarn, isFixMode, name, node.startOffset, node) {
            val kdocTemplate = "/**\n" +
                    (missingParameters.joinToString("") { " * @param $it\n" } +
                            (if (returnCheckFailed) " * @return\n" else "") +
                            explicitlyThrownExceptions.joinToString("") { " * @throws $it\n" } +
                            " */\n"
                    )
            val kdocNode = KotlinParser().createNode(kdocTemplate).findChildByType(KDOC)!!
            node.appendNewlineMergingWhiteSpace(node.firstChildNode, node.firstChildNode)
            node.addChild(kdocNode, node.firstChildNode)
        }
    }

    private fun checkKdocBody(node: ASTNode) {
        val kdocTextNodes = node.getChildren(TokenSet.create(KDOC_TEXT))
        if (kdocTextNodes.size == 1) {
            val kdocText = kdocTextNodes
                .first()
                .text
                .trim()
            if (kdocText.matches(uselessKdocRegex)) {
                KDOC_TRIVIAL_KDOC_ON_FUNCTION.warn(configRules, emitWarn, isFixMode, kdocText, kdocTextNodes.first().startOffset, node)
            }
        }
    }

    private fun ASTNode.isSingleLineGetterOrSetter() = isGetterOrSetter() && (expressionBodyTypes.any { hasChildOfType(it) } || getBodyLines().size == 1)

    companion object {
        // expression body of function can have a lot of 'ElementType's, this list might be not full
        private val expressionBodyTypes = setOf(BINARY_EXPRESSION, CALL_EXPRESSION, LAMBDA_EXPRESSION, REFERENCE_EXPRESSION,
            CALLABLE_REFERENCE_EXPRESSION, SAFE_ACCESS_EXPRESSION, WHEN_CONDITION_WITH_EXPRESSION, COLLECTION_LITERAL_EXPRESSION)
        private val uselessKdocRegex = """^([rR]eturn|[gGsS]et)[s]?\s+\w+(\s+\w+)?$""".toRegex()
    }
}
