package org.cqfn.diktat.ruleset.rules

import org.cqfn.diktat.common.config.rules.DIKTAT_COMMON
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.RulesConfigReader
import org.cqfn.diktat.ruleset.constants.Warnings
import org.cqfn.diktat.ruleset.dummy.DummyWarning
import org.cqfn.diktat.ruleset.rules.chapter1.FileNaming
import org.cqfn.diktat.ruleset.rules.chapter1.IdentifierNaming
import org.cqfn.diktat.ruleset.rules.chapter1.PackageNaming
import org.cqfn.diktat.ruleset.rules.chapter2.comments.CommentsRule
import org.cqfn.diktat.ruleset.rules.chapter2.comments.HeaderCommentRule
import org.cqfn.diktat.ruleset.rules.chapter2.kdoc.CommentsFormatting
import org.cqfn.diktat.ruleset.rules.chapter2.kdoc.KdocComments
import org.cqfn.diktat.ruleset.rules.chapter2.kdoc.KdocFormatting
import org.cqfn.diktat.ruleset.rules.chapter2.kdoc.KdocMethods
import org.cqfn.diktat.ruleset.rules.chapter3.AnnotationNewLineRule
import org.cqfn.diktat.ruleset.rules.chapter3.BlockStructureBraces
import org.cqfn.diktat.ruleset.rules.chapter3.BooleanExpressionsRule
import org.cqfn.diktat.ruleset.rules.chapter3.BracesInConditionalsAndLoopsRule
import org.cqfn.diktat.ruleset.rules.chapter3.ClassLikeStructuresOrderRule
import org.cqfn.diktat.ruleset.rules.chapter3.CollapseIfStatementsRule
import org.cqfn.diktat.ruleset.rules.chapter3.ConsecutiveSpacesRule
import org.cqfn.diktat.ruleset.rules.chapter3.EmptyBlock
import org.cqfn.diktat.ruleset.rules.chapter3.EnumsSeparated
import org.cqfn.diktat.ruleset.rules.chapter3.LineLength
import org.cqfn.diktat.ruleset.rules.chapter3.LongNumericalValuesSeparatedRule
import org.cqfn.diktat.ruleset.rules.chapter3.MagicNumberRule
import org.cqfn.diktat.ruleset.rules.chapter3.MultipleModifiersSequence
import org.cqfn.diktat.ruleset.rules.chapter3.NullableTypeRule
import org.cqfn.diktat.ruleset.rules.chapter3.RangeConventionalRule
import org.cqfn.diktat.ruleset.rules.chapter3.SingleLineStatementsRule
import org.cqfn.diktat.ruleset.rules.chapter3.SortRule
import org.cqfn.diktat.ruleset.rules.chapter3.StringConcatenationRule
import org.cqfn.diktat.ruleset.rules.chapter3.StringTemplateFormatRule
import org.cqfn.diktat.ruleset.rules.chapter3.TrailingCommaRule
import org.cqfn.diktat.ruleset.rules.chapter3.WhenMustHaveElseRule
import org.cqfn.diktat.ruleset.rules.chapter3.files.BlankLinesRule
import org.cqfn.diktat.ruleset.rules.chapter3.files.FileSize
import org.cqfn.diktat.ruleset.rules.chapter3.files.FileStructureRule
import org.cqfn.diktat.ruleset.rules.chapter3.files.IndentationRule
import org.cqfn.diktat.ruleset.rules.chapter3.files.NewlinesRule
import org.cqfn.diktat.ruleset.rules.chapter3.files.TopLevelOrderRule
import org.cqfn.diktat.ruleset.rules.chapter3.files.WhiteSpaceRule
import org.cqfn.diktat.ruleset.rules.chapter3.identifiers.LocalVariablesRule
import org.cqfn.diktat.ruleset.rules.chapter4.ImmutableValNoVarRule
import org.cqfn.diktat.ruleset.rules.chapter4.NullChecksRule
import org.cqfn.diktat.ruleset.rules.chapter4.SmartCastRule
import org.cqfn.diktat.ruleset.rules.chapter4.TypeAliasRule
import org.cqfn.diktat.ruleset.rules.chapter4.VariableGenericTypeDeclarationRule
import org.cqfn.diktat.ruleset.rules.chapter4.calculations.AccurateCalculationsRule
import org.cqfn.diktat.ruleset.rules.chapter5.AsyncAndSyncRule
import org.cqfn.diktat.ruleset.rules.chapter5.AvoidNestedFunctionsRule
import org.cqfn.diktat.ruleset.rules.chapter5.CheckInverseMethodRule
import org.cqfn.diktat.ruleset.rules.chapter5.CustomLabel
import org.cqfn.diktat.ruleset.rules.chapter5.FunctionArgumentsSize
import org.cqfn.diktat.ruleset.rules.chapter5.FunctionLength
import org.cqfn.diktat.ruleset.rules.chapter5.LambdaLengthRule
import org.cqfn.diktat.ruleset.rules.chapter5.LambdaParameterOrder
import org.cqfn.diktat.ruleset.rules.chapter5.NestedFunctionBlock
import org.cqfn.diktat.ruleset.rules.chapter5.OverloadingArgumentsFunction
import org.cqfn.diktat.ruleset.rules.chapter6.AvoidEmptyPrimaryConstructor
import org.cqfn.diktat.ruleset.rules.chapter6.AvoidUtilityClass
import org.cqfn.diktat.ruleset.rules.chapter6.CustomGetterSetterRule
import org.cqfn.diktat.ruleset.rules.chapter6.ExtensionFunctionsInFileRule
import org.cqfn.diktat.ruleset.rules.chapter6.ExtensionFunctionsSameNameRule
import org.cqfn.diktat.ruleset.rules.chapter6.ImplicitBackingPropertyRule
import org.cqfn.diktat.ruleset.rules.chapter6.PropertyAccessorFields
import org.cqfn.diktat.ruleset.rules.chapter6.RunInScript
import org.cqfn.diktat.ruleset.rules.chapter6.TrivialPropertyAccessors
import org.cqfn.diktat.ruleset.rules.chapter6.UselessSupertype
import org.cqfn.diktat.ruleset.rules.chapter6.classes.AbstractClassesRule
import org.cqfn.diktat.ruleset.rules.chapter6.classes.CompactInitialization
import org.cqfn.diktat.ruleset.rules.chapter6.classes.DataClassesRule
import org.cqfn.diktat.ruleset.rules.chapter6.classes.InlineClassesRule
import org.cqfn.diktat.ruleset.rules.chapter6.classes.SingleConstructorRule
import org.cqfn.diktat.ruleset.rules.chapter6.classes.SingleInitRule
import org.cqfn.diktat.ruleset.rules.chapter6.classes.StatelessClassesRule

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import org.jetbrains.kotlin.org.jline.utils.Levenshtein
import org.slf4j.LoggerFactory

