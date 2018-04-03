package sparklin.kshell.repl

object Sandbox {
    @JvmStatic
    fun main(args: Array<String>) {
        val repl = NewRepl()
//        (1..20).map {
//            println("i=" + it)
//            repl.eval(it, "fun f$it(x: Int) = x + $it")
//        }

//        repl.eval(101, "println(f100(1000))")

//
//        repl.eval(1, "val x = 10\nfun <R> g(x: R, y: Int) = x\nprintln(11111)\nx")
        repl.eval(1, "val x = 10\nprintln(10)\nx + 1")
        repl.eval(2, "class A() { val x = 10 }")
        repl.eval(3, "println(res1)\nval a = A()")


//        repl.eval(1, "val x = 10\n val y = 20\nfun f(x: Int) = x + 1\nfun f(x: Int, y: Int) = x + y\nprintln(y)")
//        repl.state.snippets.forEach {
//            if (it is NamedSnippet) {
//                println("***** " + it.toImportStatement())
//            }
//        }
//        repl.eval(2, "fun <R> f(x: R, y: Int) = x\ny")
//        repl.eval(3, "println(f(1.0, 100))")

//        repl.eval(1, "fun f(x: Int) = x + 1\nfun f(x: Int, y: Int) = x + y")
//        repl.eval(2, "fun f(x: Int) = x + 12\nprintln(f(10))")
//        repl.eval(3, "println(f(100))")

//        repl.eval(3, "f(10)")
    }

}