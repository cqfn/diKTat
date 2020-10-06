package org.cqfn.diktat.ruleset.chapter5

import com.pinterest.ktlint.core.LintError
import generated.WarningNames
import org.cqfn.diktat.ruleset.rules.DIKTAT_RULE_SET_ID
import org.cqfn.diktat.ruleset.constants.Warnings.WRONG_OVERLOADING_FUNCTION_ARGUMENTS
import org.cqfn.diktat.ruleset.rules.OverloadingArgumentsFunction
import org.cqfn.diktat.util.LintTestBase
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class OverloadingArgumentsFunctionWarnTest : LintTestBase(::OverloadingArgumentsFunction) {

    private val ruleId = "$DIKTAT_RULE_SET_ID:overloading-default-values"

    @Test
    @Tag(WarningNames.WRONG_OVERLOADING_FUNCTION_ARGUMENTS)
    fun `check simple example`() {
        lintMethod(
                """
                    |fun foo() {}
                    |
                    |fun foo(a: Int) {}
                    |
                    |fun goo(a: Double) {}
                    |
                    |fun goo(a: Float, b: Double) {}
                    |
                    |fun goo(b: Float, a: Double, c: Int) {}
                    |
                    |@Suppress("WRONG_OVERLOADING_FUNCTION_ARGUMENTS")
                    |fun goo(a: Float)
                    |
                    |fun goo(a: Double? = 0.0) {}
                    |
                    |override fun goo() {}
                    |
                    |class A {
                    |   fun foo() {}
                    |}
                    |
                    |abstract class B {
                    |   abstract fun foo(a: Int)
                    |   
                    |   fun foo(){}
                    |}
                """.trimMargin(),
                LintError(1,1, ruleId, "${WRONG_OVERLOADING_FUNCTION_ARGUMENTS.warnText()} foo", false),
                LintError(16,1, ruleId, "${WRONG_OVERLOADING_FUNCTION_ARGUMENTS.warnText()} goo", false),
                LintError(25,4, ruleId, "${WRONG_OVERLOADING_FUNCTION_ARGUMENTS.warnText()} foo", false)
        )
    }
}