import java.io.File

/**
 * this constant will be used everywhere in the code to mark usage of Diktat ruleset
 */
const val DIKTAT_RULE_SET_ID = "diktat-ruleset"
const val DIKTAT_ANALYSIS_CONF = "diktat-analysis.yml"
const val DIKTAT_CONF_PROPERTY = "diktat.config.path"

/**
 * [RuleSetProvider] that provides diKTat ruleset.
 * By default it is expected to have diktat-analysis.yml configuration in the root folder where 'ktlint' is run
 * otherwise it will use default configuration where some rules are disabled
 *
 * @param diktatConfigFile - configuration file where all configurations for inspections and rules are stored
 */
class DiktatRuleSetProvider(private var diktatConfigFile: String = DIKTAT_ANALYSIS_CONF) : RuleSetProvider {
    private val possibleConfigs: Sequence<String?> = sequence {
        yield(resolveDefaultConfig())
        yield(resolveConfigFileFromJarLocation())
        yield(resolveConfigFileFromSystemProperty())
    }

    @Suppress(
        "LongMethod",
        "TOO_LONG_FUNCTION",
        "SpreadOperator")
    override fun get(): RuleSet {
        log.debug("Will run $DIKTAT_RULE_SET_ID with $diktatConfigFile" +
                " (it can be placed to the run directory or the default file from resources will be used)")
        val configPath = possibleConfigs
            .firstOrNull { it != null && File(it).exists() }
        diktatConfigFile = configPath
            ?: run {
                val possibleConfigsList = possibleConfigs.toList()
                log.warn(
                    "Configuration file not found in directory where diktat is run (${possibleConfigsList[0]}) " +
                            "or in the directory where diktat.jar is stored (${possibleConfigsList[1]}) " +
                            "or in system property <diktat.config.path> (${possibleConfigsList[2]}), " +
                            "the default file included in jar will be used. " +
                            "Some configuration options will be disabled or substituted with defaults. " +
                            "Custom configuration file should be placed in diktat working directory if run from CLI " +
                            "or provided as configuration options in plugins."
                )
                diktatConfigFile
            }

        val configRules = RulesConfigReader(javaClass.classLoader)
            .readResource(diktatConfigFile)
            ?.onEach(::validate)
            ?: emptyList()
        // Note: the order of rules is important in autocorrect mode. For example, all rules that add new code should be invoked before rules that fix formatting.
        // We don't have a way to enforce a specific order, so we should just be careful when adding new rules to this list and, when possible,
        // cover new rules in smoke test as well. If a rule needs to be at a specific position in a list, please add comment explaining it (like for NewlinesRule).
        val rules = listOf(
            // test warning that can be used for manual testing of diktat
            ::DummyWarning,

            // comments & documentation
            ::CommentsRule,
            ::SingleConstructorRule,  // this rule can add properties to a primary constructor, so should be before KdocComments
            ::KdocComments,
            ::KdocMethods,
            ::KdocFormatting,
            ::CommentsFormatting,
            // naming
            ::FileNaming,
            ::PackageNaming,
            ::IdentifierNaming,
            // code structure
            ::UselessSupertype,
            ::ClassLikeStructuresOrderRule,
            ::WhenMustHaveElseRule,
            ::BracesInConditionalsAndLoopsRule,
            ::EmptyBlock,
            ::AvoidEmptyPrimaryConstructor,
            ::EnumsSeparated,
            ::TopLevelOrderRule,
            ::SingleLineStatementsRule,
            ::MultipleModifiersSequence,
            ::TrivialPropertyAccessors,
            ::CustomGetterSetterRule,
            ::CompactInitialization,
            // other rules
            ::InlineClassesRule,
            ::ExtensionFunctionsInFileRule,
            ::CheckInverseMethodRule,
            ::StatelessClassesRule,
            ::ImplicitBackingPropertyRule,
            ::DataClassesRule,
            ::LocalVariablesRule,
            ::SmartCastRule,
            ::AvoidUtilityClass,
            ::PropertyAccessorFields,
            ::AbstractClassesRule,
            ::TrailingCommaRule,
            ::SingleInitRule,
            ::RangeConventionalRule,
            ::CustomLabel,
            ::VariableGenericTypeDeclarationRule,
            ::LongNumericalValuesSeparatedRule,
            ::NestedFunctionBlock,
            ::AnnotationNewLineRule,
            ::SortRule,
            ::StringConcatenationRule,
            ::StringTemplateFormatRule,
            ::AccurateCalculationsRule,
            ::CollapseIfStatementsRule,
            ::LineLength,
            ::RunInScript,
            ::TypeAliasRule,
            ::OverloadingArgumentsFunction,
            ::FunctionLength,
            ::MagicNumberRule,
            ::LambdaParameterOrder,
            ::FunctionArgumentsSize,
            ::BlankLinesRule,
            ::FileSize,
            ::AsyncAndSyncRule,
            ::NullableTypeRule,
            ::NullChecksRule,
            ::ImmutableValNoVarRule,
            ::AvoidNestedFunctionsRule,
            ::ExtensionFunctionsSameNameRule,
            ::LambdaLengthRule,
            ::BooleanExpressionsRule,
            // formatting: moving blocks, adding line breaks, indentations etc.
            ::BlockStructureBraces,
            ::ConsecutiveSpacesRule,
            ::HeaderCommentRule,
            ::FileStructureRule,  // this rule should be right before indentation because it should operate on already valid code
            ::NewlinesRule,  // newlines need to be inserted right before fixing indentation
            ::WhiteSpaceRule,  // this rule should be after other rules that can cause wrong spacing
            ::IndentationRule,  // indentation rule should be the last because it fixes formatting after all the changes done by previous rules

        )
            .map {
                it.invoke(configRules)
            }
            .toTypedArray()
        return RuleSet(
            DIKTAT_RULE_SET_ID,
            *rules
        )
    }

