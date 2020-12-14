package org.cqfn.diktat.ruleset.generation

import org.cqfn.diktat.ruleset.utils.autoEnd
import org.cqfn.diktat.ruleset.utils.autoTable
import java.io.File


fun main() {
    generateAvailableRules()
}

@Suppress("MagicNumber")
private fun generateAvailableRules() {
    val ruleMap = File("info/rules-mapping.md").readLines()
        .drop(2)
        .map { it.drop(1).dropLast(1).split("|") }
        .map { RuleDescription(it[0].replace("\\s+".toRegex(), ""), it[1], it[2], null) }
        .associateBy { it.ruleName }
    File("info/available-rules.md").readLines()
        .drop(2)
        .map { it.drop(1).dropLast(1).split("|") }
        .map { it[2].replace("\\s+".toRegex(), "") to it[5] }
        .forEach { ruleMap[it.first]!!.config = it.second}
    val newText = File("wp/sections/appendix.tex").readLines().toMutableList()
    newText.removeAll(newText.subList(newText.indexOf("\\section*{available-rules}") + 1,newText.indexOf("\\section*{\\textbf{Diktat Coding Convention}}")))
    var index = newText.indexOf("\\section*{available-rules}") + 1
    autoTable.trimIndent().split("\n").forEach { newText.add(index++, it) }
    ruleMap.map { it.value }
            .map { "${it.correctRuleName} & ${it.correctCodeStyle} & ${it.autoFix} & ${it.correctConfig}\\\\" }
            .forEach { newText.add(index++, it) }
    autoEnd.trimIndent().split("\n").forEach { newText.add(index++, it) }
    File("wp/sections/appendix.tex").writeText(newText.joinToString(separator = "\n"))
}

@Suppress("UnsafeCallOnNullableType")
data class RuleDescription(val ruleName: String, val codeStyle: String, val autoFix: String, var config: String?) {
    val correctCodeStyle = codeStyle.substring(codeStyle.indexOf("[") + 1, codeStyle.indexOf("]"))
    val correctRuleName = ruleName.replace("_", "\\underline{ }")
    val correctConfig: String by lazy {
        config!!.replace("<br>", " ")
    }
}
