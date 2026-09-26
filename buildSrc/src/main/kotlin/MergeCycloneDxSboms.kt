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

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.UUID
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "The generated CycloneDX serial number is intentionally unique")
abstract class MergeCycloneDxSboms : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sbomFiles: ConfigurableFileCollection

    @get:Input
    abstract val repositoryVersion: Property<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun merge() {
        val parser = JsonSlurper()
        val documents = sbomFiles.files.sortedBy { it.absolutePath }.map {
            parser.parse(it) as Map<*, *>
        }
        val merged = documents.first().toMutableMap()
        merged["serialNumber"] = "urn:uuid:${UUID.randomUUID()}"
        merged["metadata"] = (merged["metadata"] as Map<*, *>).toMutableMap().apply {
            put(
                "component",
                mapOf(
                    "type" to "application",
                    "bom-ref" to "com.bloomberg.selekt:selekt",
                    "group" to "com.bloomberg.selekt",
                    "name" to "selekt",
                    "version" to repositoryVersion.get()
                )
            )
        }

        val components = linkedMapOf<String, Map<*, *>>()
        val dependencies = linkedMapOf<String, LinkedHashSet<String>>()
        documents.forEach { document ->
            (document["components"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.forEach { component ->
                components[component["bom-ref"].toString()] = component
            }
            (document["dependencies"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.forEach { dependency ->
                val ref = dependency["ref"].toString()
                val children = dependencies.getOrPut(ref) { linkedSetOf() }
                (dependency["dependsOn"] as? List<*>)?.forEach { children += it.toString() }
            }
        }
        merged["components"] = components.values.sortedBy { it["bom-ref"].toString() }
        merged["dependencies"] = dependencies.entries.sortedBy { it.key }.map { (ref, children) ->
            mapOf("ref" to ref, "dependsOn" to children.sorted())
        }

        val target = output.get().asFile
        target.parentFile.mkdirs()
        target.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(merged)) + System.lineSeparator())
    }
}
