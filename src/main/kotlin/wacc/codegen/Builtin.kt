package wacc.codegen

import java.lang.IllegalStateException
import wacc.codegen.types.*
import wacc.codegen.types.Condition.*
import wacc.codegen.types.Function
import wacc.codegen.types.Instruction.*
import wacc.codegen.types.Instruction.Special.Label
import wacc.codegen.types.Operand.Imm
import wacc.codegen.types.Operand.Reg
import wacc.codegen.types.Operation.AddOp
import wacc.codegen.types.Register.*

typealias BuiltinDependency = Pair<List<BuiltinFunction>, List<BuiltinString>>
typealias BuiltinString = Pair<String, String> // Label to Value

data class BuiltinFunction(val function: Function, val deps: BuiltinDependency) {
    val label: Operand.Label
        get() = with(function[0]) {
            when (this) {
                is Label -> Operand.Label(this.name)
                else -> throw IllegalStateException()
            }
        }
}

private val BuiltinString.label: Operand.Label
    get() = Operand.Label(this.first)

// <editor-fold desc="print functions">

val printLnString: BuiltinString = "__s_print_ln" to ""
val printLn: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_print_ln"),
        Push(LinkRegister),
        Load(R0, printLnString.label),
        Op(AddOp, R0, R0, Imm(4)),
        BranchLink(Operand.Label("puts")),
        Move(R0, Imm(0)),
        BranchLink(Operand.Label("fflush")),
        Pop(ProgramCounter)
), emptyList<BuiltinFunction>() to listOf(printLnString))

val printStringString: BuiltinString = "__s_print_string" to "%.*s"
val printString: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_print_string"),
        Push(LinkRegister),
        Load(R1, Reg(R0)),
        Op(AddOp, R2, R0, Imm(4)),
        Load(R0, printStringString.label),
        Op(AddOp, R0, R0, Imm(4)),
        BranchLink(Operand.Label("printf")),
        Move(R0, Imm(0)),
        BranchLink(Operand.Label("fflush")),
        Pop(ProgramCounter)
), emptyList<BuiltinFunction>() to listOf(printStringString))

val printIntString: BuiltinString = "__s_print_int" to "%d"
val printInt: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_print_int"),
        Push(LinkRegister),
        Move(R1, Reg(R0)),
        Load(R0, printIntString.label),
        Op(AddOp, R0, R0, Imm(4)),
        BranchLink(Operand.Label("printf")),
        Move(R0, Imm(0)),
        BranchLink(Operand.Label("fflush")),
        Pop(ProgramCounter)
), emptyList<BuiltinFunction>() to listOf(printIntString))

val printReferenceString: BuiltinString = "__s_print_reference" to "%p"
val printReference: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_print_reference"),
        Push(LinkRegister),
        Move(R1, Reg(R0)),
        Load(R0, printReferenceString.label),
        Op(AddOp, R0, R0, Imm(4)),
        BranchLink(Operand.Label("printf")),
        Move(R0, Imm(0)),
        BranchLink(Operand.Label("fflush")),
        Pop(ProgramCounter)
), emptyList<BuiltinFunction>() to listOf(printReferenceString))

val printBoolTrueString: BuiltinString = "__s_print_bool_true" to "true"
val printBoolFalseString: BuiltinString = "__s_print_bool_false" to "false"
val printBool: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_print_bool"),
        Push(LinkRegister),
        Compare(R0, Imm(0)),
        Load(R0, printBoolTrueString.label, condition = NotEqual),
        Load(R0, printBoolFalseString.label, condition = Equal),
        Op(AddOp, R0, R0, Imm(4)),
        BranchLink(Operand.Label("printf")),
        Move(R0, Imm(0)),
        BranchLink(Operand.Label("fflush")),
        Pop(ProgramCounter)
), emptyList<BuiltinFunction>() to listOf(printBoolTrueString, printBoolFalseString))

// </editor-fold>

// <editor-fold desc="print functions">

val readIntString: BuiltinString = "__s_read_int" to "%d"
val readInt: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_read_int"),
        Push(LinkRegister),
        Move(R1, Reg(R0)),
        Load(R0, readIntString.label),
        Op(AddOp, R0, R0, Imm(4)),
        BranchLink(Operand.Label("scanf")),
        Pop(ProgramCounter)
), emptyList<BuiltinFunction>() to listOf(readIntString))

