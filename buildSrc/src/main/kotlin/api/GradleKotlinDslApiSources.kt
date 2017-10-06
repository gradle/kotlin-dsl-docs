package api

import java.io.File

import org.gradle.api.*
import org.gradle.api.tasks.*

open class GradleKotlinDslApiSources : DefaultTask() {

    @get:InputDirectory
    var gskClone: File? = null

    @get:OutputDirectory
    var sourceDir: File? = null

    @TaskAction
    fun copyGradleScriptKotlinApiSources() {
        project.sync {
            from(File(gskClone!!, "provider/src/main/kotlin"))
            from(File(gskClone!!, "provider/src/generated/kotlin"))
            from(File(gskClone!!, "plugins/src/main/kotlin"))
            from(File(gskClone!!, "tooling-models/src/main/kotlin"))
            exclude("org/gradle/kotlin/dsl/accessors/**")
            exclude("org/gradle/kotlin/dsl/provider/**")
            exclude("org/gradle/kotlin/dsl/resolver/**")
            exclude("org/gradle/kotlin/dsl/support/**")
            exclude("org/gradle/script/lang/kotlin/**")
            into(sourceDir!!)
        }
    }
}
