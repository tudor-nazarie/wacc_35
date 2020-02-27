package wacc.ast.codegen

import java.lang.IllegalStateException
import wacc.ast.*
import wacc.ast.BinaryOperator.*
import wacc.ast.UnaryOperator.*
import wacc.ast.codegen.types.*
import wacc.ast.codegen.types.Condition.*
import wacc.ast.codegen.types.ImmType.*
import wacc.ast.codegen.types.Instruction.*
import wacc.ast.codegen.types.Operand.Imm
import wacc.ast.codegen.types.Register.*

/*
We don't need to worry about register vs. stack allocation when dealing with Stat and AssignRhs
since there shouldn't be anytime when registers are "reserved" from previous statements.
 */

private const val MIN_USABLE_REG = 4
private const val MAX_USABLE_REG = 12

val usableRegs = (MIN_USABLE_REG..MAX_USABLE_REG).map { GeneralRegister(it) }

private class GlobalCodeGenData(var labelCount: Int = 0, var strings: List<String>) {
    fun getLabel() = "L${labelCount++}"

    fun getStringLabel(s: String): String =
            strings.indexOfFirst { s == it }.let { if (it < 0) strings.size.also { strings += s } else it }
                    .let { "msg_$it" }
}

private class CodeGenContext(
    val global: GlobalCodeGenData,
    val func: Func?,
    val scopes: List<List<Pair<String, MemoryAccess>>>,
    val availableRegs: List<Register> = usableRegs
) {
    fun offsetOfIdent(ident: String): Int {
        var offset = 0
        var found = false
        for (scope in scopes) {
            for (varData in scope) {
                val (name, memAcc) = varData
                if (found)
                    offset += memAcc.size
                found = found || (name == ident)
            }
            if (found)
                break

        }
        if (!found)
            throw IllegalStateException()
        return offset
    }

    fun takeReg(newScope: List<Pair<String, MemoryAccess>>? = null): Pair<Register, CodeGenContext>? =
            availableRegs.getOrNull(0)?.let { reg ->
                reg to CodeGenContext(global, func, newScope?.let { listOf(it) + scopes } ?: scopes, availableRegs.drop(1))
            }

    fun withNewScope(newScope: List<Pair<String, MemoryAccess>>): CodeGenContext =
            CodeGenContext(global, func, listOf(newScope) + scopes, availableRegs)

    fun takeRegs(n: Int): Pair<List<Register>, CodeGenContext>? =
            if (availableRegs.size < n)
                null
            else
                availableRegs.take(n) to CodeGenContext(global, func, scopes, availableRegs.drop(n))

    fun withRegs(vararg regs: Register) =
            CodeGenContext(global, func, scopes, regs.asList() + availableRegs)

    val dst: Register?
        get() = availableRegs.getOrNull(0)
}

fun Program.getAsm(): String {
    val (data, text) = genCode()
    val builder = StringBuilder()
    if (data.data.isNotEmpty()) {
        data.data.forEach { builder.append(it) }
    }
    builder.append(".text\n")
    text.functions.forEach { builder.append(it) }
    return builder.toString()
}

private fun Program.genCode(): Pair<Section.DataSection, Section.TextSection> {
    TODO()
//    val dataSection = Section.DataSection(emptyList())
//    val funcs = funcs.map(Func::codeGen).toMutableList()
//    funcs += stat.genMainFunc()
//    return Section.DataSection(emptyList()) to Section.TextSection(funcs)
}

// private fun Func.codeGen(): Function {
//    return Function(Label(name), emptyList(), false)
// }

