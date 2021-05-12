import com.mattworzala.canvas.compiler.CanvasPlugin
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar

fun compile(
    sourceFiles: List<SourceFile>,
    plugin: ComponentRegistrar = CanvasPlugin(),
): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        sources = sourceFiles
        useIR = true
        compilerPlugins = listOf(plugin)
        inheritClassPath = true
    }.compile()
}

fun compile(
    sourceFile: SourceFile,
    plugin: ComponentRegistrar = CanvasPlugin(),
): KotlinCompilation.Result {
    return compile(listOf(sourceFile), plugin)
}