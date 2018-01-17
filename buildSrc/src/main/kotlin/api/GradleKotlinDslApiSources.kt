package api

import java.io.File

import org.gradle.api.*
import org.gradle.api.tasks.*

open class GradleKotlinDslApiSources : DefaultTask() {

    @get:InputDirectory
    lateinit var kotlinDslClone: File

    @get:OutputDirectory
    lateinit var sourceDir: File

    @TaskAction
    fun copyGradleScriptKotlinApiSources() {
        project.sync {
            from(File(kotlinDslClone, "provider/src/main/kotlin"))
            from(File(kotlinDslClone, "provider/src/generated/kotlin"))
            from(File(kotlinDslClone, "plugins/src/main/kotlin"))
            from(File(kotlinDslClone, "tooling-models/src/main/kotlin"))
            exclude("org/gradle/kotlin/dsl/accessors/**")
            exclude("org/gradle/kotlin/dsl/provider/**")
            exclude("org/gradle/kotlin/dsl/resolver/**")
            exclude("org/gradle/kotlin/dsl/support/**")
            exclude("org/gradle/script/lang/kotlin/**")
            into(sourceDir)
        }
    }
}
