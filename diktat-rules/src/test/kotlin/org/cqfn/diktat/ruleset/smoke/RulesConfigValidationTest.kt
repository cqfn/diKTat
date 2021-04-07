package org.cqfn.diktat.ruleset.smoke

import org.cqfn.diktat.ruleset.rules.DiktatRuleSetProvider

import com.charleskorn.kaml.InvalidPropertyValueException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import java.io.File
import java.lang.IllegalArgumentException
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempFile

@OptIn(ExperimentalPathApi::class)
class RulesConfigValidationTest {
    private lateinit var file: File

    @BeforeEach
    fun setUp() {
        file = createTempFile().toFile()
    }

    @AfterEach
    fun tearDown() {
        file.delete()
    }

    @Test
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    fun `should throw error if name is missing in Warnings`() {
        file.writeText(
            """
                |- name: MISSING_DOC_TOP_LEVEL
                |  enabled: true
                |  configuration: {}
            """.trimMargin()
        )
        val exception = assertThrows<IllegalArgumentException> {
            DiktatRuleSetProvider(file.absolutePath).get()
        }
        Assertions.assertEquals("Warning name <MISSING_DOC_TOP_LEVEL> in configuration file is invalid, did you mean <MISSING_KDOC_TOP_LEVEL>?", exception.message)
    }

    @Test
    fun `should throw error on invalid yml config`() {
        file.writeText(
            """
                |- name: PACKAGE_NAME_MISSING
                |  enabled: true
                |  configuration:
            """.trimMargin()
        )
        assertThrows<InvalidPropertyValueException> {
            DiktatRuleSetProvider(file.absolutePath).get()
        }
    }

    @Test
    @Disabled("https://github.com/cqfn/diKTat/issues/395")
    fun `should throw error on invalid configuration section`() {
        file.writeText(
            """
                |- name: TOO_LONG_FUNCTION
                |  enabled: true
                |  configuration:
                |    maxFunctionLength: 1o
                |    isIncludeHeader: Fslse
            """.trimMargin()
        )
        DiktatRuleSetProvider(file.absolutePath).get()
    }
}
