package org.cqfn.diktat.ruleset.utils

import com.pinterest.ktlint.core.ast.ElementType.FILE
import org.cqfn.diktat.ruleset.utils.search.findAllVariablesWithAssignments
import org.cqfn.diktat.ruleset.utils.search.findAllVariablesWithShadowing
import org.cqfn.diktat.ruleset.utils.search.findAllVariablesWithUsages
import org.cqfn.diktat.util.applyToCode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Suppress("UnsafeCallOnNullableType")
class VariablesWithShadowingSearchTest {
    @Test
    fun `check for simple shadowing in while block`() {
        applyToCode("""
            fun main() {
                var x = 1
                while (x < 10) {
                    var x = 0
                    x++
                }
            }
        """.trimIndent(), 0) { node, _ ->
            if (node.elementType == FILE) {
                val vars = node.findAllVariablesWithShadowing().mapKeys { it.key.text }
                val keys = vars.keys
                val var1 = keys.elementAt(0)
                assertEquals("var x = 1", var1)
                assertEquals(1, vars[var1]?.size)
            }
        }
    }

    @Test
    fun `check for shadowing of function arguments`() {
        applyToCode("""
        fun foo(a: Int) {
            val a = 10
        } 
        """.trimIndent(), 0) { node, _ ->
            if (node.elementType == FILE) {
                val vars = node.findAllVariablesWithAssignments().mapKeys { it.key.text }
                val keys = vars.keys
                val var1 = keys.elementAt(0)
                Assertions.assertEquals("var o = 1", var1)
                Assertions.assertEquals(2, vars[var1]?.size)
            }
        }
    }

    @Test
    fun `check for shadowing with lambda args`() {
        applyToCode("""
            fun foo() {
                val a = 10
                val b = emptyList<String>()
                b.forEach { a ->
                    println(a)
                }
            } 
        """.trimIndent(), 0) { node, _ ->
            if (node.elementType == FILE) {
                val vars = node.findAllVariablesWithAssignments().mapKeys { it.key.text }
                val keys = vars.keys
                val var1 = keys.elementAt(0)
                Assertions.assertEquals("val a = 10", var1)
                Assertions.assertEquals(1, vars[var1]?.size)
            }
        }
    }

    @Test
    fun `check for shadowing class properties`() {
        applyToCode("""
        class A {
            val a = 10
            fun foo() {
                val a = 11
            }
        }
        """.trimIndent(), 0) { node, _ ->
            if (node.elementType == FILE) {
                val vars = node.findAllVariablesWithAssignments().mapKeys { it.key.text }
                val keys = vars.keys
                val var1 = keys.elementAt(0)
                assertEquals("val a = 10", var1)
                assertEquals(2, vars[var1]?.size)
            }
        }
    }
}
