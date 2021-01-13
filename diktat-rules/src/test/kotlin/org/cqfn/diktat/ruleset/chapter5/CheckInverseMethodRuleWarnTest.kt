package org.cqfn.diktat.ruleset.chapter5

import com.pinterest.ktlint.core.LintError
import org.cqfn.diktat.ruleset.constants.Warnings
import org.cqfn.diktat.ruleset.rules.CheckInverseMethodRule
import org.cqfn.diktat.ruleset.rules.DIKTAT_RULE_SET_ID
import org.cqfn.diktat.util.LintTestBase
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class CheckInverseMethodRuleWarnTest : LintTestBase(::CheckInverseMethodRule) {
    private val ruleId = "$DIKTAT_RULE_SET_ID:inverse-method"

    @Test
    @Tag("STUB")
    fun `should not raise warning`() {
        lintMethod(
                """
                    |fun some() {
                    |   if (list.isEmpty()) {
                    |       // some cool logic 
                    |   }
                    |}
                """.trimMargin()
        )
    }

    @Test
    @Tag("STUB")
    fun `should raise warning`() {
        lintMethod(
                """
                    |fun some() {
                    |   if (!list.isEmpty()) {
                    |       // some cool logic 
                    |   }
                    |}
                """.trimMargin(),
                LintError(2, 14, ruleId, "${Warnings.INVERSE_FUNCTION_PREFERRED.warnText()} isNotEmpty() instead of !isEmpty()", true)
        )
    }
}