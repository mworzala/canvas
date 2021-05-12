package com.mattworzala.canvas.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

class CanvasGradleSubplugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        // todo could add extensions if helpful
    }

    override fun getCompilerPluginId(): String = "canvas-compiler-plugin"

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "com.mattworzala.canvas",
            artifactId = "canvas-compiler-plugin",
            version = "1.0"
        )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    // See for passing data to the compiler plugin
    // https://github.com/ZacSweers/redacted-compiler-plugin/blob/main/redacted-compiler-plugin-gradle/src/main/kotlin/dev/zacsweers/redacted/gradle/RedactedGradleSubplugin.kt
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        //todo add canvas dependency automatically
        //project.dependencies.add("implementation",
        //          "dev.zacsweers.redacted:redacted-compiler-plugin-annotations:$VERSION")

        return project.provider { listOf() }
    }
}