package org.cqfn.diktat.ruleset.chapter3

import org.cqfn.diktat.ruleset.rules.chapter3.RangeConventionalRule
import org.cqfn.diktat.util.FixTestBase
import org.junit.jupiter.api.Test

class RangeConventionalRuleFixTest : FixTestBase("test/paragraph3/range", ::RangeConventionalRule) {
    @Test
    fun `should fix with until`() {
        fixAndCompare("RangeToUntilExpected.kt", "RangeToUntilTest.kt")
    }

    @Test
    fun `should fix with rangeTo`() {
        fixAndCompare("RangeToExpected.kt", "RangeToTest.kt")
    }
}
