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

    maven(url = "http://repo.spongepowered.org/maven")
    maven(url = "https://libraries.minecraft.net")
}

dependencies {
    // Kotlin
    compileOnly(platform("org.jetbrains.kotlin:kotlin-bom"))
    compileOnly(kotlin("stdlib"))
    compileOnly(kotlin("reflect"))

    // Minestom
    compileOnly("com.github.Minestom:Minestom:fc694f4b49")

    testImplementation(kotlin("reflect"))
    testImplementation("com.github.Minestom:Minestom:fc694f4b49")
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
