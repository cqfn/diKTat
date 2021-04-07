package org.cqfn.diktat.ruleset.smoke

import org.cqfn.diktat.common.config.rules.DIKTAT_COMMON
import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.common.config.rules.RulesConfigReader
import org.cqfn.diktat.ruleset.constants.Warnings
import org.cqfn.diktat.ruleset.constants.Warnings.EMPTY_BLOCK_STRUCTURE_ERROR
import org.cqfn.diktat.ruleset.constants.Warnings.FILE_NAME_MATCH_CLASS
import org.cqfn.diktat.ruleset.constants.Warnings.HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_NO_EMPTY_TAGS
import org.cqfn.diktat.ruleset.constants.Warnings.KDOC_WITHOUT_PARAM_TAG
import org.cqfn.diktat.ruleset.constants.Warnings.MISSING_KDOC_CLASS_ELEMENTS
import org.cqfn.diktat.ruleset.constants.Warnings.MISSING_KDOC_ON_FUNCTION
import org.cqfn.diktat.ruleset.constants.Warnings.MISSING_KDOC_TOP_LEVEL
import org.cqfn.diktat.ruleset.rules.DIKTAT_RULE_SET_ID
import org.cqfn.diktat.ruleset.rules.DiktatRuleSetProvider
import org.cqfn.diktat.util.FixTestBase
import org.cqfn.diktat.util.assertEquals

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.pinterest.ktlint.core.LintError
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

import java.time.LocalDate

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempFile
import kotlinx.serialization.encodeToString

typealias ruleToConfig = Map<String, Map<String, String>>

/**
 * Test for [DiktatRuleSetProvider] in autocorrect mode as a whole. All rules are applied to a file.
 * Note: ktlint uses initial text from a file to calculate line and column from offset. Because of that line/col of unfixed errors
 * may change after some changes to text or other rules.
 */
