/*
 * Copyright 2022 Bloomberg Finance L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import java.net.URL
import java.time.Duration
import java.util.Locale
import kotlinx.kover.api.VerificationValueType
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.jlleitschuh.gradle.ktlint.tasks.GenerateReportsTask

repositories {
    mavenCentral()
}

plugins {
    jacoco
    id("io.gitlab.arturbosch.detekt") version Versions.DETEKT.version
    id("io.github.gradle-nexus.publish-plugin") version Versions.NEXUS_PLUGIN.version
    id("org.jetbrains.dokka") version Versions.DOKKA.version
    id("org.jetbrains.kotlinx.kover") version Versions.KOTLINX_KOVER.version
    id("org.jetbrains.qodana") version Versions.QODANA_PLUGIN.version
    id("org.jlleitschuh.gradle.ktlint") version Versions.KTLINT_GRADLE_PLUGIN.version
}

group = selektGroupId
version = selektVersionName
logger.quiet("Group: $group; Version: $version")

nexusPublishing {
    repositories {
        sonatype()
    }
    transitionCheckOptions {
        maxRetries.set(180)
        delayBetween.set(Duration.ofSeconds(10L))
    }
}

subprojects {
    apply {
        plugin("selekt")
    }
}

subprojects {
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            mapOf(
                "android" to "AndroidLib",
                "android-sqlcipher" to "AndroidSQLCipher",
                "annotations" to "Annotations",
                "api" to "ApiLib",
                "java" to "Lib",
                "sqlite3" to "SQLite3"
            ).forEach {
                substitute(module(selekt(it.key))).apply {
                    using(project(":${it.value}"))
                    because("we work with an unreleased version")
                }
            }
        }
    }
}

dependencies {
    ktlint("com.pinterest:ktlint:${Versions.KTLINT}")
}

subprojects {
    apply {
        plugin("io.gitlab.arturbosch.detekt")
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
            jvmTarget = "11"
        }
    }

    configure<DetektExtension> {
        toolVersion = Versions.DETEKT.version
        source = files("src")
        config = files("${rootProject.projectDir}/config/detekt/config.yml")
        buildUponDefaultConfig = true
        parallel = false
        debug = false
        ignoreFailures = false
    }
    tasks.withType<Detekt>().configureEach {
        exclude("**/res/**")
        exclude("**/tmp/**")
        reports.html.outputLocation.fileValue(File("$rootDir/build/reports/detekt/${project.name}-detekt.html"))
    }

    plugins.withType<JacocoPlugin> {
        configure<JacocoPluginExtension> {
            toolVersion = Versions.JACOCO.version
        }
    }

    plugins.withType<SigningPlugin> {
        configure<SigningExtension> {
            val signingKeyId: String? by project
            val signingKey: String? by project
            val signingPassword: String? by project
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
            configure<PublishingExtension> {
                publications {
                    sign(this)
                }
            }
        }
    }

    tasks.withType<DokkaTask>().configureEach {
        moduleName.set("Selekt")
        dokkaSourceSets.named("main") {
            sourceLink {
                remoteUrl.set(URL("https://github.com/bloomberg/selekt/tree/master/" +
                    "${this@configureEach.project.name}/src/main/kotlin"))
                localDirectory.set(file("src/main/kotlin"))
            }
            includeNonPublic.set(false)
            jdkVersion.set(JavaVersion.VERSION_11.majorVersion.toInt())
            noAndroidSdkLink.set(false)
            noJdkLink.set(false)
            noStdlibLink.set(false)
        }
    }
}

allprojects {
    apply {
        plugin("org.jlleitschuh.gradle.ktlint")
    }
    configure<KtlintExtension> {
        version.set(Versions.KTLINT.version)
        disabledRules.set(setOf("import-ordering", "indent", "wrapping"))
        reporters {
            reporter(ReporterType.HTML)
        }
    }
    tasks.withType<GenerateReportsTask>().configureEach {
        reportsOutputDirectory.set(rootProject.layout.buildDirectory.dir("reports/ktlint/${project.name}/$name"))
    }
}

fun JacocoReportBase.initialise() {
    group = "verification"
    val block: (JacocoReport) -> Unit = {
        this@initialise.classDirectories.from(it.classDirectories)
        this@initialise.executionData.from(it.executionData)
        this@initialise.sourceDirectories.from(it.sourceDirectories)
    }
    subprojects {
        plugins.withType<JacocoAndroidPlugin> {
            plugins.withId("com.android.library") {
                val capitalisedVariant = this@subprojects.extensions.getByType(
                    JacocoAndroidUnitTestReportExtension::class.java).preferredVariant.capitalize(Locale.ROOT)
                tasks.withType<JacocoReport>().all {
                    if (name.contains(capitalisedVariant)) {
                        block(this)
                        this@initialise.dependsOn(this)
                    }
                }
            }
        }
        plugins.withId("jacoco") {
            plugins.withId("org.jetbrains.kotlin.jvm") {
                tasks.withType<JacocoReport>().all {
                    block(this)
                    this@initialise.dependsOn(this)
                }
            }
        }
    }
}

tasks.register<JacocoReport>("jacocoSelektTestReport") {
    initialise()
    description = "Generates a global JaCoCo coverage report."
    reports {
        csv.required.set(false)
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoSelektCoverageVerification") {
    initialise()
    description = "Verifies JaCoCo coverage bounds globally."
    violationRules {
        rule {
            isEnabled = true
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.9761".toBigDecimal() // Does not include inlined blocks. Jacoco can't yet cover these.
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.9326".toBigDecimal() // Does not include inlined blocks. Jacoco can't yet cover these.
            }
        }
    }
    mustRunAfter("jacocoSelektTestReport")
}

kover {
    disabledProjects = setOf("AndroidCli", "AndroidLibBenchmark", "AndroidLint")
}

tasks.koverMergedVerify {
    rule {
        name = "Minimal line coverage"
        bound {
            minValue = 94
            valueType = VerificationValueType.COVERED_LINES_PERCENTAGE
        }
    }
}

qodana {
    saveReport.set(true)
}