val readCharString: BuiltinString = "__s_read_char" to " %c"
val readChar: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_read_char"),
        Push(LinkRegister),
        Move(R1, Reg(R0)),
        Load(R0, readCharString.label),
        Op(AddOp, R0, R0, Imm(4)),
        BranchLink(Operand.Label("scanf")),
        Pop(ProgramCounter)
), emptyList<BuiltinFunction>() to listOf(readCharString))

// </editor-fold>

// <editor-fold desc="runtime errors">

val throwRuntimeError: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_throw_runtime_error"),
        BranchLink(printString.label),
        Move(R0, Imm(-1)),
        BranchLink(Operand.Label("exit"))
), listOf(printString) to emptyList())

val overflowErrorString: BuiltinString = "__s_overflow_error" to "OverflowError: the result is too small/large to store in a 4-byte signed-integer.\n"
val throwOverflowError: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_throw_overflow_error"),
        Load(R0, overflowErrorString.label),
        BranchLink(throwRuntimeError.label)
), listOf(throwRuntimeError) to listOf(overflowErrorString))

// </editor-fold>

// <editor-fold desc="memory stuff">

val negativeArrayIndexString: BuiltinString = "__s_array_index_negative" to "ArrayIndexOutOfBoundsError: negative index\n"
val arrayIndexTooLargeString: BuiltinString = "__s_array_index_too_large" to "ArrayIndexOutOfBoundsError: index too large\n"
val checkArrayBounds: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_check_array_bounds"),
        Push(LinkRegister),
        Compare(R0, Imm(0)),
        Load(R0, negativeArrayIndexString.label, condition = SignedLess),
        BranchLink(throwRuntimeError.label, condition = SignedLess),
        Load(R1, Reg(R1)),
        Compare(R0, Reg(R1)),
        Load(R0, arrayIndexTooLargeString.label, condition = CarrySet),
        BranchLink(throwRuntimeError.label, condition = CarrySet),
        Pop(ProgramCounter)
), listOf(throwRuntimeError) to listOf(negativeArrayIndexString, arrayIndexTooLargeString))

val checkNullPointerString: BuiltinString = "__s_check_null_pointer" to "NullReferenceError: dereference a null reference\n"
val checkNullPointer: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_check_null_pointer"),
        Push(LinkRegister),
        Compare(R0, Imm(0)),
        Load(R0, checkNullPointerString.label, condition = Equal),
        BranchLink(throwRuntimeError.label, condition = Equal),
        Pop(ProgramCounter)
), listOf(throwRuntimeError) to listOf(checkNullPointerString))

val nullPointerDereferenceString: BuiltinString = "__s_null_pointer_deref" to "NullReferenceError: dereference a null reference\n"
val freePair: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_free_pair"),
        Push(LinkRegister),
        Compare(R0, Imm(0)),
        Load(R0, nullPointerDereferenceString.label, condition = Equal),
        Branch(throwRuntimeError.label, condition = Equal),
        Push(listOf(R0)),
        Load(R0, Reg(R0)),
        BranchLink(Operand.Label("free")),
        Load(R0, Reg(StackPointer)),
        Load(R0, R0.op, Imm(4)),
        BranchLink(Operand.Label("free")),
        Pop(listOf(R0)),
        BranchLink(Operand.Label("free")),
        Pop(ProgramCounter)
), listOf(throwRuntimeError) to listOf(nullPointerDereferenceString))

val nullPointerFreeString: BuiltinString = "__s_null_pointer_free" to "NullReferenceError: freeing a null reference\n"
val freeInstance: BuiltinFunction = BuiltinFunction(listOf(
        Label("__f_free_instance"),
        Push(LinkRegister),
        Compare(R0, Imm(0)),
        Load(R0, nullPointerFreeString.label, condition = Equal),
        Branch(throwRuntimeError.label, condition = Equal),
        BranchLink(Operand.Label("free")),
        Pop(StackPointer)
), listOf(throwRuntimeError) to listOf(nullPointerFreeString))

val divideByZeroString: BuiltinString = "__s_divide_by_zero" to "DivideByZeroError: divide or modulo by zero\n"
val checkDivideByZero = BuiltinFunction(listOf(
        Label("__f_check_divide_by_zero"),
        Push(LinkRegister),
        Compare(R1, Imm(0)),
        Load(R0, divideByZeroString.label, condition = Equal),
        Branch(throwRuntimeError.label, condition = Equal),
        Pop(ProgramCounter)
), listOf(throwRuntimeError) to listOf(divideByZeroString))

// </editor-fold>
