import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.mattworzala.canvas")
}

tasks.withType(KotlinCompile::class.java).configureEach {
    kotlinOptions {
        if (JavaVersion.current().isJava9Compatible) {
            jvmTarget = JavaVersion.current().toString()
        }
    }
}


tasks.findByName("compileKotlin")?.dependsOn(":kotlin-plugin:publishToMavenLocal")

dependencies {
    implementation(rootProject)

    implementation("com.github.Minestom:Minestom:5d7a49c009")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.5.0")
}
