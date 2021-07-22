package org.cqfn.diktat.ruleset.constants

import org.cqfn.diktat.common.config.rules.Rule
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.isRuleEnabled
import org.cqfn.diktat.ruleset.utils.hasSuppress
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

typealias EmitType = ((offset: Int,
                       errorMessage: String,
                       canBeAutoCorrected: Boolean) -> Unit)

typealias ListOfList = MutableList<MutableList<ASTNode>>

typealias ListOfPairs = MutableList<Pair<ASTNode, String>>

/**
 * This class represent individual inspections of diktat code style.
 * A [Warnings] entry contains rule name, warning message and is used in code check.
 * @property canBeAutoCorrected whether this inspection can automatically fix the code. Should be public to be able to use it in docs generator.
 * @property ruleId number of the inspection according to []diktat code style](https://www.cqfn.org/diKTat/info/guide/diktat-coding-convention.html)
 */
@Suppress(
    "ForbiddenComment",
    "MagicNumber",
    "WRONG_DECLARATIONS_ORDER",
    "MaxLineLength",
    "WRONG_NEWLINES"
)
enum class Warnings(
    @Suppress("PRIVATE_MEMBER") val canBeAutoCorrected: Boolean,
    val ruleId: String,
    private val warn: String) : Rule {
    // ======== dummy test warning ======
    DUMMY_TEST_WARNING(true, "0.0.0", "this is a dummy warning that can be used for manual testing of fixer/checker"),

    // ======== chapter 1 ========
    PACKAGE_NAME_MISSING(true, "1.2.1", "no package name declared in a file"),
    PACKAGE_NAME_INCORRECT_CASE(true, "1.2.1", "package name should be completely in a lower case"),
    PACKAGE_NAME_INCORRECT_PREFIX(true, "1.2.1", "package name should start from company's domain"),

    // FixMe: should add autofix
    PACKAGE_NAME_INCORRECT_SYMBOLS(false, "1.2.1", "package name should contain only latin (ASCII) letters or numbers. For separation of words use dot"),
    PACKAGE_NAME_INCORRECT_PATH(true, "1.2.1", "package name does not match the directory hierarchy for this file, the real package name should be"),
    INCORRECT_PACKAGE_SEPARATOR(true, "1.2.1", "package name parts should be separated only by dots - there should be no other symbols like underscores (_)"),
    CLASS_NAME_INCORRECT(true, "1.3.1", "class/enum/interface name should be in PascalCase and should contain only latin (ASCII) letters or numbers"),
    OBJECT_NAME_INCORRECT(true, "1.3.1", "object structure name should be in PascalCase and should contain only latin (ASCII) letters or numbers"),
    VARIABLE_NAME_INCORRECT_FORMAT(true, "1.6.1",
        "variable name should be in lowerCamelCase and should contain only latin (ASCII) letters or numbers and should start from lower letter"),
    VARIABLE_NAME_INCORRECT(false, "1.1.1", "variable name should contain more than one letter"),
    CONSTANT_UPPERCASE(true, "1.5.1", "<val> properties from companion object or on file level mostly in all cases are constants - please use upper snake case for them"),
    VARIABLE_HAS_PREFIX(true, "1.1.1", "variable has prefix (like mVariable or M_VARIABLE), generally it is a bad code style (Android - is the only exception)"),
    IDENTIFIER_LENGTH(false, "1.1.1", "identifier's length is incorrect, it should be in range of [2, 64] symbols"),
    ENUM_VALUE(true, "1.3.1", "enum values should be in selected UPPER_CASE snake/PascalCase format"),
    GENERIC_NAME(true, "1.1.1", "generic name should contain only one single capital letter, it can be followed by a number"),
    BACKTICKS_PROHIBITED(false, "1.1.1", "backticks should not be used in identifier's naming. The only exception test methods marked with @Test annotation"),
    FUNCTION_NAME_INCORRECT_CASE(true, "1.4.1", "function/method name should be in lowerCamelCase"),
    TYPEALIAS_NAME_INCORRECT_CASE(true, "1.3.1", "typealias name should be in pascalCase"),
    FUNCTION_BOOLEAN_PREFIX(true, "1.6.2", "functions that return the value of Boolean type should have <is> or <has> prefix"),
    FILE_NAME_INCORRECT(true, "1.1.1", "file name is incorrect - it should end with .kt extension and be in PascalCase"),
    EXCEPTION_SUFFIX(true, "1.1.1", "all exception classes should have \"Exception\" suffix"),
    CONFUSING_IDENTIFIER_NAMING(false, "1.1.1", "it's a bad name for identifier"),

    // ======== chapter 2 ========
    MISSING_KDOC_TOP_LEVEL(false, "2.1.1", "all public and internal top-level classes and functions should have Kdoc"),
    MISSING_KDOC_CLASS_ELEMENTS(false, "2.1.1", "all public, internal and protected classes, functions and variables inside the class should have Kdoc"),
    MISSING_KDOC_ON_FUNCTION(true, "2.1.1", "all public, internal and protected functions should have Kdoc with proper tags"),
    KDOC_TRIVIAL_KDOC_ON_FUNCTION(false, "2.3.1", "KDocs should not be trivial (e.g. method getX should not de documented as 'returns X')"),
    KDOC_WITHOUT_PARAM_TAG(true, "2.1.2", "all methods which take arguments should have @param tags in KDoc"),
    KDOC_WITHOUT_RETURN_TAG(true, "2.1.2", "all methods which return values should have @return tag in KDoc"),
    KDOC_WITHOUT_THROWS_TAG(true, "2.1.2", "all methods which throw exceptions should have @throws tag in KDoc"),
    KDOC_EMPTY_KDOC(false, "2.1.3", "KDoc should never be empty"),
    KDOC_WRONG_SPACES_AFTER_TAG(true, "2.1.3", "there should be exactly one white space after tag name in KDoc"),
    KDOC_WRONG_TAGS_ORDER(true, "2.1.3", "in KDoc standard tags are arranged in order @param, @return, @throws, but are"),
    KDOC_NEWLINES_BEFORE_BASIC_TAGS(true, "2.1.3",
        "in KDoc block of standard tags @param, @return, @throws should contain newline before only if there is other content before it"),
    KDOC_NO_NEWLINES_BETWEEN_BASIC_TAGS(true, "2.1.3", "in KDoc standard tags @param, @return, @throws should not containt newline between them, but these tags do"),
    KDOC_NO_NEWLINE_AFTER_SPECIAL_TAGS(true, "2.1.3", "in KDoc there should be exactly one empty line after special tags"),
    KDOC_NO_EMPTY_TAGS(false, "2.2.1", "no empty descriptions in tag blocks are allowed"),
    KDOC_NO_DEPRECATED_TAG(true, "2.1.3", "KDoc doesn't support @deprecated tag, use @Deprecated annotation instead"),
    KDOC_NO_CONSTRUCTOR_PROPERTY(true, "2.1.1", "all properties from the primary constructor should be documented in a @property tag in KDoc"),
    KDOC_NO_CLASS_BODY_PROPERTIES_IN_HEADER(true, "2.1.1", "only properties from the primary constructor should be documented in a @property tag in class KDoc"),
    KDOC_EXTRA_PROPERTY(false, "2.1.1", "There is property in KDoc which is not present in the class"),
    KDOC_NO_CONSTRUCTOR_PROPERTY_WITH_COMMENT(true, "2.1.1", "replace comment before property with @property tag in class KDoc"),
    KDOC_CONTAINS_DATE_OR_AUTHOR(false, "2.1.3", "KDoc should not contain creation date and author name"),
    HEADER_WRONG_FORMAT(true, "2.2.1", "file header comments should be properly formatted"),
    HEADER_MISSING_OR_WRONG_COPYRIGHT(true, "2.2.1", "file header comment must include copyright information inside a block comment"),
    WRONG_COPYRIGHT_YEAR(true, "2.2.1", "year defined in copyright and current year are different"),
    HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE(false, "2.2.1", "files that contain multiple or no classes should contain description of what is inside of this file"),
    HEADER_NOT_BEFORE_PACKAGE(true, "2.2.1", "header KDoc should be placed before package and imports"),
    COMMENTED_OUT_CODE(false, "2.4.2", "you should not comment out code, use VCS to save it in history and delete this block"),
    WRONG_NEWLINES_AROUND_KDOC(true, "2.4.1", "there should be a blank line above the kDoc and there should not be no blank lines after kDoc"),
    FIRST_COMMENT_NO_BLANK_LINE(true, "2.4.1", "there should not be any blank lines before first comment"),
    COMMENT_WHITE_SPACE(true, "2.4.1", "there should be a white space between code and comment also between code start token and comment text"),
    IF_ELSE_COMMENTS(true, "2.4.1", "invalid comments structure. Comment should be inside the block"),

    // ======== chapter 3 ========
    FILE_IS_TOO_LONG(false, "3.1.1", "file has more number of lines than expected"),
    FILE_CONTAINS_ONLY_COMMENTS(false, "3.1.2", "empty files or files that contain only comments should be avoided"),
    FILE_INCORRECT_BLOCKS_ORDER(true, "3.1.2", "general structure of kotlin source file is wrong, parts are in incorrect order"),
    FILE_NO_BLANK_LINE_BETWEEN_BLOCKS(true, "3.1.2", "general structure of kotlin source file is wrong, general code blocks sohuld be separated by empty lines"),
    FILE_UNORDERED_IMPORTS(true, "3.1.2", "imports should be ordered alphabetically and shouldn't be separated by newlines"),
    FILE_WILDCARD_IMPORTS(false, "3.1.2", "wildcard imports should not be used"),
    UNUSED_IMPORT(true, "3.1.2", "unused imports should be removed"),
    NO_BRACES_IN_CONDITIONALS_AND_LOOPS(true, "3.2.1", "in if, else, when, for, do, and while statements braces should be used. Exception: single line if statement."),
    WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES(true, "3.1.4", "the declaration part of a class-like code structures (class/interface/etc.) should be in the proper order"),
    BLANK_LINE_BETWEEN_PROPERTIES(true, "3.1.4", "there should be no blank lines between properties without comments; comment or KDoc on property should have blank line before"),
    TOP_LEVEL_ORDER(true, "3.1.5", "the declaration part of a top level elements should be in the proper order"),
    BRACES_BLOCK_STRUCTURE_ERROR(true, "3.2.2", "braces should follow 1TBS style"),
    WRONG_INDENTATION(true, "3.3.1", "only spaces are allowed for indentation and each indentation should equal to 4 spaces (tabs are not allowed)"),
    EMPTY_BLOCK_STRUCTURE_ERROR(true, "3.4.1", "incorrect format of empty block"),
    MORE_THAN_ONE_STATEMENT_PER_LINE(true, "3.6.1", "there should not be more than one code statement in one line"),
    LONG_LINE(true, "3.5.1", "this line is longer than allowed"),
    REDUNDANT_SEMICOLON(true, "3.6.2", "there should be no redundant semicolon at the end of lines"),
    WRONG_NEWLINES(true, "3.6.2", "incorrect line breaking"),
    TRAILING_COMMA(true, "3.6.2", "use trailing comma"),
    COMPLEX_EXPRESSION(false, "3.6.3", "complex dot qualified expression should be replaced with variable"),
    COMPLEX_BOOLEAN_EXPRESSION(true, "3.6.4", "too complex boolean expression, that can be simplified"),

    // FixMe: autofixing will be added for this rule
    STRING_CONCATENATION(true, "3.15.1", "strings should not be concatenated using plus operator - use string templates instead if the statement fits one line"),
    TOO_MANY_BLANK_LINES(true, "3.7.1", "too many consecutive blank lines"),
    WRONG_WHITESPACE(true, "3.8.1", "incorrect usage of whitespaces for code separation"),
    TOO_MANY_CONSECUTIVE_SPACES(true, "3.8.1", "too many consecutive spaces"),
    ANNOTATION_NEW_LINE(true, "3.12.1", "annotations must be on new line"),
    ENUMS_SEPARATED(true, "3.9.1", "enum is incorrectly formatted"),
    WHEN_WITHOUT_ELSE(true, "3.11.1", "each 'when' statement must have else at the end"),
    LONG_NUMERICAL_VALUES_SEPARATED(true, "3.14.2", "long numerical values should be separated with underscore"),
    MAGIC_NUMBER(false, "3.14.3", "avoid using magic numbers, instead define constants with clear names describing what the magic number means"),
    WRONG_DECLARATIONS_ORDER(true, "3.1.4", "declarations of constants and enum members should be sorted alphabetically"),
    WRONG_MULTIPLE_MODIFIERS_ORDER(true, "3.14.1", "sequence of modifier-keywords is incorrect"),
    LOCAL_VARIABLE_EARLY_DECLARATION(false, "3.10.2", "local variables should be declared close to the line where they are first used"),
    STRING_TEMPLATE_CURLY_BRACES(true, "3.15.2", "string template has redundant curly braces"),
    STRING_TEMPLATE_QUOTES(true, "3.15.2", "string template has redundant quotes"),
    FILE_NAME_MATCH_CLASS(true, "3.1.2", "file name is incorrect - it should match with the class described in it if there is the only one class declared"),
    COLLAPSE_IF_STATEMENTS(true, "3.16.1", "avoid using redundant nested if-statements, which could be collapsed into a single one"),
    CONVENTIONAL_RANGE(true, "3.17.1", "use conventional rule for range case"),

    // ======== chapter 4 ========
    NULLABLE_PROPERTY_TYPE(true, "4.3.1", "try to avoid use of nullable types"),
    TYPE_ALIAS(false, "4.2.2", "variable's type is too complex and should be replaced with typealias"),
    SMART_CAST_NEEDED(true, "4.2.1", "you can omit explicit casting"),
    SAY_NO_TO_VAR(false, "4.1.3", "Usage of a mutable variables with [var] modifier - is a bad style, use [val] instead"),
    GENERIC_VARIABLE_WRONG_DECLARATION(true, "4.3.2", "variable should have explicit type declaration"),

    // FixMe: change float literal to BigDecimal? Or kotlin equivalent?
    FLOAT_IN_ACCURATE_CALCULATIONS(false, "4.1.1", "floating-point values shouldn't be used in accurate calculations"),
    AVOID_NULL_CHECKS(false, "4.3.3", "Try to avoid explicit null-checks"),

    // ======== chapter 5 ========
    TOO_LONG_FUNCTION(false, "5.1.1", "function is too long: split it or make more primitive"),
    AVOID_NESTED_FUNCTIONS(true, "5.1.3", "try to avoid using nested functions"),
    LAMBDA_IS_NOT_LAST_PARAMETER(false, "5.2.1", "lambda inside function parameters should be in the end"),
    TOO_MANY_PARAMETERS(false, "5.2.2", "function has too many parameters"),
    NESTED_BLOCK(false, "5.1.2", "function has too many nested blocks and should be simplified"),
    WRONG_OVERLOADING_FUNCTION_ARGUMENTS(false, "5.2.3", "use default argument instead of function overloading"),
    RUN_BLOCKING_INSIDE_ASYNC(false, "5.2.4", "avoid using runBlocking in asynchronous code"),
    TOO_MANY_LINES_IN_LAMBDA(false, "5.2.5", "long lambdas should have a parameter name instead of it"),
    CUSTOM_LABEL(false, "5.2.6", "avoid using expression with custom label"),
    INVERSE_FUNCTION_PREFERRED(true, "5.1.4", "it is better to use inverse function"),

    // ======== chapter 6 ========
    SINGLE_CONSTRUCTOR_SHOULD_BE_PRIMARY(true, "6.1.1", "if a class has single constructor, it should be converted to a primary constructor"),
    USE_DATA_CLASS(false, "6.1.2", "this class can be converted to a data class"),
    WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR(false, "6.1.9", "use `field` keyword instead of property name inside property accessors"),
    MULTIPLE_INIT_BLOCKS(true, "6.1.4", "avoid using multiple `init` blocks, this logic can be moved to constructors or properties declarations"),
    CLASS_SHOULD_NOT_BE_ABSTRACT(true, "6.1.6", "class should not be abstract, because it has no abstract functions"),
    CUSTOM_GETTERS_SETTERS(false, "6.1.8", "custom getters and setters are not recommended, use class methods instead"),
    COMPACT_OBJECT_INITIALIZATION(true, "6.1.11", "class instance can be initialized in `apply` block"),
    USELESS_SUPERTYPE(true, "6.1.5", "unnecessary supertype specification"),
    TRIVIAL_ACCESSORS_ARE_NOT_RECOMMENDED(true, "6.1.10", "trivial property accessors are not recommended"),
    EXTENSION_FUNCTION_SAME_SIGNATURE(false, "6.2.2", "extension functions should not have same signature if their receiver classes are related"),
    EMPTY_PRIMARY_CONSTRUCTOR(true, "6.1.3", "avoid empty primary constructor"),
    NO_CORRESPONDING_PROPERTY(false, "6.1.7", "backing property should have the same name, but there is no corresponding property"),
    AVOID_USING_UTILITY_CLASS(false, "6.4.1", "avoid using utility classes/objects, use extensions functions"),
    OBJECT_IS_PREFERRED(true, "6.4.2", "it is better to use object for stateless classes"),
    INLINE_CLASS_CAN_BE_USED(true, "6.1.12", "inline class can be used"),
    EXTENSION_FUNCTION_WITH_CLASS(false, "6.2.3", "do not use extension functions for the class defined in the same file"),
    RUN_IN_SCRIPT(true, "6.5.1", "wrap blocks of code in top-level scope functions like `run`"),
    ;

    /**
     * Name of the inspection, it is used in configuration and in output.
     */
    override fun ruleName() = this.name

    /**
     * Warning message that will be logged to analysis report
     */
    fun warnText() = "[${ruleName()}] ${this.warn}:"

    /**
     * @param configRules list of [RulesConfig]
     * @param emit function that will be called on warning
     * @param isFixMode whether autocorrect mode is on
     * @param freeText text that will be added to the warning message
     * @param offset offset from the beginning of the file
     * @param node the [ASTNode] on which the warning was triggered
     * @param canBeAutoCorrected whether this warning can be autocorrected
     * @param autoFix function that will be called to autocorrect the warning
     */
    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
    fun warnAndFix(configRules: List<RulesConfig>,
                   emit: EmitType,
                   isFixMode: Boolean,
                   freeText: String,
                   offset: Int,
                   node: ASTNode,
                   canBeAutoCorrected: Boolean = this.canBeAutoCorrected,
                   autoFix: () -> Unit) {
        warn(configRules, emit, canBeAutoCorrected, freeText, offset, node)
        if (canBeAutoCorrected) {
            fix(configRules, isFixMode, node, autoFix)
        }
    }

    /**
     * @param configs list of [RulesConfig]
     * @param emit function that will be called on warning
     * @param autoCorrected whether this warning can be autocorrected
     * @param freeText text that will be added to the warning message
     * @param offset offset from the beginning of the file
     * @param node the [ASTNode] on which the warning was triggered
     */
    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
    fun warn(configs: List<RulesConfig>,
             emit: EmitType,
             autoCorrected: Boolean,
             freeText: String,
             offset: Int,
             node: ASTNode) {
        if (isRuleFromActiveChapter(configs) && configs.isRuleEnabled(this) && !node.hasSuppress(name)) {
            val trimmedFreeText = freeText
                .lines()
                .run { if (size > 1) "${first()}..." else first() }
            emit(offset,
                "${this.warnText()} $trimmedFreeText",
                autoCorrected
            )
        }
    }

    private inline fun fix(
        configs: List<RulesConfig>,
        isFix: Boolean,
        node: ASTNode,
        autoFix: () -> Unit) {
        if (isRuleFromActiveChapter(configs) && configs.isRuleEnabled(this) && isFix && !node.hasSuppress(name)) {
            autoFix()
        }
    }

    companion object {
        val names by lazy {
            values().map { it.name }
        }
    }
}
