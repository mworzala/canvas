package com.mattworzala.canvas


// Heavily inspired by Jetpack Compose's @Composable
@Retention(AnnotationRetention.BINARY)
@Target(
    // function declarations
    // @Fragment fun Foo() { ... }
    // lambda expressions
    // val foo = @Fragment { ... }
    AnnotationTarget.FUNCTION,

    // type declarations
    // var foo: @Fragment () -> Unit = { ... }
    // parameter types
    // foo: @Fragment () -> Unit
    AnnotationTarget.TYPE,

    // fragment types inside of type signatures
    // foo: (@Fragment () -> Unit) -> Unit
    AnnotationTarget.TYPE_PARAMETER,

    // fragment property getters and setters
    // val foo: Int @Fragment get() { ... }
    // var bar: Int
    //   @Fragment get() { ... }
    AnnotationTarget.PROPERTY_GETTER
)
annotation class Fragment

//todo
annotation class DisallowFragmentCalls
annotation class ReadOnlyFragment
annotation class ExplicitGroupsFragment
annotation class NonRestartableFragment

class FragmentContext {
    fun sayHello() {
        println("I AM FRAGMENT CONTEXT!!!!")
    }
}