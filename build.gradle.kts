plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.21"
    id("java")
    id("java-library")
    id("maven")
}

repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven(url = "https://jcenter.bintray.com/")

    maven(url = "https://repo.spongepowered.org/maven")
    maven(url = "https://libraries.minecraft.net")
}

dependencies {
    // Kotlin
    compileOnly(platform("org.jetbrains.kotlin:kotlin-bom"))
    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("reflect"))

    // Minestom
    compileOnly("com.github.Minestom:Minestom:9a8b6e2a11")

    testImplementation(kotlin("reflect"))
    testImplementation("com.github.Minestom:Minestom:9a8b6e2a11")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> { kotlinOptions.jvmTarget = "11" }
val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks

compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}
