package org.cqfn.diktat.plugin.gradle

import generated.DIKTAT_VERSION
import generated.KTLINT_VERSION
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.attributes.Bundling

/**
 * Plugin that configures diktat and registers tasks to run diktat
 */
@Suppress("unused", "MagicNumber")
class DiktatGradlePlugin : Plugin<Project> {
    /**
     * @param project a gradle [Project] that the plugin is applied to
     */
    override fun apply(project: Project) {
        val diktatExtension = project.extensions.create(DIKTAT_EXTENSION, DiktatExtension::class.java).apply {
            inputs = project.fileTree("src").apply {
                include("**/*.kt")
            }
            diktatConfigFile = project.rootProject.file("diktat-analysis.yml")
            excludes = project.files()
        }

        // only gradle 7+ (or maybe 6.8) will embed kotlin 1.4+, kx.serialization is incompatible with kotlin 1.3, so until then we have to use JavaExec wrapper
        // FixMe: when gradle with kotlin 1.4 is out, proper configurable tasks should be added
        // configuration to provide JavaExec with correct classpath
        val diktatConfiguration = project.configurations.create(DIKTAT_CONFIGURATION) { configuration ->
            configuration.isVisible = false
            configuration.dependencies.add(project.dependencies.create("com.pinterest:ktlint:$KTLINT_VERSION", closureOf<ExternalModuleDependency> {
                exclude(
                    mutableMapOf(
                        "group" to "com.pinterest.ktlint",
                        "module" to "ktlint-ruleset-standard"
                    )
                )
                attributes {
                    it.attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling::class.java, Bundling.EXTERNAL))
                }
            }))
            configuration.dependencies.add(project.dependencies.create("org.cqfn.diktat:diktat-rules:$DIKTAT_VERSION"))
        }

        project.registerDiktatCheckTask(diktatExtension, diktatConfiguration)
        project.registerDiktatFixTask(diktatExtension, diktatConfiguration)
    }

    companion object {
        const val DIKTAT_CHECK_TASK = "diktatCheck"
        const val DIKTAT_CONFIGURATION = "diktat"
        const val DIKTAT_EXTENSION = "diktat"
        const val DIKTAT_FIX_TASK = "diktatFix"
    }
}
