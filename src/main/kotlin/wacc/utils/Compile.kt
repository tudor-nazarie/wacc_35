package wacc.utils

import WaccLexer
import WaccParser
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.Callable
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import picocli.CommandLine.*
import wacc.RETURN_CODE_OK
import wacc.RETURN_CODE_SEMANTIC_ERROR
import wacc.RETURN_CODE_SYNTACTIC_ERROR
import wacc.ast.visitors.ProgramVisitor
import wacc.checker.SyntaxErrorListener
import wacc.checker.checkSemantics
import wacc.codegen.getAsm

@Command(description = ["Compile a WACC program"], name = "wacc",
        mixinStandardHelpOptions = true, version = [wacc.VERSION])
class Compile : Callable<Int>, Logging {
    private val logger = logger()

    @Parameters(index = "0", description = ["WACC program source to compile"])
    private var file: File? = null

    @Option(names = ["-s", "--semantic"], description = ["Perform semantic analysis"], negatable = true)
    private var semantic = true

    @Option(names = ["-t", "--stdout"], description = ["Print output to STDOUT"])
    private var stdout = false

    override fun call(): Int {
        // Generate input from file
        val inputStream = FileInputStream(file!!)
        val charStream = CharStreams.fromStream(inputStream)

        // Lex and parse the input
        val lexer = WaccLexer(charStream)
        val tokens = CommonTokenStream(lexer)
        val parser = WaccParser(tokens)

        // Replace error listener with our logging one
        parser.removeErrorListeners()
        parser.addErrorListener(SyntaxErrorListener())

        val tree = parser.program()

        // Check for syntax errors from
        if (parser.numberOfSyntaxErrors > 0) {
            println("${parser.numberOfSyntaxErrors} syntax errors. Halting compilation.")
            return RETURN_CODE_SYNTACTIC_ERROR
        } else if (!semantic)
            return RETURN_CODE_OK

        // Generate the AST
        val programVisitor = ProgramVisitor()
        val program = programVisitor.visit(tree)

        // Check for further syntax and semantic errors from the tree
        val errors = program.checkSemantics().reversed()
        errors.sorted().forEach(::println)
        if (errors.any { !it.isSemantic })
            return RETURN_CODE_SYNTACTIC_ERROR
        if (errors.any { it.isSemantic })
            return RETURN_CODE_SEMANTIC_ERROR

        val programAsm = program.getAsm()
        if (stdout) {
            println(programAsm)
        } else {
            val asmFile = File(file!!.nameWithoutExtension + ".s")
            asmFile.writeText(programAsm)
        }

        return RETURN_CODE_OK
    }
}