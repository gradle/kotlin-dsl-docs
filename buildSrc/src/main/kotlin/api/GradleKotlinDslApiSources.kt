package api

import java.io.File

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.script.lang.kotlin.*

open class GradleKotlinDslApiSources : DefaultTask() {

    @get:InputDirectory
    var gskClone: File? = null

    @get:OutputDirectory
    var sourceDir: File? = null

    @TaskAction
    fun copyGradleScriptKotlinApiSources(): Unit {
        project.sync {
            from(File(gskClone!!, "provider/src/main/kotlin"))
            from(File(gskClone!!, "provider/src/generated/kotlin"))
            from(File(gskClone!!, "tooling-models/src/main/kotlin"))
            exclude("org/gradle/script/lang/kotlin/accessors/**")
            exclude("org/gradle/script/lang/kotlin/provider/**")
            exclude("org/gradle/script/lang/kotlin/resolver/**")
            exclude("org/gradle/script/lang/kotlin/support/**")
            into(sourceDir!!)
        }
    }
}
