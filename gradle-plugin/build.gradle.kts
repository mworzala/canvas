

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.0"
    kotlin("kapt") version "1.5.0"

    id("java-gradle-plugin")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.5.0")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.0")

    compileOnly("com.google.auto.service:auto-service:1.0")
    kapt("com.google.auto.service:auto-service:1.0")
}

gradlePlugin {
    plugins {
        create("canvasPlugin") {
            id = "com.mattworzala.canvas"
            implementationClass = "com.mattworzala.canvas.gradle.CanvasGradleSubplugin"
        }
    }
}
