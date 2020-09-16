package org.cqfn.diktat.ruleset.rules.kdoc

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.ElementType.BLOCK
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.IF
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.KDOC_TEXT
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.THEN
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.isWhiteSpace
import org.cqfn.diktat.common.config.rules.RuleConfiguration
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.getRuleConfig
import org.cqfn.diktat.ruleset.constants.Warnings.WRONG_NEWLINES_AROUND_KDOC
import org.cqfn.diktat.ruleset.constants.Warnings.COMMENT_WHITE_SPACE
import org.cqfn.diktat.ruleset.constants.Warnings.FIRST_COMMENT_NO_SPACES
import org.cqfn.diktat.ruleset.constants.Warnings.IF_ELSE_COMMENTS
import org.cqfn.diktat.ruleset.utils.*
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType


/**
 * This class handles rule 2.6
 * Part 1:
 * there must be 1 space between the comment character and the content of the comment;
 * there must be a newline between a Kdoc and the previous code above and no blank lines after.
 * No need to add a blank line before a first comment in this particular name space (code block), for example between function declaration and first comment in a function body.
 *
 * Part 2:
 * Leave one single space between the comment on the right side of the code and the code.
 * Comments in if else should be inside code blocks. Exception: General if comment
 */
class CommentsFormatting(private val configRules: List<RulesConfig>) : Rule("kdoc-comments-codeblocks-formatting") {

    companion object {
        private const val MAX_SPACES = 1
        private const val APPROPRIATE_COMMENT_SPACES = 1
    }