    private fun validate(config: RulesConfig) =
            require(config.name == DIKTAT_COMMON || config.name in Warnings.names) {
                val closestMatch = Warnings.names.minByOrNull { Levenshtein.distance(it, config.name) }
                "Warning name <${config.name}> in configuration file is invalid, did you mean <$closestMatch>?"
            }

    private fun resolveDefaultConfig() = diktatConfigFile

    private fun resolveConfigFileFromJarLocation(): String {
        // for some aggregators of static analyzers we need to provide configuration for cli
        // in this case diktat would take the configuration from the directory where jar file is stored
        val ruleSetProviderPath =
                DiktatRuleSetProvider::class
                    .java
                    .protectionDomain
                    .codeSource
                    .location
                    .toURI()

        val configPathWithFileName = File(ruleSetProviderPath).absolutePath

        val indexOfName = configPathWithFileName.lastIndexOf(File.separator)
        val configPath = if (indexOfName > -1) configPathWithFileName.substring(0, indexOfName) else configPathWithFileName

        return "$configPath${File.separator}$diktatConfigFile"
    }

    private fun resolveConfigFileFromSystemProperty(): String? = System.getProperty(DIKTAT_CONF_PROPERTY)

    companion object {
        private val log = LoggerFactory.getLogger(DiktatRuleSetProvider::class.java)
    }
}
