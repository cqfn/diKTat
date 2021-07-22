package org.cqfn.diktat.ruleset.rules.chapter2.comments

import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.ListOfPairs
import org.cqfn.diktat.ruleset.constants.Warnings.COMMENTED_OUT_CODE
import org.cqfn.diktat.ruleset.rules.DiktatRule
import org.cqfn.diktat.ruleset.utils.findAllDescendantsWithSpecificType

import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.prevSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.TokenType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath

/**
 * This rule performs checks if there is any commented code.
 * No commented out code is allowed, including imports.
 */
@Suppress("ForbiddenComment")
class CommentsRule(configRules: List<RulesConfig>) : DiktatRule(
    "comments",
    configRules,
    listOf(COMMENTED_OUT_CODE)) {
    private lateinit var ktPsiFactory: KtPsiFactory

    override fun logic(node: ASTNode) {
        ktPsiFactory = KtPsiFactory(node.psi.project, false)  // regarding markGenerated see KDoc in Kotlin sources
        if (node.elementType == FILE) {
            checkCommentedCode(node)
        }
    }

    /**
     * This method tries to detect commented code by uncommenting and parsing it. If parser reports errors,
     * we assume this is not code (it is hard to try and parse further because comments can contain code snippets).
     *
     * @implNote
     * 1. Import and package directives should be separated from the rest of uncommented lines
     * 2. Import usually go on top of the file, so if comment contains not only imports, it probably contains imports only on top. (this possibly needs fixme)
     * 3. Code can be surrounded by actual comments. One possible heuristic here is to assume actual comments start
     *    with '// ' with whitespace, while automatic commenting in, e.g., IDEA creates slashes in the beginning of the line
     *
     */
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    private fun checkCommentedCode(node: ASTNode) {
        val errorNodesWithText: ListOfPairs = mutableListOf()
        val eolCommentsOffsetToText = getOffsetsToTextBlocksFromEolComments(node, errorNodesWithText)
        val blockCommentsOffsetToText = node
            .findAllDescendantsWithSpecificType(BLOCK_COMMENT)
            .map {
                errorNodesWithText.add(it to it.text.trim().removeSurrounding("/*", "*/"))
                it.startOffset to it.text.trim().removeSurrounding("/*", "*/")
            }
        (eolCommentsOffsetToText + blockCommentsOffsetToText)
            .flatMap { (offset, text) ->
                val (singleLines, blockLines) = text.lines().partition { it.contains(importOrPackage) }
                val block = if (blockLines.isNotEmpty()) listOf(blockLines.joinToString("\n")) else emptyList()
                (singleLines + block).map {
                    offset to it
                }
            }
            .mapNotNull { (offset, text) ->
                when {
                    text.contains(importKeyword) ->
                        offset to ktPsiFactory.createImportDirective(ImportPath.fromString(text.substringAfter("$importKeyword "))).node
                    text.contains(packageKeyword) ->
                        offset to ktPsiFactory.createPackageDirective(FqName(text.substringAfter("$packageKeyword "))).node
                    else -> if (isRequirePartOfCode(text)) {
                        offset to ktPsiFactory.createBlockCodeFragment(text, null).node
                    } else {
                        null
                    }
                }
            }
            .filter { (_, parsedNode) ->
                parsedNode
                    .findAllDescendantsWithSpecificType(TokenType.ERROR_ELEMENT)
                    .isEmpty()
            }
            .forEach { (offset, parsedNode) ->
                COMMENTED_OUT_CODE.warn(configRules, emitWarn, isFixMode, parsedNode.text.substringBefore("\n").trim(), offset,
                    errorNodesWithText.find { it.second == parsedNode.text }?.first ?: parsedNode)
            }
    }

    /**
     * This method is used to extract text from EOL comments in a form which can be used for parsing.
     * Multiple consecutive EOL comments can correspond to one code block, so we try to glue them together here.
     * Splitting back into lines, if necessary, will be done outside of this method, for both text from EOL and block.
     * fixme: in this case offset is lost for lines which will be split once more
     */
    private fun getOffsetsToTextBlocksFromEolComments(node: ASTNode, errorNodesWithText: ListOfPairs): List<Pair<Int, String>> {
        val comments = node
            .findAllDescendantsWithSpecificType(EOL_COMMENT)
            .filter { !it.text.contains(eolCommentStart) || isCodeAfterCommentStart(it.text) }
        return if (comments.isNotEmpty()) {
            val result = mutableListOf(mutableListOf(comments.first()))
            comments
                .drop(1)
                .fold(result) { acc, astNode ->
                    val isImportOrPackage = astNode.text.contains(importOrPackage)
                    val previousNonWhiteSpaceNode = astNode.prevSibling { it.elementType != WHITE_SPACE }
                    if (!isImportOrPackage && previousNonWhiteSpaceNode in acc.last()) {
                        acc.last().add(astNode)
                    } else {
                        acc.add(mutableListOf(astNode))
                    }
                    acc
                }
                .map { list ->
                    list.forEach { errorNodesWithText.add(it to it.text.removePrefix("//")) }
                    list.first().startOffset to list.joinToString("\n") { it.text.removePrefix("//") }
                }
        } else {
            emptyList()
        }
    }

    /**
     * This is a very rare case. We should check this cases for 4 things:
     *
     * 1. If it is a class/object at the beginning of the line
     * 2. If it is a function
     * 3. If it is import/package implementation
     * 4. If it is }. This case is used when } goes after one space and it is closing class or fun
     */
    private fun isCodeAfterCommentStart(text: String): Boolean {
        val textWithoutCommentStartToken = text.removePrefix("//").trim()
        return codeFileStartCases.any { textWithoutCommentStartToken.contains(it) }
    }

    private fun isRequirePartOfCode(text: String): Boolean =
            text.contains("val", true) || text.contains("var", true) || text.contains("=", true) || (text.contains("{", true) && text.substringAfter("{").contains("}", true))

    @Suppress("MaxLineLength")
    companion object {
        private val importKeyword = KtTokens.IMPORT_KEYWORD.value
        private val packageKeyword = KtTokens.PACKAGE_KEYWORD.value
        private val importOrPackage = """($importKeyword|$packageKeyword) """.toRegex()
        private val classRegex =
                """^\s*(public|private|protected)*\s*(internal)*\s*(open|data|sealed)*\s*(internal)*\s*(class|object)\s+(\w+)(\(.*\))*(\s*:\s*\w+(\(.*\))*)?\s*\{*$""".toRegex()
        private val importOrPackageRegex = """^(import|package)?\s+([a-zA-Z.])+;*$""".toRegex()
        private val functionRegex = """^(public|private|protected)*\s*(override|abstract|actual|expect)*\s?fun\s+\w+(\(.*\))?(\s*:\s*\w+)?\s*[{=]${'$'}""".toRegex()
        private val rightBraceRegex = """^\s*}$""".toRegex()
        private val codeFileStartCases = listOf(classRegex, importOrPackageRegex, functionRegex, rightBraceRegex)
        private val eolCommentStart = """// \S""".toRegex()
    }
}
