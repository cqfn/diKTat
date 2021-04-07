package org.cqfn.diktat.plugin.gradle

import org.cqfn.diktat.plugin.gradle.DiktatGradlePlugin.Companion.DIKTAT_CHECK_TASK
import org.cqfn.diktat.plugin.gradle.DiktatGradlePlugin.Companion.DIKTAT_FIX_TASK
import org.cqfn.diktat.ruleset.rules.DIKTAT_CONF_PROPERTY
import org.cqfn.diktat.ruleset.utils.log

import generated.DIKTAT_VERSION
import generated.KTLINT_VERSION
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.VerificationTask
import org.gradle.util.GradleVersion

import java.io.File
import javax.inject.Inject

/**
 * A base diktat task for gradle <6.8, which wraps [JavaExec].
 *
 * Note: class being `open` is required for gradle to create a task.
 */
open class DiktatJavaExecTaskBase @Inject constructor(
    gradleVersionString: String,
    diktatExtension: DiktatExtension,
    diktatConfiguration: Configuration,
    additionalFlags: Iterable<String> = emptyList()
) : JavaExec(), VerificationTask {
    /**
     * A backing [Property] for [getIgnoreFailures] and [setIgnoreFailures]
     */
    @get:Internal
    internal val ignoreFailuresProp: Property<Boolean> = project.objects.property(Boolean::class.javaObjectType)

    /**
     * Whether diktat should be executed via JavaExec or not.
     */
    @get:Internal
    internal var shouldRun = true

    init {
        group = "verification"
        if (isMainClassPropertySupported(gradleVersionString)) {
            // `main` is deprecated and replaced with `mainClass` since gradle 6.4
            mainClass.set("com.pinterest.ktlint.Main")
        } else {
            main = "com.pinterest.ktlint.Main"
        }
        // Plain, checkstyle and json reporter are provided out of the box in ktlint
        if (diktatExtension.reporterType == "html") {
            diktatConfiguration.dependencies.add(project.dependencies.create("com.pinterest.ktlint:ktlint-reporter-html:$KTLINT_VERSION"))
        }
        classpath = diktatConfiguration
        project.logger.debug("Setting diktatCheck classpath to ${diktatConfiguration.dependencies.toSet()}")
        if (diktatExtension.debug) {
            logger.lifecycle("Running diktat $DIKTAT_VERSION with ktlint $KTLINT_VERSION")
        }
        ignoreFailures = diktatExtension.ignoreFailures
        isIgnoreExitValue = ignoreFailures  // ignore returned value of JavaExec started process if lint errors shouldn't fail the build
        systemProperty(DIKTAT_CONF_PROPERTY, resolveConfigFile(diktatExtension.diktatConfigFile).also {
            logger.info("Setting system property for diktat config to $it")
        })
        args = additionalFlags.toMutableList().apply {
            if (diktatExtension.debug) {
                add("--debug")
            }
            diktatExtension
                .inputs
                .also {
                    if (it.isEmpty) {
                        /*
                         If ktlint receives empty patterns, it implicitly uses &#42;&#42;/*.kt, **/*.kts instead.
                         This can lead to diktat analyzing gradle buildscripts and so on. We want to prevent it.
                        */
                        logger.warn("Inputs for $name do not exist, will not run diktat")
                        shouldRun = false
                    }
                }
                .files
                .forEach {
                    addPattern(it)
                }
            diktatExtension.excludes.files.forEach {
                addPattern(it, negate = true)
            }

            add(createReporterFlag(diktatExtension))
        }
        logger.debug("Setting JavaExec args to $args")
    }

    @TaskAction
    override fun exec() {
        if (shouldRun) {
            super.exec()
        } else {
            logger.info("Skipping diktat execution")
        }
    }

    /**
     * @param ignoreFailures whether failure in this plugin should be ignored by a build
     */
    override fun setIgnoreFailures(ignoreFailures: Boolean) = ignoreFailuresProp.set(ignoreFailures)

    /**
     * @return whether failure in this plugin should be ignored by a build
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    override fun getIgnoreFailures(): Boolean = ignoreFailuresProp.getOrElse(false)

    private fun createReporterFlag(diktatExtension: DiktatExtension): String {
        val flag: StringBuilder = StringBuilder()

        // Plain, checkstyle and json reporter are provided out of the box in ktlint
        when (diktatExtension.reporterType) {
            "json" -> flag.append("--reporter=json")
            "html" -> flag.append("--reporter=html")
            "checkstyle" -> flag.append("--reporter=checkstyle")
            else -> customReporter(diktatExtension, flag)
        }

        if (diktatExtension.output.isNotEmpty()) {
            flag.append(",output=${diktatExtension.output}")
        }

        return flag.toString()
    }

    private fun customReporter(diktatExtension: DiktatExtension, flag: java.lang.StringBuilder) {
        if (diktatExtension.reporterType.startsWith("custom")) {
            val name = diktatExtension.reporterType.split(":")[1]
            val jarPath = diktatExtension.reporterType.split(":")[2]
            if (name.isEmpty() || jarPath.isEmpty()) {
                log.warn("Either name or path to jar is not specified. Falling to plain reporter")
                flag.append("--reporter=plain")
            } else {
                flag.append("--reporter=$name,artifact=$jarPath")
            }
        } else {
            flag.append("--reporter=plain")
            log.debug("Unknown reporter was specified. Falling back to plain reporter.")
        }
    }

    @Suppress("MagicNumber")
    private fun isMainClassPropertySupported(gradleVersionString: String) =
            GradleVersion.version(gradleVersionString) >= GradleVersion.version("6.4")

    private fun MutableList<String>.addPattern(pattern: File, negate: Boolean = false) {
        log.info("pattern - pattern - pattern: $pattern")
        val path = if (pattern.isAbsolute) {
            pattern.relativeTo(project.projectDir).normalize()
        } else {
            pattern
        }
            .invariantSeparatorsPath
        log.info("path - path - path: $path")
        log.info("projectDir - projectDir - projectDir: ${project.projectDir}")
        add((if (negate) "!" else "") + path)
    }

    private fun resolveConfigFile(file: File): String {
        if (file.toPath().startsWith(project.rootDir.toPath())) {
            // In gradle, project.files() returns File relative to project.projectDir.
            // There is no need to resolve file further if it has been passed via gradle files API.
            return file.absolutePath
        }

        // otherwise, e.g. if file is passed as java.io.File with relative path, we try to find it
        return generateSequence(project.projectDir) { it.parentFile }
            .map { it.resolve(file) }
            .run {
                firstOrNull { it.exists() } ?: first()
            }
            .absolutePath
    }
}

/**
 * @param diktatExtension [DiktatExtension] with some values for task configuration
 * @param diktatConfiguration dependencies of diktat run
 * @return a [TaskProvider]
 */
fun Project.registerDiktatCheckTask(diktatExtension: DiktatExtension, diktatConfiguration: Configuration): TaskProvider<DiktatJavaExecTaskBase> =
        tasks.register(
            DIKTAT_CHECK_TASK, DiktatJavaExecTaskBase::class.java, gradle.gradleVersion,
            diktatExtension, diktatConfiguration
        )

/**
 * @param diktatExtension [DiktatExtension] with some values for task configuration
 * @param diktatConfiguration dependencies of diktat run
 * @return a [TaskProvider]
 */
fun Project.registerDiktatFixTask(diktatExtension: DiktatExtension, diktatConfiguration: Configuration): TaskProvider<DiktatJavaExecTaskBase> =
        tasks.register(
            DIKTAT_FIX_TASK, DiktatJavaExecTaskBase::class.java, gradle.gradleVersion,
            diktatExtension, diktatConfiguration, listOf("-F ")
        )