    private lateinit var emitWarn: ((offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit)
    private var isFixMode: Boolean = false

    override fun visit(node: ASTNode,
                       autoCorrect: Boolean,
                       emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        isFixMode = autoCorrect
        emitWarn = emit

        val configuration = CommentsFormattingConfiguration(
                configRules.getRuleConfig(COMMENT_WHITE_SPACE)?.configuration ?: mapOf())


        when (node.elementType) {
            CLASS, FUN, PROPERTY -> {
                checkBlankLineAfterKdoc(node, EOL_COMMENT)
                checkBlankLineAfterKdoc(node, KDOC)
                checkBlankLineAfterKdoc(node, BLOCK_COMMENT)
            }
            IF -> {
                handleIfElse(node)
            }
            EOL_COMMENT, BLOCK_COMMENT -> {
                handleEolAndBlockComments(node, configuration)
            }
            KDOC -> {
                handleKdocComments(node, configuration)
            }
        }
    }

    private fun checkBlankLineAfterKdoc(node: ASTNode, type: IElementType) {
        val kdoc = node.getFirstChildWithType(type)
        val nodeAfterKdoc = kdoc?.treeNext
        if (nodeAfterKdoc?.elementType == ElementType.WHITE_SPACE && nodeAfterKdoc.numNewLines() > 1) {
            WRONG_NEWLINES_AROUND_KDOC.warnAndFix(configRules, emitWarn, isFixMode, kdoc.text, nodeAfterKdoc.startOffset, nodeAfterKdoc) {
                nodeAfterKdoc.leaveOnlyOneNewLine()
            }
        }
    }

    private fun handleKdocComments(node: ASTNode, configuration: CommentsFormattingConfiguration) {
        if (node.treeParent.treeParent.elementType == BLOCK) {
            checkCommentsInCodeBlocks(node.treeParent) // node.treeParent is a node that contains a comment.
        } else if (node.treeParent.elementType != IF) {
            checkClassComment(node)
        }
        checkWhiteSpaceBeforeComment(node, configuration)
    }

    private fun handleEolAndBlockComments(node: ASTNode, configuration: CommentsFormattingConfiguration) {
        basicCommentsChecks(node, configuration)
        checkWhiteSpaceBeforeComment(node, configuration)
    }

    private fun basicCommentsChecks(node: ASTNode, configuration: CommentsFormattingConfiguration) {
        checkSpaceBetweenPropertyAndComment(node, configuration)

        if (node.treeParent.elementType == BLOCK && node.treeNext != null) {
            // Checking if comment is inside a code block like fun{}
            // Not checking last comment as well
            checkCommentsInCodeBlocks(node)
        } else if (node.treeParent.lastChildNode != node && node.treeParent.elementType != IF) {
            // Else the comment is in CLASS_BODY and not in IF block
            checkClassComment(node)
        }
    }

    private fun handleIfElse(node: ASTNode) {
        if (node.hasChildOfType(ELSE)) {
            val elseKeyWord = node.getFirstChildWithType(ELSE_KEYWORD)!!
            val elseBlock = node.getFirstChildWithType(ELSE)!!
            val elseCodeBlock = elseBlock.getFirstChildWithType(BLOCK)!!
            val comment = when {
                elseKeyWord.prevNodeUntilNode(THEN, EOL_COMMENT) != null -> elseKeyWord.prevNodeUntilNode(THEN, EOL_COMMENT)
                elseKeyWord.prevNodeUntilNode(THEN, BLOCK_COMMENT) != null -> elseKeyWord.prevNodeUntilNode(THEN, BLOCK_COMMENT)
                elseKeyWord.prevNodeUntilNode(THEN, KDOC) != null -> elseKeyWord.prevNodeUntilNode(THEN, KDOC)
                else -> null
            }
            val copyComment = comment?.copyElement()
            if (comment != null) {
                IF_ELSE_COMMENTS.warnAndFix(configRules, emitWarn, isFixMode, comment.text, node.startOffset, node) {
                    if (elseBlock.hasChildOfType(BLOCK)) {
                        elseCodeBlock.addChild(copyComment!!,
                                elseCodeBlock.firstChildNode.treeNext)
                        elseCodeBlock.addChild(PsiWhiteSpaceImpl("\n"),
                                elseCodeBlock.firstChildNode.treeNext)
                        node.removeChild(comment)
                    } else {
                        val text = "else { \n${comment.text}\n ${elseBlock.text} \n }"
                        node.removeChild(elseBlock)
                        node.addChild(KotlinParser().createNode(text), null)
                    }

                    val whiteSpace = elseKeyWord.prevNodeUntilNode(THEN, WHITE_SPACE)

                    if (whiteSpace != null)
                        node.removeChild(whiteSpace)
                }
            }
        }
    }

    private fun checkCommentsInCodeBlocks(node: ASTNode) {
        if (isFirstComment(node)) {
            if (node.isBlockOrClassBody())
                // Just check white spaces before comment
                checkFirstCommentSpaces(node)
            else
                // TreeParent is property. Then check white spaces before property
                checkFirstCommentSpaces(node.treeParent)
            return
        }

        if (!node.treePrev.isWhiteSpace()) {
            // If node treePrev is not a whiteSpace then node treeParent is a property
            WRONG_NEWLINES_AROUND_KDOC.warnAndFix(configRules, emitWarn, isFixMode, node.text, node.startOffset, node) {
                node.treeParent.addChild(PsiWhiteSpaceImpl("\n"), node.treeParent)
            }
        } else {
            if (node.treePrev.numNewLines() == 1 || node.treePrev.numNewLines() > 2) {
                WRONG_NEWLINES_AROUND_KDOC.warnAndFix(configRules, emitWarn, isFixMode, node.text, node.startOffset,node) {
                    (node.treePrev as LeafPsiElement).replaceWithText("\n\n")
                }
            }
        }
    }

    private fun checkSpaceBetweenPropertyAndComment(node: ASTNode, configuration: CommentsFormattingConfiguration) {
        if (node.treeParent.elementType == PROPERTY
                && node.treeParent.firstChildNode != node) {
            if (!node.treePrev.isWhiteSpace()) {
                // if comment is like this: val a = 5// Comment
                COMMENT_WHITE_SPACE.warnAndFix(configRules, emitWarn, isFixMode, node.text, node.startOffset, node) {
                    node.treeParent.addChild(PsiWhiteSpaceImpl(" ".repeat(configuration.maxSpacesBeforeComment)), node)
                }
            } else if (node.treePrev.text.length != configuration.maxSpacesBeforeComment) {
                // if there are too many spaces before comment
                COMMENT_WHITE_SPACE.warnAndFix(configRules, emitWarn, isFixMode, node.text, node.startOffset, node) {
                    (node.treePrev as LeafPsiElement).replaceWithText(" ".repeat(configuration.maxSpacesBeforeComment))
                }
            }
        }
    }

    private fun checkWhiteSpaceBeforeComment(node: ASTNode, configuration: CommentsFormattingConfiguration) {
        if (node.elementType == EOL_COMMENT &&
                node.text.trim('/').takeWhile { it == ' ' }.length == configuration.maxSpacesInComment)
            return

        if (node.elementType == BLOCK_COMMENT
                && (node.text.trim('/', '*').takeWhile { it == ' ' }.length == configuration.maxSpacesInComment
                        || node.text.trim('/', '*').takeWhile { it == '\n' }.isNotEmpty())) {
            return
        }

        if (node.elementType == KDOC) {
            node.findAllNodesWithSpecificType(KDOC_TEXT).forEach {
                if (it.text.startsWith(" ".repeat(configuration.maxSpacesInComment)))
                    return
            }
        }

        COMMENT_WHITE_SPACE.warnAndFix(configRules, emitWarn, isFixMode, node.text, node.startOffset, node) {
            var commentText = node.text.drop(2).trim()

            when (node.elementType) {
                EOL_COMMENT -> (node as LeafPsiElement).replaceWithText("// $commentText")
                BLOCK_COMMENT -> (node as LeafPsiElement).replaceWithText("/* $commentText")
                KDOC -> {
                    node.findAllNodesWithSpecificType(KDOC_TEXT).forEach {
                        if (!it.text.startsWith(" ".repeat(configuration.maxSpacesInComment))) {
                            commentText = it.text.trim()
                            val indent = " ".repeat(configuration.maxSpacesInComment)
                            (it as LeafPsiElement).replaceWithText("$indent $commentText")
                        }
                    }
                }
            }
        }
    }

    private fun checkClassComment(node: ASTNode) {
        if (isFirstComment(node)) {
            if (node.isBlockOrClassBody())
                checkFirstCommentSpaces(node)
            else
                checkFirstCommentSpaces(node.treeParent)

            return
        }

        if (node.treeParent.elementType != FILE && !node.treeParent.treePrev.isWhiteSpace()) {
            WRONG_NEWLINES_AROUND_KDOC.warnAndFix(configRules, emitWarn, isFixMode, node.text, node.startOffset, node) {
                node.treeParent.treeParent.addChild(PsiWhiteSpaceImpl("\n"), node.treeParent)
            }
        } else if (node.treeParent.elementType != FILE) {
            if (node.treeParent.treePrev.numNewLines() == 1 || node.treeParent.treePrev.numNewLines() > 2) {
                WRONG_NEWLINES_AROUND_KDOC.warnAndFix(configRules, emitWarn, isFixMode, node.text, node.startOffset, node) {
                    (node.treeParent.treePrev as LeafPsiElement).replaceWithText("\n\n")
                }
            }
        }
    }

    private fun checkFirstCommentSpaces(node: ASTNode) {
        if (node.treePrev.isWhiteSpace()) {
            if (node.treePrev.numNewLines() > 1
                    || node.treePrev.numNewLines() == 0) {
                FIRST_COMMENT_NO_SPACES.warnAndFix(configRules, emitWarn, isFixMode, node.text, node.startOffset, node) {
                    (node.treePrev as LeafPsiElement).replaceWithText("\n")
                }
            }
        }
    }


    private fun isFirstComment(node: ASTNode): Boolean {
        // In case when comment is inside of a function or class
        if (node.isBlockOrClassBody()) {
            return if (node.treePrev.isWhiteSpace())
                node.treePrev.treePrev.elementType == LBRACE
            else
                node.treePrev.elementType == LBRACE
        }

        // When comment inside of a PROPERTY
        if (node.treeParent.elementType != FILE)
            return node.treeParent.treePrev.treePrev.elementType == LBRACE

        return node.treeParent.getAllChildrenWithType(node.elementType).first() == node
    }


    private fun ASTNode.isBlockOrClassBody() : Boolean = treeParent.elementType == BLOCK || treeParent.elementType == CLASS_BODY

    class CommentsFormattingConfiguration(config: Map<String, String>) : RuleConfiguration(config) {
        val maxSpacesBeforeComment = config["maxSpacesBeforeComment"]?.toIntOrNull() ?: MAX_SPACES
        val maxSpacesInComment = config["maxSpacesInComment"]?.toIntOrNull() ?: APPROPRIATE_COMMENT_SPACES
    }
}
