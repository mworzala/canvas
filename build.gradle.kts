plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.0"
    id("java")
    id("java-library")
    id("maven-publish")
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()

        maven(url = "https://jitpack.io")
        maven(url = "https://jcenter.bintray.com/")
        maven(url = "https://repo.spongepowered.org/maven")
        maven(url = "https://libraries.minecraft.net")
        maven(url = "https://repo.velocitypowered.com/snapshots/")
    }
}

dependencies {
    // Kotlin
//    compileOnly(platform("org.jetbrains.kotlin:kotlin-bom"))
//    compileOnly(kotlin("stdlib"))
//    compileOnly(kotlin("reflect"))



    // Minestom
    compileOnly("com.github.Minestom:Minestom:5d7a49c009")

//    testImplementation(kotlin("reflect"))
//    testImplementation("com.github.Minestom:Minestom:5d7a49c009")
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
