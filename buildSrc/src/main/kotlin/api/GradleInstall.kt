package api

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class GradleInstall : DefaultTask() {

    @get:InputDirectory
    lateinit var gradleClone: File

    @get:OutputDirectory
    lateinit var gradleInstall: File


    @TaskAction
    fun gradleInstall() {
        val buildDirectory = File(temporaryDir, "gradle")
        project.delete(buildDirectory)
        project.copy {
            from(gradleClone)
            into(buildDirectory)
        }
        project.delete(gradleInstall)
        val gradlew = File(buildDirectory, "gradlew")
        project.exec {
            commandLine = listOf(
                    gradlew.absolutePath, "-q", "--no-scan",
                    "-c", File(buildDirectory, "settings.gradle.kts").absolutePath,
                    "-p", buildDirectory.absolutePath,
                    "install", "-Pgradle_installPath=${gradleInstall.absolutePath}")
        }
    }
}
