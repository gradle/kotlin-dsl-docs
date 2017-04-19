import org.eclipse.jgit.api.Git
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

plugins {
    id("org.ajoberstar.github-pages") version "1.7.1"
}

buildscript {
    repositories { jcenter() }
    dependencies { classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.13") }
}

val cloneGradle = task<GitClone>("cloneGradle") {
    uri = "https://github.com/gradle/gradle.git"
    ref = "gradle-script-kotlin"
    outputDirectory = files("$buildDir/clones/gradle")
}

val cloneGSK = task<GitClone>("cloneGSK") {
    uri = "https://github.com/gradle/gradle-script-kotlin.git"
    ref = "master"
    outputDirectory = files("$buildDir/clones/gradle-script-kotlin")
}

val copyGradleApiSources = task<CopyGradleApiSources>("copyGradleApiSources") {
    dependsOn(cloneGradle)
    gradleClone = cloneGradle.outputDirectory
    outputDirectory = files("$buildDir/gradle-api-sources")
}

apply {
    // This is applied imperatively because using the plugins block fails
    plugin("org.jetbrains.dokka")
    // This is in a Groovy file because the Dokka task type is not reachable
    from("gradle/dokka.gradle")
    // This is in a Groovy file because githubPages extensions is not usable from Kotlin
    from("gradle/githubPages.gradle")
}

val dokka by tasks
dokka.dependsOn(copyGradleApiSources, cloneGSK)

val publishGhPages by tasks
publishGhPages.dependsOn(dokka)

open class GitClone : DefaultTask()
{
    @Input
    var uri: String? = null

    @Input
    var ref: String? = null

    @OutputDirectories
    var outputDirectory: FileCollection? = null

    @TaskAction
    fun gitClone(): Unit
    {
        outputDirectory!!.singleFile.deleteRecursively()
        var clone: Git? = null
        try
        {
            clone = Git.cloneRepository()
                .setURI(uri!!)
                .setDirectory(outputDirectory!!.singleFile)
                .setBranchesToClone(listOf(ref!!))
                .setBranch(ref!!)
                .call()
        }
        finally
        {
            clone?.close()
        }
    }
}

// TODO Gradle API definition extracted from gradle/gradle/build.gradle and gradle/gradle/subprojects/docs/docs.gradle
open class CopyGradleApiSources : DefaultTask()
{
    @InputFiles
    var gradleClone: FileCollection? = null

    @OutputDirectories
    var outputDirectory: FileCollection? = null

    @TaskAction
    fun copyGradleApiSources(): Unit
    {
        File(gradleClone!!.singleFile, "subprojects").listFiles { file: File ->
            file.isDirectory() && !file.name.startsWith("internal") &&
            file.name !in listOf("integTest", "distributions", "performance", "buildScanPerformance")
        }.forEach { subprojectDir: File ->
            project.copy {
                from(File(subprojectDir, "src/main/java")) {
                    include(
                        "org/gradle/*",
                        "org/gradle/api/**",
                        "org/gradle/authentication/**",
                        "org/gradle/buildinit/**",
                        "org/gradle/caching/**",
                        "org/gradle/external/javadoc/**",
                        "org/gradle/ide/**",
                        "org/gradle/ivy/**",
                        "org/gradle/jvm/**",
                        "org/gradle/language/**",
                        "org/gradle/maven/**",
                        "org/gradle/nativeplatform/**",
                        "org/gradle/platform/**",
                        "org/gradle/play/**",
                        "org/gradle/plugin/devel/**",
                        "org/gradle/plugin/repository/*",
                        "org/gradle/plugin/use/*",
                        "org/gradle/plugin/management/*",
                        "org/gradle/plugins/**",
                        "org/gradle/process/**",
                        "org/gradle/testfixtures/**",
                        "org/gradle/testing/jacoco/**",
                        "org/gradle/tooling/**",
                        "org/gradle/model/**",
                        "org/gradle/testkit/**",
                        "org/gradle/testing/**")
                    exclude("**/internal/**")
                }
                into(outputDirectory!!.singleFile)
            }
        }
    }
}