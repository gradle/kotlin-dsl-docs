package api

import java.io.ByteArrayOutputStream
import java.io.File

import org.gradle.api.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*

import java.util.jar.JarFile


open class GradlePluginsAccessors : DefaultTask() {

    @get:InputDirectory
    lateinit var gradleInstall: File

    @get:Internal
    lateinit var buildDirectory: File

    @get:InputFile
    val buildScript: File
        get() = buildDirectory.resolve("build.gradle.kts")

    private
    val accessorsDirState = project.objects.property(File::class.java)

    @get:OutputDirectory
    var accessorsDir
        get() = accessorsDirState.get()
        set(value) = accessorsDirState.set(value)

    @get:Internal
    val accessorsDirProvider: Provider<File>
        get() = accessorsDirState

    @TaskAction
    fun gradlePluginsAccessors() {
        accessorsDir.deleteRecursively()
        val gradleUserHomeDir = temporaryDir.resolve("guhd").also { it.deleteRecursively() }
        val baos = ByteArrayOutputStream()
        val gradlew = gradleInstall.resolve("bin/gradle")
        project.exec {
            commandLine = listOf(
                    gradlew.absolutePath, "-q", "-s",
                    "-g", gradleUserHomeDir.absolutePath,
                    "-c", buildDirectory.resolve("settings.gradle.kts").absolutePath,
                    "-p", buildDirectory.absolutePath,
                    "kotlinDslAccessorsReport")
            standardOutput = baos
        }
        val output = baos.toString()
        extractGradleCorePluginsAccessors(output)
        extractGradleApiKotlinDslExtensions(gradleUserHomeDir)
    }

    fun extractGradleCorePluginsAccessors(output: String) {
        val corePluginsAccessorsSource = """
            package org.gradle.kotlin.dsl

            import org.gradle.api.*
            import org.gradle.api.artifacts.*
            import org.gradle.api.artifacts.dsl.*

            $output
            """
        val accessorsFile = accessorsDir.resolve("GradleCorePluginsAccessors.kt")
        accessorsFile.parentFile.mkdirs()
        accessorsFile.writeText(corePluginsAccessorsSource)
    }

    fun extractGradleApiKotlinDslExtensions(gradleUserHomeDir: File) {

        val generatedJar = gradleUserHomeDir.resolve("caches")
                .listFiles { f -> f.isDirectory && f.name[0].isDigit() }
                .single().resolve("generated-gradle-jars")
                .listFiles { f -> f.isFile && f.name.startsWith("gradle-kotlin-dsl-extensions-") }
                .single()

        JarFile(generatedJar).use { jar ->
            jar.entries().asSequence()
                    .filter { !it.isDirectory && it.name.endsWith(".kt") }
                    .forEach { sourceEntry ->
                        val sourceFile = accessorsDir.resolve(sourceEntry.name)
                        sourceFile.parentFile.mkdirs()
                        jar.getInputStream(sourceEntry).use { input -> sourceFile.outputStream().use { output -> input.copyTo(output) } }
                    }
        }
    }
}
