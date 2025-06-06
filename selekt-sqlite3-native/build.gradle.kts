/*
 * Copyright 2025 Bloomberg Finance L.P.
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

import java.util.Locale

description = "Selekt native library."

plugins {
    `java-library`
    `maven-publish`
    signing
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("intermediates/libs"))
}

fun osName() = System.getProperty("os.name").lowercase(Locale.US).run {
    when {
        startsWith("mac") -> "darwin"
        startsWith("windows") -> "windows"
        else -> replace("\\s+", "_")
    }
}

fun platformIdentifier() = "${osName()}-${System.getProperty("os.arch")}"

tasks.named<Jar>("jar") {
    archiveClassifier.set(platformIdentifier())
}

tasks.withType<ProcessResources>().configureEach {
    dependsOn("buildNativeHost", "copyJniLibs")
}

tasks.register<Task>("buildNativeHost") {
    dependsOn(":SQLite3:buildHost")
    finalizedBy("copyJniLibs")
}

tasks.register<Copy>("copyJniLibs") {
    from(fileTree(project(":SQLite3").layout.buildDirectory.dir("intermediates/libs")))
    into(layout.buildDirectory.dir("intermediates/libs/jni"))
    mustRunAfter("buildNativeHost")
}

publishing {
    publications.register<MavenPublication>("native") {
        listOf(
            "linux-amd64"
        ).forEach {
            artifact(file("build/libs/${project.name}-$version-$it.jar")) {
                classifier = it
            }
        }
        pom {
            commonInitialisation(project)
            description.set("Selekt native library.")
        }
    }
}
