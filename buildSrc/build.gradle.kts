plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

dependencies {
    compile("org.ajoberstar:gradle-git:1.7.1")
    compile("org.jetbrains.dokka:dokka-gradle-plugin:0.9.15")
}
