import org.gradle.api.tasks.GradleBuild

plugins { base }

buildscript {
    // dokka requires a repository from which to download dokka-fatjar on demand
    configure(listOf(repositories, project.repositories)) {
        gradleScriptKotlin()
    }
}

apply {
    plugin("org.jetbrains.dokka")
    plugin("org.ajoberstar.github-pages")
}

// Sources sources
// You can change this to local clones URIs and refs for faster turnaround
val gradleGitUri = "https://github.com/gradle/gradle.git"
val gradleGitRef = "gradle-script-kotlin"
val gskGitUri = "https://github.com/gradle/gradle-script-kotlin.git"
val gskGitRef = "master"


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

    // Gradle Script Kotlin API sources extraction and generation
    val cloneGSK by tasks.creating(git.GitClone::class) {
        group = cloningGroup
        description = "Clones Gradle Script Kotlin sources."
        uri = gskGitUri
        ref = gskGitRef
        cloneDir = file("$buildDir/clones/gradle-script-kotlin")
    }
    val generateGskExtensions by creating(GradleBuild::class) {
        dir = cloneGSK.cloneDir
        tasks = listOf(":provider:generateExtensions")
        dependsOn(cloneGSK)
    }
    val gradleScriptKotlinApiSources by creating(api.GradleScriptKotlinApiSources::class) {
        group = apiSourcesGroup
        description = "Generates Gradle Script Kotlin API sources."
        gskClone = cloneGSK.cloneDir
        sourceDir = file("$buildDir/api-sources/gradle-script-kotlin")
        dependsOn(generateGskExtensions)
    }

    // Gradle built-in plugins accessors API generation
    val buildWithCorePluginsDir = file("build-with-core-plugins")
    val gradlePluginsAccessors by creating(api.GradlePluginsAccessors::class) {
        buildDirectory = buildWithCorePluginsDir
        accessorsDir = file("$buildDir/generated/gradle-plugins-accessors")
    }

    // API docs generation using dokka
    val declareDokkaDependencies by creating {
        dependsOn(cloneGradle, cloneGSK)
        doLast {
            val groovyVersion = File(cloneGradle.cloneDir, "gradle/dependencies.gradle")
                .readLines().find { it.startsWith("versions.groovy =") }!!
                .split("=").last().replace("\"", "").trim()
            val kotlinVersion = File(cloneGSK.cloneDir, "kotlin-version.txt")
                .readText().trim()
            val dokkaDependencies by configurations.creating
            dependencies {
                dokkaDependencies("org.codehaus.groovy:groovy-all:$groovyVersion")
                dokkaDependencies("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
                dokkaDependencies("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
                dokkaDependencies("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
            }
        }
    }
    project.apply {
        // This is in a Groovy file because the Dokka task type is not reachable
        from("gradle/dokka.gradle")
    }
    val dokka by this
    dokka.dependsOn(
        declareDokkaDependencies,
        gradleApiSources,
        gradleScriptKotlinApiSources,
        gradlePluginsAccessors)
    val apiDocumentation by creating {
        group = "documentation"
        description = "Generates Gradle Script Kotlin API documentation."
        dependsOn(dokka)
    }

    // User Guide
    val gradleKtsUserGuideHtml by creating(GradleBuild::class) {
        dir = cloneGradle.cloneDir
        tasks = listOf(":docs:ktsUserGuideHtml")
        dependsOn(cloneGradle)
    }

    val userGuideHtml by creating(Copy::class) {
        group = "User Guide"
        from(File(cloneGradle.cloneDir, "subprojects/docs/build/docs/ktsUserGuide"))
        into("$buildDir/docs/gradle/userguide")
        dependsOn(gradleKtsUserGuideHtml)
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
        dependsOn(apiDocumentation, userGuideHtml)
    }
    "check" {
        dependsOn("checkApiDocumentation")
    }

    // Publishing
    project.apply {
        // This is in a Groovy file because githubPages extensions is not usable from Kotlin
        from("gradle/githubPages.gradle")
    }
    "prepareGhPages" {
        description = "Stages documentation locally."
        dependsOn("assemble")
    }
    "publishGhPages" {
        description = "Publishes documentation to production."
        dependsOn("check")
    }
}
