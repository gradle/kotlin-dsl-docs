package api

import java.io.File

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.script.lang.kotlin.*

// TODO Gradle API definition extracted from gradle/gradle/build.gradle
// and gradle/gradle/publicApi.gradle
open class GradleApiSources : DefaultTask() {

    @get:InputDirectory
    var gradleClone: File? = null

    @get:OutputDirectory
    var sourceDir: File? = null

    @TaskAction
    fun copyGradleApiSources(): Unit {
        File(gradleClone!!, "subprojects").listFiles { file: File ->
            file.isDirectory() && !file.name.startsWith("internal") &&
            file.name !in listOf("integTest", "distributions", "performance", "buildScanPerformance")
        }.forEach { subprojectDir: File ->
            project.copy {
                from(File(subprojectDir, "src/main/java"))
                from(File(subprojectDir, "src/main/groovy"))
                include(
                    "org/gradle/*",
                    "org/gradle/api/**",
                    "org/gradle/authentication/**",
                    "org/gradle/buildinit/**",
                    "org/gradle/caching/**",
                    "org/gradle/concurrent/**",
                    "org/gradle/deployment/**",
                    "org/gradle/external/javadoc/**",
                    "org/gradle/ide/**",
                    "org/gradle/includedbuild/**",
                    "org/gradle/ivy/**",
                    "org/gradle/jvm/**",
                    "org/gradle/language/**",
                    "org/gradle/maven/**",
                    "org/gradle/nativeplatform/**",
                    "org/gradle/normalization/**",
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
                    "org/gradle/testing/**",
                    "org/gradle/vcs/**",
                    "org/gradle/workers/**")
                exclude("**/internal/**")
                into(sourceDir!!)
            }
        }
    }
}
