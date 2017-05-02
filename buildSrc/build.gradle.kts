buildscript {
    repositories {
        gradleScriptKotlin()
    }
    dependencies {
        classpath(kotlinModule("gradle-plugin"))
    }
}

apply {
    plugin("kotlin")
}

repositories {
    gradleScriptKotlin()
    jcenter()
}

dependencies {
    compile(gradleScriptKotlinApi())
    compile(kotlinModule("stdlib"))
    compile("org.ajoberstar:gradle-git:1.7.1")
    compile("org.jetbrains.dokka:dokka-gradle-plugin:0.9.13")
}
