/*
 * Copyright 2026 Bloomberg Finance L.P.
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

description = "Custom Detekt rules that enforce Selekt's SQL trust boundary."

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.detekt.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.detekt.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

configurations.named("testRuntimeClasspath") {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" &&
            requested.name == "kotlin-compiler-embeddable"
        ) {
            useVersion("1.7.22")
            because("Detekt 1.22.0 test harness targets kotlin-compiler-embeddable 1.7.x")
        }
    }
}
configurations.named("testCompileClasspath") {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" &&
            requested.name == "kotlin-compiler-embeddable"
        ) {
            useVersion("1.7.22")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
