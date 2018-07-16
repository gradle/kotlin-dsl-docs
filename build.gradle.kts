import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL


plugins {
    base
    id("org.jetbrains.dokka")
    id("org.ajoberstar.github-pages")
}

buildscript {
    // dokka requires a repository from which to download dokka-fatjar on demand
    configure(listOf(repositories, project.repositories)) {
        jcenter()
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev") }
    }
}


// Sources sources
// Uses local clones if available

val gradleGitUseLocalClone = file("../gradle/.git").isDirectory
val gradleGitUri =
    if (gradleGitUseLocalClone) file("../gradle").toURI().toString()
    else "https://github.com/gradle/gradle.git"
val gradleGitRef = "kotlin-dsl-docs"

val kotlinDslGitUseLocalClone = file("../kotlin-dsl/.git").isDirectory
val kotlinDslGitUri =
    if (kotlinDslGitUseLocalClone) file("../kotlin-dsl").toURI().toString()
    else "https://github.com/gradle/kotlin-dsl.git"
val kotlinDslGitRef = "v0.14.2"

logger.lifecycle("\nGradle sources for Kotlin DSL API\n  uri = $gradleGitUri\n  ref = $gradleGitRef")
logger.lifecycle("\nKotlin DSL sources for Kotlin DSL API\n  uri = $kotlinDslGitUri\n  ref = $kotlinDslGitRef")


// Groovy and Kotlin versions
// Required to be fetched at configuration time

val groovyVersionSourceFilePath = "gradle/dependencies.gradle"
val groovyVersion =
    if (gradleGitUseLocalClone)
        file("../gradle/$groovyVersionSourceFilePath").readLines().extractGroovyVersion()
    else
        URL("https://raw.githubusercontent.com/gradle/gradle/$gradleGitRef/$groovyVersionSourceFilePath")
            .openStream().bufferedReader().use { it.lineSequence().toList().extractGroovyVersion() }

val kotlinVersionSourceFilePath = "kotlin-version.txt"
val kotlinVersion =
    if (kotlinDslGitUseLocalClone)
        file("../kotlin-dsl/$kotlinVersionSourceFilePath").readLines().extractKotlinVersion()
    else
        URL("https://raw.githubusercontent.com/gradle/kotlin-dsl/$kotlinDslGitRef/$kotlinVersionSourceFilePath")
            .openStream().bufferedReader().use { it.lineSequence().toList().extractKotlinVersion() }

fun List<String>.extractGroovyVersion() =
    find { it.startsWith("libraries.groovy =") }!!
        .split("=").last().substringAfterLast("version: '").substringBeforeLast("'").trim()

fun List<String>.extractKotlinVersion() =
    first().trim()

logger.lifecycle("\nRuntime versions for Kotlin DSL API\n  Groovy $groovyVersion\n  Kotlin $kotlinVersion")