private fun Stat.genCode(ctx: CodeGenContext): List<Instruction> = when (this) {
    is Stat.Skip -> emptyList()
    is Stat.AssignNew -> rhs.genCode(ctx).let { rhsInstrs ->
        rhsInstrs + Store(ctx.dst!!, StackPointer, Imm(ctx.offsetOfIdent(name), INT))
    }
    is Stat.Assign -> rhs.genCode(ctx).let { rhsInstrs ->
        when (lhs) {
            is AssignLhs.Variable -> rhsInstrs + Store(ctx.dst!!, StackPointer, Imm(ctx.offsetOfIdent(lhs.name), INT))
            is AssignLhs.ArrayElem -> TODO()
            is AssignLhs.PairElem -> TODO()
        }
    }
    is Stat.Read -> TODO()
    is Stat.Free -> TODO()
    is Stat.Return -> expr.genCode(ctx) + Move(GeneralRegister(0), Operand.Reg(ctx.dst!!))
    is Stat.Exit ->
        expr.genCode(ctx) + Move(GeneralRegister(0), Operand.Reg(ctx.dst!!)) + Pop(listOf(ProgramCounter))
    is Stat.Print -> TODO()
    is Stat.Println -> TODO()
    is Stat.IfThenElse -> (ctx.global.getLabel() to ctx.global.getLabel()).let { (label1, label2) ->
        emptyList<Instruction>() +
                expr.genCode(ctx) + // condition
                Compare(ctx.dst!!, Imm(0, INT)) +
                Branch(Operand.Label(label1), Equal) +
                branch2.genCode(ctx) + // code if false
                Branch(Operand.Label(label2)) +
                Special.Label(label1) +
                branch1.genCode(ctx) + // code if true
                Special.Label(label2)
    }
    is Stat.WhileDo -> (ctx.global.getLabel() to ctx.global.getLabel()).let { (label1, label2) ->
        emptyList<Instruction>() +
                Branch(Operand.Label(label1)) +
                Special.Label(label2) +
                stat.genCodeWithNewScope(ctx) + // loop body
                Special.Label(label1) +
                expr.genCode(ctx) + // loop condition
                Compare(ctx.dst!!, Imm(1, INT)) +
                Branch(Operand.Label(label1), Equal)
    }
    is Stat.Begin -> stat.genCodeWithNewScope(ctx) // ignore context from inner scope
    is Stat.Compose -> stat1.genCode(ctx) + stat2.genCode(ctx)
}

private fun AssignRhs.genCode(ctx: CodeGenContext): List<Instruction> = when (this) {
    is AssignRhs.Expression -> expr.genCode(ctx)
    is AssignRhs.ArrayLiteral -> ctx.takeReg()!!.let { (arrayAddr, innerCtx) -> emptyList<Instruction>() +
            ctx.malloc((exprs.size + 1) * 4) +  // Allocate array
            exprs.mapIndexed { i, expr ->
                expr.genCode(innerCtx) + Store(innerCtx.dst!!, arrayAddr, Imm((i + 1) * 4, INT))
            }.flatten() +  // Store array values
            Load(innerCtx.dst!!, Imm(exprs.size, INT)) +
            Store(innerCtx.dst!!, arrayAddr)  // Store array length
    }
    is AssignRhs.Newpair -> listOf(
            Load(GeneralRegister(0), Imm(8, INT)),
            BranchLink(Operand.Label("malloc")),
            Move(ctx.dst!!, Operand.Reg(GeneralRegister(0)))
    ) + ctx.takeReg()!!.let { (pairReg, ctx2) ->
        listOf((expr1 to null), (expr2 to Imm(4, INT))).flatMap { (expr, offset) ->
            expr.genCode(ctx2) +
                    Load(GeneralRegister(0), Imm(4, INT)) +
                    BranchLink(Operand.Label("malloc")) +
                    Store(ctx2.dst!!, GeneralRegister(0)) +
                    Store(GeneralRegister(0), pairReg, offset)
        }
    }
    is AssignRhs.PairElem -> TODO()
    is AssignRhs.Call -> TODO()
}