@OptIn(ExperimentalPathApi::class)
class DiktatSmokeTest : FixTestBase("test/smoke/src/main/kotlin",
    { DiktatRuleSetProvider(configFilePath) },
    { lintError, _ -> unfixedLintErrors.add(lintError) },
    null
) {
    /**
     * Disable some of the rules.
     */
    @Suppress("UnsafeCallOnNullableType")
    private fun overrideRulesConfig(rulesToDisable: List<Warnings>, rulesToOverride: ruleToConfig = emptyMap()) {
        val rulesConfig = RulesConfigReader(javaClass.classLoader).readResource(configFilePath)!!
            .toMutableList()
            .also { rulesConfig ->
                rulesToDisable.forEach { warning ->
                    rulesConfig.removeIf { it.name == warning.name }
                    rulesConfig.add(RulesConfig(warning.name, enabled = false, configuration = emptyMap()))
                }
                rulesToOverride.forEach { (warning, configuration) ->
                    rulesConfig.removeIf { it.name == warning }
                    rulesConfig.add(RulesConfig(warning, enabled = true, configuration = configuration))
                }
            }
        createTempFile().toFile()
            .also {
                configFilePath = it.absolutePath
            }
            .writeText(
                Yaml(configuration = YamlConfiguration(strictMode = true))
                    .encodeToString(rulesConfig)
            )
    }

    @BeforeEach
    fun setUp() {
        unfixedLintErrors.clear()
    }

    @AfterEach
    fun tearDown() {
        configFilePath = DEFAULT_CONFIG_PATH
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `smoke test with multiplatform project layout`() {
        fixAndCompare("../../jsMain/kotlin/org/cqfn/diktat/scripts/ScriptExpected.kt",
            "../../jsMain/kotlin/org/cqfn/diktat/scripts/ScriptTest.kt")
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `regression - should not fail if package is not set`() {
        overrideRulesConfig(listOf(Warnings.PACKAGE_NAME_MISSING, Warnings.PACKAGE_NAME_INCORRECT_PATH,
            Warnings.PACKAGE_NAME_INCORRECT_PREFIX))
        fixAndCompare("DefaultPackageExpected.kt", "DefaultPackageTest.kt")
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `smoke test #7`() {
        fixAndCompare("Example7Expected.kt", "Example7Test.kt")
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `smoke test #6`() {
        fixAndCompare("Example6Expected.kt", "Example6Test.kt")
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `smoke test #5`() {
        overrideRulesConfig(emptyList(),
            mapOf(
                Warnings.HEADER_MISSING_OR_WRONG_COPYRIGHT.name to mapOf(
                    "isCopyrightMandatory" to "true",
                    "copyrightText" to """|Copyright 2018-${LocalDate.now().year} John Doe.
                                    |    Licensed under the Apache License, Version 2.0 (the "License");
                                    |    you may not use this file except in compliance with the License.
                                    |    You may obtain a copy of the License at
                                    |
                                    |        http://www.apache.org/licenses/LICENSE-2.0
                                """.trimMargin()
                ),
                DIKTAT_COMMON to mapOf(
                    "domainName" to "org.cqfn.diktat",
                    "kotlinVersion" to "1.3.7"
                )
            )
        )
        fixAndCompare("Example5Expected.kt", "Example5Test.kt")

        Assertions.assertFalse(
            unfixedLintErrors.contains(LintError(line = 1, col = 1, ruleId = "diktat-ruleset:comments", detail = "${Warnings.COMMENTED_OUT_CODE.warnText()} /*"))
        )

        Assertions.assertTrue(
            unfixedLintErrors.contains(LintError(1, 1, "diktat-ruleset:inline-classes", "${Warnings.INLINE_CLASS_CAN_BE_USED.warnText()} class Some"))
        )
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `smoke test #4`() {
        fixAndCompare("Example4Expected.kt", "Example4Test.kt")
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `smoke test #3`() {
        fixAndCompare("Example3Expected.kt", "Example3Test.kt")
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `regression - shouldn't throw exception in cases similar to #371`() {
        fixAndCompare("Bug1Expected.kt", "Bug1Test.kt")
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `smoke test #2`() {
        fixAndCompare("Example2Expected.kt", "Example2Test.kt")
        unfixedLintErrors.assertEquals(
            LintError(1, 1, "$DIKTAT_RULE_SET_ID:header-comment", "${HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE.warnText()} there are 2 declared classes and/or objects", false),
            LintError(15, 23, "$DIKTAT_RULE_SET_ID:kdoc-methods",
                "${KDOC_WITHOUT_PARAM_TAG.warnText()} createWithFile (containerName)", true),
            LintError(31, 13, "$DIKTAT_RULE_SET_ID:empty-block-structure",
                "${EMPTY_BLOCK_STRUCTURE_ERROR.warnText()} empty blocks are forbidden unless it is function with override keyword", false)
        )
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `smoke test #1`() {
        fixAndCompare("Example1Expected.kt", "Example1Test.kt")
        unfixedLintErrors.assertEquals(
            LintError(1, 1, "$DIKTAT_RULE_SET_ID:file-naming", "${FILE_NAME_MATCH_CLASS.warnText()} Example1Test.kt vs Example", true),
            LintError(1, 1, "$DIKTAT_RULE_SET_ID:kdoc-formatting", "${KDOC_NO_EMPTY_TAGS.warnText()} @return", false),
            LintError(3, 6, "$DIKTAT_RULE_SET_ID:kdoc-comments", "${MISSING_KDOC_TOP_LEVEL.warnText()} Example", false),
            LintError(3, 26, "$DIKTAT_RULE_SET_ID:kdoc-comments", "${MISSING_KDOC_CLASS_ELEMENTS.warnText()} isValid", false),
            LintError(6, 9, "$DIKTAT_RULE_SET_ID:kdoc-comments", "${MISSING_KDOC_CLASS_ELEMENTS.warnText()} foo", false),
            LintError(8, 8, "$DIKTAT_RULE_SET_ID:kdoc-comments", "${MISSING_KDOC_CLASS_ELEMENTS.warnText()} foo", false),
            LintError(8, 8, "$DIKTAT_RULE_SET_ID:kdoc-methods", "${MISSING_KDOC_ON_FUNCTION.warnText()} foo", false),
            LintError(9, 3, "$DIKTAT_RULE_SET_ID:empty-block-structure", EMPTY_BLOCK_STRUCTURE_ERROR.warnText() +
                    " empty blocks are forbidden unless it is function with override keyword", false),
            LintError(12, 10, "$DIKTAT_RULE_SET_ID:kdoc-formatting", "${KDOC_NO_EMPTY_TAGS.warnText()} @return", false),
            LintError(14, 3, "$DIKTAT_RULE_SET_ID:kdoc-formatting", "${KDOC_NO_EMPTY_TAGS.warnText()} @return", false),
            LintError(19, 15, "$DIKTAT_RULE_SET_ID:kdoc-formatting", "${KDOC_NO_EMPTY_TAGS.warnText()} @return", false)
        )
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `smoke test with kts files`() {
        overrideRulesConfig(
            listOf(
                HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE  // because build.gradle.kts doesn't need extra comments, and this rule can be manually disabled if needed
            ),
            mapOf(
                Warnings.WRONG_INDENTATION.name to mapOf(
                    "newlineAtEnd" to "false",
                    "extendedIndentOfParameters" to "false",
                )
            )
        )  // so that trailing newline isn't checked
        // file name is `gradle_` so that IDE doesn't suggest to import gradle project
        fixAndCompare("../../../build.gradle_.kts", "../../../build.gradle_.kts")
        Assertions.assertTrue(unfixedLintErrors.isEmpty())
    }

    @Test
    @Tag("DiktatRuleSetProvider")
    fun `disable charters`() {
        overrideRulesConfig(
            emptyList(),
            mapOf(
                DIKTAT_COMMON to mapOf(
                    "domainName" to "org.cqfn.diktat",
                    "disabledChapters" to "Naming,3,4,5,Classes"
                )
            )
        )
        fixAndCompare("Example1-2Expected.kt", "Example1Test.kt")
        unfixedLintErrors.assertEquals(
            LintError(1, 1, "$DIKTAT_RULE_SET_ID:kdoc-formatting", "${KDOC_NO_EMPTY_TAGS.warnText()} @return", false),
            LintError(3, 1, "$DIKTAT_RULE_SET_ID:kdoc-comments", "${MISSING_KDOC_TOP_LEVEL.warnText()} example", false),
            LintError(3, 16, "$DIKTAT_RULE_SET_ID:kdoc-comments", "${MISSING_KDOC_CLASS_ELEMENTS.warnText()} isValid", false),
            LintError(6, 5, "$DIKTAT_RULE_SET_ID:kdoc-comments", "${MISSING_KDOC_CLASS_ELEMENTS.warnText()} foo", false),
            LintError(6, 5, "$DIKTAT_RULE_SET_ID:kdoc-methods", "${MISSING_KDOC_ON_FUNCTION.warnText()} foo", false),
            LintError(8, 5, "$DIKTAT_RULE_SET_ID:kdoc-comments", "${MISSING_KDOC_CLASS_ELEMENTS.warnText()} foo", false),
            LintError(10, 4, "$DIKTAT_RULE_SET_ID:kdoc-formatting", "${KDOC_NO_EMPTY_TAGS.warnText()} @return", false),
            LintError(13, 9, "$DIKTAT_RULE_SET_ID:kdoc-formatting", "${KDOC_NO_EMPTY_TAGS.warnText()} @return", false),
            LintError(18, 40, "$DIKTAT_RULE_SET_ID:kdoc-formatting", "${KDOC_NO_EMPTY_TAGS.warnText()} @return", false)
        )
    }

    companion object {
        private const val DEFAULT_CONFIG_PATH = "../diktat-analysis.yml"
        private val unfixedLintErrors: MutableList<LintError> = mutableListOf()

        // by default using same yml config as for diktat code style check, but allow to override
        private var configFilePath = DEFAULT_CONFIG_PATH
    }
}
