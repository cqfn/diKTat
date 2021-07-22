package org.cqfn.diktat.ruleset.chapter3

import org.cqfn.diktat.common.config.rules.RulesConfig
import org.cqfn.diktat.ruleset.constants.Warnings
import org.cqfn.diktat.ruleset.rules.DIKTAT_RULE_SET_ID
import org.cqfn.diktat.ruleset.rules.chapter3.RangeConventionalRule
import org.cqfn.diktat.util.LintTestBase

import com.pinterest.ktlint.core.LintError
import org.junit.jupiter.api.Test

class RangeConventionalRuleWarnTest : LintTestBase(::RangeConventionalRule) {
    private val ruleId = "$DIKTAT_RULE_SET_ID:range"
    private val rulesConfigRangeRule: List<RulesConfig> = listOf(
        RulesConfig(
            Warnings.CONVENTIONAL_RANGE.name, true,
            mapOf("isRangeToIgnore" to "true"))
    )

    @Test
    fun `check simple examples with until`() {
        lintMethod(
            """
                    |fun foo() {   
                    |    for (i in 1..(4 - 1)) print(i)
                    |    for (i in 1..(b - 1)) print(i)
                    |    for (i in ((1 .. ((4 - 1))))) print(i)
                    |    for (i in 4 downTo 1) print(i)
                    |    for (i in 1..4) print(i)
                    |    for (i in 1..4 step 2) print(i)
                    |    for (i in 4 downTo 1 step 3) print(i)
                    |    if (6 in (1..10) && true) {}
                    |    for (i in 1..(4 - 1) step 3) print(i)
                    |}
                """.trimMargin(),
            LintError(2, 15, ruleId, "${Warnings.CONVENTIONAL_RANGE.warnText()} replace `..` with `until`: 1..(4 - 1)", true),
            LintError(3, 15, ruleId, "${Warnings.CONVENTIONAL_RANGE.warnText()} replace `..` with `until`: 1..(b - 1)", true),
            LintError(4, 17, ruleId, "${Warnings.CONVENTIONAL_RANGE.warnText()} replace `..` with `until`: 1 .. ((4 - 1))", true),
            LintError(10, 15, ruleId, "${Warnings.CONVENTIONAL_RANGE.warnText()} replace `..` with `until`: 1..(4 - 1)", true)
        )
    }

    @Test
    fun `check simple examples with rangeTo`() {
        lintMethod(
            """
                    |fun foo() {   
                    |    val num = 1
                    |    val w = num.rangeTo(num, num)
                    |    val q = 1..5
                    |    val w = num.rangeTo(num)
                    |}
                """.trimMargin(),
            LintError(5, 13, ruleId, "${Warnings.CONVENTIONAL_RANGE.warnText()} replace `rangeTo` with `..`: num.rangeTo(num)", true)
        )
    }

    @Test
    fun `check simple examples with rangeTo with config`() {
        lintMethod(
            """
                    |fun foo() {   
                    |    val w = num.rangeTo(num, num)
                    |    val w = num.rangeTo(num)
                    |}
                """.trimMargin(),
            rulesConfigList = rulesConfigRangeRule
        )
    }
}