private fun Expr.genCode(ctx: CodeGenContext): List<Instruction> = when (this) {
    is Expr.Literal.IntLiteral -> listOf(Move(ctx.dst!!, Imm(value.toInt(), INT))) // TODO: int vs long?
    is Expr.Literal.BoolLiteral -> listOf(Move(ctx.dst!!, Imm(if (value) 1 else 0, BOOL)))
    is Expr.Literal.CharLiteral -> listOf(Move(ctx.dst!!, Imm(value.toInt(), CHAR)))
    is Expr.Literal.StringLiteral -> listOf(Move(ctx.dst!!, Operand.Label(ctx.global.getStringLabel(value))))
    is Expr.Literal.PairLiteral -> throw IllegalStateException()
    is Expr.Ident -> listOf(Load(ctx.dst!!, Operand.Reg(StackPointer), Imm(ctx.offsetOfIdent(name), INT)))
    is Expr.ArrayElem -> TODO()
    is Expr.UnaryOp -> when (operator) {
        BANG -> expr.genCode(ctx) + Op(Operation.NegateOp, ctx.dst!!, ctx.dst!!, Operand.Reg(ctx.dst!!))
        MINUS -> expr.genCode(ctx) + Op(Operation.RevSubOp, ctx.dst!!, ctx.dst!!, Imm(0, INT))
        LEN -> TODO()
        ORD, CHR -> expr.genCode(ctx) // Chars and ints should be represented the same way; ignore conversion
    }
    is Expr.BinaryOp -> ctx.takeRegs(2)?.let { (regs, ctx2) ->
        val (dst, nxt) = regs
        if (expr1.weight <= expr2.weight) {
            expr1.genCode(ctx2.withRegs(dst, nxt)) + expr2.genCode(ctx2.withRegs(nxt))
        } else {
            expr2.genCode(ctx2.withRegs(nxt, dst)) + expr2.genCode(ctx2.withRegs(dst))
        } + when (operator) {
            MUL -> listOf(Op(Operation.MulOp, dst, dst, Operand.Reg(nxt)))
            DIV -> listOf(Op(Operation.DivOp(), dst, dst, Operand.Reg(nxt)))
            MOD -> listOf(Op(Operation.ModOp(), dst, dst, Operand.Reg(nxt)))
            ADD -> listOf(Op(Operation.AddOp, dst, dst, Operand.Reg(nxt)))
            SUB -> listOf(Op(Operation.SubOp, dst, dst, Operand.Reg(nxt)))
            GT -> regs.assignBool(SignedGreaterThan)
            GTE -> regs.assignBool(SignedGreaterOrEqual)
            LT -> regs.assignBool(SignedLess)
            LTE -> regs.assignBool(SignedLessOrEqual)
            EQ -> regs.assignBool(Equal)
            NEQ -> regs.assignBool(NotEqual)
            LAND -> listOf(Op(Operation.AndOp, dst, dst, Operand.Reg(nxt)))
            LOR -> listOf(Op(Operation.OrOp, dst, dst, Operand.Reg(nxt)))
        }
    } ?: TODO()
}

// private fun Stat.genMainFunc(): Function {
//    // TODO remove hardcoded function
//    return Function(Label("main"), listOf(
//            Push(listOf(LinkRegister)),
//            Move(GeneralRegister(0), Imm(0, INT)),
//            Pop(listOf(ProgramCounter)),
//            Special.Ltorg
//    ), true)
// }

private fun Pair<Register, Register>.assignBool(cond: Condition) = listOf(
        Compare(first, Operand.Reg(second)),
        Move(first, Imm(1, BOOL), cond),
        Move(first, Imm(0, BOOL), cond.inverse)
)

private fun List<Register>.assignBool(cond: Condition) = let { (reg1, reg2) -> (reg1 to reg2).assignBool(cond) }

private fun CodeGenContext.malloc(size: Int): List<Instruction> = listOf(
        Load(GeneralRegister(0), Imm(size, INT)),
        BranchLink(Operand.Label("malloc")),
        Move(dst!!, Operand.Reg(GeneralRegister(0)))
)

private val Condition.inverse
    get() = when (this) {
        Minus -> Plus
        Plus -> Minus
        Equal -> NotEqual
        NotEqual -> Equal
        UnsignedHigherOrSame -> UnsignedLower
        UnsignedLower -> UnsignedHigherOrSame
        CarrySet -> CarryClear
        CarryClear -> CarrySet
        Overflow -> NoOverflow
        NoOverflow -> Overflow
        UnsignedHigher -> UnsignedLowerOrSame
        UnsignedLowerOrSame -> UnsignedHigher
        SignedGreaterOrEqual -> SignedLess
        SignedLess -> SignedGreaterOrEqual
        SignedGreaterThan -> SignedLessOrEqual
        SignedLessOrEqual -> SignedGreaterThan
        Always -> throw IllegalStateException()
    }

val MemoryAccess.size: Int
    get() = when (this) {
        MemoryAccess.Byte -> 1
        MemoryAccess.HalfWord -> 2
        MemoryAccess.Word -> 4
    }

private val List<Pair<String, MemoryAccess>>.offset: Int
    get() = sumBy { it.second.size }

// Generates code for a statement, with instructions to adjust the stack pointer to account for the new scope
private fun Stat.genCodeWithNewScope(ctx: CodeGenContext): List<Instruction> {
    val pre = Op(Operation.SubOp, StackPointer, StackPointer, Imm(vars.offset))
    val post = Op(Operation.AddOp, StackPointer, StackPointer, Imm(vars.offset))
    return emptyList<Instruction>() +
            if (vars.isEmpty()) emptyList() else listOf(pre) +
            genCode(ctx.withNewScope(vars)) +
            if (vars.isEmpty()) emptyList() else listOf(post)
}