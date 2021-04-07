<img src="/logo.svg" width="64px"/>

![Build and test](https://github.com/cqfn/diKTat/workflows/Build%20and%20test/badge.svg)
![deteKT static analysis](https://github.com/cqfn/diKTat/workflows/Run%20deteKT/badge.svg)
![diKTat code style](https://github.com/cqfn/diKTat/workflows/Run%20diKTat%20from%20release%20version/badge.svg?branch=master)
[![License](https://img.shields.io/github/license/cqfn/diKtat)](https://github.com/cqfn/diKTat/blob/master/LICENSE)
[![codecov](https://codecov.io/gh/cqfn/diKTat/branch/master/graph/badge.svg)](https://codecov.io/gh/cqfn/diKTat)

[![Releases](https://img.shields.io/github/v/release/cqfn/diKTat)](https://github.com/cqfn/diKTat/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.cqfn.diktat/diktat-rules)](https://mvnrepository.com/artifact/org.cqfn.diktat)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fcqfn%2FdiKTat.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fcqfn%2FdiKTat?ref=badge_shield)
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
[![Chat on Telegram](https://img.shields.io/badge/Chat%20on-Telegram-brightgreen.svg)](https://t.me/joinchat/AAAAAFDg-ipuZFGyBGPPeg)

[![Hits-of-Code](https://hitsofcode.com/github/cqfn/diktat)](https://hitsofcode.com/view/github/cqfn/diktat)
![Lines of code](https://img.shields.io/tokei/lines/github/cqfn/diktat)
![GitHub repo size](https://img.shields.io/github/repo-size/cqfn/diktat)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin)


DiKTat is a strict [coding standard ](info/guide/diktat-coding-convention.md) for Kotlin and a collection of [Kotlin](https://kotlinlang.org/) code style rules implemented
as AST visitors on the top of [KTlint](https://ktlint.github.io/). It can be used for detecting and autofixing code smells in CI/CD process. 
The full list of available supported rules and inspections can be found [here](info/available-rules.md).

Now diKTat was already added to the lists of [static analysis tools](https://github.com/analysis-tools-dev/static-analysis) and to [kotlin-awesome](https://github.com/KotlinBy/awesome-kotlin). Thanks to the community for this support! 

## See first

|  |  |  |  |  |  |  
| --- | --- | --- | --- | --- | --- |
|[Codestyle](info/guide/diktat-coding-convention.md)|[Inspections](info/available-rules.md) | [Examples](examples) | [Demo](https://ktlint-demo.herokuapp.com) | [White Paper](wp/wp.pdf) | [Groups of Inspections](info/rules-mapping.md) |

## Why should I use diktat in my CI/CD?

There are several tools like `detekt` and `ktlint` that are doing static analysis. Why do I need diktat?

First of all - actually you can combine diktat with any other static analyzers. And diKTat is even using ktlint framework for parsing the code into the AST.
And we are trying to contribute to those projects. 
Main features of diktat are the following:

1) **More inspections.** It has 100+ inspections that are tightly coupled with it's codestyle.

2) **Unique inspections** that are missing in other linters.

3) **Highly configurable**. Each and every inspection can be configured and suppressed both from the code or from the configuration file.

4) **Strict detailed coding convention** that you can use in your project.

## Run as CLI-application
1. Install KTlint manually: [here](https://github.com/pinterest/ktlint/releases)

   **OR** use curl:
    ```bash
    curl -sSLO https://github.com/pinterest/ktlint/releases/download/0.39.0/ktlint && chmod a+x ktlint
    # another option is "brew install ktlint"
    ```
   
2. Load diKTat manually: [here](https://github.com/cqfn/diKTat/releases/download/v0.4.2/diktat.jar)

   **OR** use curl:
   ```bash
   $ curl -sSLO https://github.com/cqfn/diKTat/releases/download/v0.4.2/diktat-0.4.2.jar
   ```
   
3. Finally, run KTlint (with diKTat injected) to check your `*.kt` files in `dir/your/dir`:
   ```bash
   $ ./ktlint -R diktat.jar --disabled_rules=standard "dir/your/dir/**/*.kt"
   ```

To **autofix** all code style violations use `-F` option.

## Run with Maven using diktat-maven-plugin
This plugin is available since version 0.1.3. You can see how it is configured in our project for self-checks: [pom.xml](pom.xml).
If you use it and encounter any problems, feel free to open issues on [github](https://github.com/cqfn/diktat/issues).

Add this plugin to your pom.xml:
```xml
            <plugin>
                <groupId>org.cqfn.diktat</groupId>
                <artifactId>diktat-maven-plugin</artifactId>
                <version>${diktat.version}</version>
                <executions>
                    <execution>
                        <id>diktat</id>
                        <phase>none</phase>
                        <goals>
                            <goal>check</goal>
                            <goal>fix</goal>
                        </goals>
                        <configuration>
                            <inputs>
                                <input>${project.basedir}/src/main/kotlin</input>
                                <input>${project.basedir}/src/test/kotlin</input>
                            </inputs>
                            <diktatConfigFile>diktat-analysis.yml</diktatConfigFile>
                           <excludes>
                              <exclude>${project.basedir}/src/test/kotlin/excluded</exclude>
                           </excludes>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

To run diktat in **only-check** mode use command `$ mvn diktat:check@diktat`.
To run diktat in **autocorrect** mode use command `$ mvn diktat:fix@diktat`.

## Run with Gradle using diktat-gradle-plugin
Requires a gradle version no lower than 5.3.

This plugin is available since version 0.1.5. You can see how the plugin is configured in our examples: [build.gradle.kts](examples/gradle-kotlin-dsl/build.gradle.kts).
Add this plugin to your `build.gradle.kts`:
```kotlin
plugins {
    id("org.cqfn.diktat.diktat-gradle-plugin") version "0.4.2"
}
```

Or use buildscript syntax:
```kotlin
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.cqfn.diktat:diktat-gradle-plugin:0.4.2")
    }
}

apply(plugin = "org.cqfn.diktat.diktat-gradle-plugin")
```

You can then configure diktat using `diktat` extension:
```kotlin
diktat {
    inputs = files("src/**/*.kt")  // file collection that will be checked by diktat
    debug = true  // turn on debug logging
    excludes = files("src/test/kotlin/excluded")  // these files will not be checked by diktat
}
```

Also `diktat` extension has different reporters. You can specify `json`, `html`, `checkstyle`, `plain` (default) or your own custom reporter:
```kotlin
diktat {
    reporter = "json" // "html", "checkstyle", "plain"
}
```

Example of your custom reporter:
```kotlin
diktat {
    reporter = "custom:name:pathToJar"
}
```
Name parameter is the name of your reporter and as the last parameter you should specify path to jar, which contains your reporter.
[Example of the junit custom reporter.](https://github.com/kryanod/ktlint-junit-reporter)

You can also specify an output. 
```kotlin
diktat {
    reporter = "json"
    output = "someFile.json"
}
```

You can run diktat checks using task `diktatCheck` and automatically fix errors with tasks `diktatFix`.

## Run with Spotless
[Spotless](https://github.com/diffplug/spotless) is a linter aggregator.

### Gradle
Diktat can be run via spotless-gradle-plugin since version 5.10.0

<details>
<summary>Add this plugin to your build.gradle.kts</summary>

```kotlin
plugins {
   id("com.diffplug.spotless") version "5.10.0"
}

spotless {
   kotlin {
      diktat()
   }
   kotlinGradle {
      diktat()
   }
}
```
</details>

<details>
<summary>You can provide a version and configuration path manually as configFile.</summary>

```kotlin
spotless {
   kotlin {
      diktat("0.4.2").configFile("full/path/to/diktat-analysis.yml")
   }
}
```
</details>

### Maven
Diktat can be run via spotless-maven-plugin since version 2.8.0

<details>
<summary>Add this plugin to your pom.xml</summary>

```xml
<plugin>
   <groupId>com.diffplug.spotless</groupId>
   <artifactId>spotless-maven-plugin</artifactId>
   <version>${spotless.version}</version>
   <configuration>
      <kotlin>
         <diktat />
      </kotlin>
   </configuration>
</plugin>
```
</details>

<details>
<summary>You can provide a version and configuration path manually as configFile</summary>

```xml
<diktat>
  <version>0.4.2</version> <!-- optional -->
  <configFile>full/path/to/diktat-analysis.yml</configFile> <!-- optional, configuration file path -->
</diktat>
```
</details>

## Customizations via `diktat-analysis.yml`

In KTlint, rules can be configured via `.editorconfig`, but
this does not give a chance to customize or enable/disable
each and every rule independently.
That is why we have supported `diktat-analysis.yml` that can be easily
changed and help in customization of your own rule set.
It has simple fields:
`name` — name of the rule,
`enabled` (true/false) — to enable or disable that rule (all rules are enabled by the default),
`configuration` — a simple map of some extra unique configurations for this particular rule.
For example:

```yaml
- name: HEADER_MISSING_OR_WRONG_COPYRIGHT
  # all rules are enabled by the default. To disable add 'enabled: false' to the config.
  enabled: true 
  configuration:
    isCopyrightMandatory: true
    copyrightText: Copyright (c) Jeff Lebowski, 2012-2020. All rights reserved.
```
Note, that you can specify and put `diktat-analysis.yml` that contains configuration of diktat in the parent directory of your project on the same level where `build.gradle/pom.xml` is stored. \
See default configuration in [diktat-analysis.yml](diktat-rules/src/main/resources/diktat-analysis.yml) \
Also see [the list of all rules supported by diKTat](info/available-rules.md).

## Suppress warnings on individual code blocks
In addition to enabling/disabling warning globally via config file (`enable = false`), you can suppress warnings by adding `@Suppress` annotation on individual code blocks

For example:

``` kotlin
@Suppress("FUNCTION_NAME_INCORRECT_CASE")
class SomeClass {
    fun methODTREE(): String {

    }
}
``` 
## How to contribute?

Main components are:
1) diktat-rules — number of rules that are supported by diKTat;
2) diktat-test-framework — functional/unit test framework that can be used for running your code fixer on the initial code and compare it with the expected result;
3) also see our demo: diktat-demo in a separate repository.

Mainly we wanted to create a common configurable mechanism that
will give us a chance to enable/disable and customize all rules.
That's why we added logic for:
1) Parsing `.yml` file with configurations of rules and passing it to visitors;
2) Passing information about properties to visitors.
This information is very useful, when you are trying to get,
for example, a filename of file where the code is stored;
3) We added a bunch of visitors, checkers and fixers that will extended KTlint functionaliity with code style rules;
4) We have proposed a code style for Kotlin language. 

Before you make a pull request, make sure the build is clean as we have lot of tests and other prechecks:

```bash
$ mvn clean install
```

Also see our [Contributing Policy](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md)
