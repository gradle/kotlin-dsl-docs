package api

import java.io.ByteArrayOutputStream
import java.io.File

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.script.lang.kotlin.*

open class GradlePluginsAccessors : DefaultTask() {

    @get:Internal
    var buildDirectory: File? = null

    @get:InputFile
    val buildScript: File
        get() = File(buildDirectory!!, "build.gradle.kts")

    private
    val accessorsDirState = project.property(File::class.java)

    @get:OutputDirectory
    var accessorsDir
        get() = accessorsDirState.get()
        set(value) = accessorsDirState.set(value)

    @get:Internal
    val accessorsDirProvider: Provider<File>
        get() = accessorsDirState

    @TaskAction
    fun gradlePluginsAccessors(): Unit {
        accessorsDir.deleteRecursively()
        val baos = ByteArrayOutputStream()
        project.exec {
            commandLine = listOf(
                "./gradlew", "-q",
                "-c", File(buildDirectory!!, "settings.gradle").absolutePath,
                "-p", buildDirectory!!.absolutePath,
                "kotlinDslAccessorsReport")
            standardOutput = baos
        }
        var text = """
        package org.gradle.kotlin.dsl

        import org.gradle.api.*
        import org.gradle.api.artifacts.*
        import org.gradle.api.artifacts.dsl.*

        ${String(baos.toByteArray())}
        """
        val accessorsFile = File(accessorsDir, "GradlePluginsAccessors.kt")
        accessorsFile.parentFile.mkdirs()
        accessorsFile.writeText(text)
    }

}
