package kshell.completion

import kshell.Command
import kshell.KShell

///**
// * Just for testing completion.
// */
//class TestComplCmd : Command("compl", "c", "Completion test", params = "<code>") {
//    lateinit var kotlinCompletion: KotlinCompletion
//
//    override fun init(repl: KShell) {
//        super.init(repl)
//        kotlinCompletion = KotlinCompletion(repl)
//    }
//
//    override fun execute(line: String) {
//        repl.apply {
//            val str = line.substring(3)
////            kotlinCompletion.generateCompletionProposals("\"hello\".", 7)
//            kotlinCompletion.generateCompletionProposals(str, str.length)
//        }
//    }
//}