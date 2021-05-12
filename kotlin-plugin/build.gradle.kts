plugins {
    id("org.jetbrains.kotlin.jvm")
    kotlin("kapt")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.5.0")

    compileOnly("com.google.auto.service:auto-service:1.0")
    kapt("com.google.auto.service:auto-service:1.0")

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.5.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.0")

    testImplementation(project(":"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.mattworzala.canvas"
            artifactId = "canvas-compiler-plugin"
            version = "1.0"

            from(components["kotlin"])
        }
    }
}