val dokkaDependencies by configurations.creating
dependencies {
    dokkaDependencies("org.codehaus.groovy:groovy-all:$groovyVersion")
    dokkaDependencies("org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion")
    dokkaDependencies("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    dokkaDependencies("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
}


tasks {

    val cloningGroup = "cloning"
    val apiSourcesGroup = "API sources"


    // Gradle API sources extraction
    val cloneGradle by creating(git.GitClone::class) {
        group = cloningGroup
        description = "Clones Gradle sources."
        uri = gradleGitUri
        ref = gradleGitRef
        cloneDir = file("$buildDir/clones/gradle")
    }
    val gradleApiSources by creating(api.GradleApiSources::class) {
        group = apiSourcesGroup
        description = "Generates Gradle API sources."
        gradleClone = cloneGradle.cloneDir
        sourceDir = file("$buildDir/api-sources/gradle")
        dependsOn(cloneGradle)
    }


    // Gradle Kotlin DSL API sources extraction and generation
    val cloneKotlinDsl by tasks.creating(git.GitClone::class) {
        group = cloningGroup
        description = "Clones Gradle Kotlin DSL sources."
        uri = kotlinDslGitUri
        ref = kotlinDslGitRef
        cloneDir = file("$buildDir/clones/kotlin-dsl")
    }
    val generateKotlinDslExtensions by creating(GradleBuild::class) {
        dir = cloneKotlinDsl.cloneDir
        tasks = listOf(":provider:generateExtensions")
        dependsOn(cloneKotlinDsl)
    }
    val gradleKotlinDslApiSources by creating(api.GradleKotlinDslApiSources::class) {
        group = apiSourcesGroup
        description = "Generates Gradle Kotlin DSL API sources."
        kotlinDslClone = cloneKotlinDsl.cloneDir
        sourceDir = file("$buildDir/api-sources/gradle-kotlin-dsl")
        dependsOn(generateKotlinDslExtensions)
    }


    // Gradle built-in plugins accessors API generation
    val installGradle by creating(api.GradleInstall::class) {
        gradleClone = cloneGradle.cloneDir
        gradleInstall = file("$buildDir/install/gradle")
        dependsOn(cloneGradle)
    }
    val buildWithCorePluginsDir = file("build-with-core-plugins")
    val gradlePluginsAccessors by creating(api.GradlePluginsAccessors::class) {
        gradleInstall = installGradle.gradleInstall
        buildDirectory = buildWithCorePluginsDir
        accessorsDir = file("$buildDir/generated/gradle-plugins-accessors")
        dependsOn(installGradle)
    }


    // API docs generation using dokka
    val dokka by getting(DokkaTask::class) {
        dependsOn(gradleApiSources, gradleKotlinDslApiSources, gradlePluginsAccessors)
        group = null
        moduleName = "api"
        outputDirectory = "$buildDir/docs/dokka"
        jdkVersion = 8
        classpath = dokkaDependencies
        sourceDirs = listOf(
            gradleKotlinDslApiSources.sourceDir,
            gradleApiSources.sourceDir,
            gradlePluginsAccessors.accessorsDir)
        includes = listOf("src/dokka/kotlin-dsl.md")
        doFirst {
            file(outputDirectory).deleteRecursively()
        }
    }
    val apiDocumentation by creating {
        group = "documentation"
        description = "Generates Gradle Kotlin DSL API documentation."
        dependsOn(dokka)
    }


    // Checks
    val checkApiDocumentation by creating {
        dependsOn(dokka)
        group = "verification"
        description = "Runs checks on the generated API documentation."
        val apiDocsRoot = file(dokka.outputDirectory)
        inputs.dir(apiDocsRoot)
        doLast {
            var gradleApiFound = false
            var gradleKotlinDslApiFound = false
            var gradleKotlinDslGeneratedApiFound = false
            var gradlePluginsAccessorsFound = false
            val filesWithErrorClass = mutableListOf<File>()
            apiDocsRoot.walk().filter { it.isFile }.forEach { file ->
                val text = file.readText()
                if (text.contains("ERROR CLASS")) {
                    filesWithErrorClass += file
                }
                if (!gradleApiFound && text.contains("id=\"org.gradle.api.Project\$task")) {
                    gradleApiFound = true
                }
                if (!gradleKotlinDslApiFound && text.contains("id=\"org.gradle.kotlin.dsl.KotlinBuildScript")) {
                    gradleKotlinDslApiFound = true
                }
                if (!gradleKotlinDslGeneratedApiFound && text.contains("embeddedKotlinVersion")) {
                    gradleKotlinDslGeneratedApiFound = true
                }
                if (!gradlePluginsAccessorsFound && text.contains("name=\"org.gradle.kotlin.dsl\$checkstyle#org.gradle.api.Project")) {
                    gradlePluginsAccessorsFound = true
                }
            }
            if (!gradleApiFound) {
                throw Exception("API documentation does not include Gradle API")
            }
            if (!gradleKotlinDslApiFound) {
                throw Exception("API documentation does not include Gradle Kotlin DSL")
            }
            if (!gradleKotlinDslGeneratedApiFound) {
                throw Exception("API documentation does not include *generated* Gradle Kotlin DSL")
            }
            if (!gradlePluginsAccessorsFound) {
                throw Exception("API documentation does not include *generated* Gradle Plugins accessors")
            }
            if (filesWithErrorClass.isNotEmpty()) {
                throw  Exception("<ERROR CLASS> found in ${filesWithErrorClass.size} files:\n  ${filesWithErrorClass.joinToString("\n  ")}")
            }
        }
    }


    // Lifecycle
    val cleanBuildWithCorePlugins by creating(GradleBuild::class) {
        dir = buildWithCorePluginsDir
        tasks = listOf("clean")
    }
    "clean" {
        dependsOn(cleanBuildWithCorePlugins)
    }
    "assemble" {
        dependsOn(apiDocumentation)
    }
    "check" {
        dependsOn(checkApiDocumentation)
    }


    // Publishing
    githubPages {
        pages.apply {
            from("$buildDir/docs/dokka")
            from("$buildDir/docs/gradle")
            from("src/pages")
        }
        commitMessage = "Updating gh-pages"
    }
    "prepareGhPages" {
        description = "Stages documentation locally."
        dependsOn("assemble")
    }
    "publishGhPages" {
        description = "Publishes documentation to production."
        dependsOn("check")
    }


    // Global tasks configuration
    withType<GradleBuild> {
        startParameter.showStacktrace = ShowStacktrace.ALWAYS
    }
}
