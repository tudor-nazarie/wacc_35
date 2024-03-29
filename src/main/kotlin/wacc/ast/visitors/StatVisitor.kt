package wacc.ast.visitors

import WaccParserBaseVisitor
import wacc.ast.Stat
import wacc.ast.pos

object StatVisitor : WaccParserBaseVisitor<Stat>() {
    override fun visitSkip(ctx: WaccParser.SkipContext?): Stat = Stat.Skip(ctx!!.pos)

    override fun visitAssignNew(ctx: WaccParser.AssignNewContext?): Stat {
        val type = TypeVisitor.visit(ctx?.type())
        val name = ctx?.IDENT()?.text!!
        val rhs = AssignRhsVisitor.visit(ctx.assignRhs())
        return Stat.AssignNew(ctx.pos, type, name, rhs)
    }

    override fun visitAssign(ctx: WaccParser.AssignContext?): Stat {
        val lhs = AssignLhsVisitor.visit(ctx?.assignLhs())
        val rhs = AssignRhsVisitor.visit(ctx?.assignRhs())
        return Stat.Assign(ctx!!.pos, lhs, rhs)
    }

    override fun visitRead(ctx: WaccParser.ReadContext?): Stat {
        val lhs = AssignLhsVisitor.visit(ctx?.assignLhs())
        return Stat.Read(ctx!!.pos, lhs)
    }

    override fun visitFree(ctx: WaccParser.FreeContext?): Stat {
        val expr = ExprVisitor.visit(ctx?.expr())
        return Stat.Free(ctx!!.pos, expr)
    }

    override fun visitReturn(ctx: WaccParser.ReturnContext?): Stat {
        val expr = ExprVisitor.visit(ctx?.expr())
        return Stat.Return(ctx!!.pos, expr)
    }

    override fun visitExit(ctx: WaccParser.ExitContext?): Stat {
        val expr = ExprVisitor.visit(ctx?.expr())
        return Stat.Exit(ctx!!.pos, expr)
    }

    override fun visitPrint(ctx: WaccParser.PrintContext?): Stat {
        val expr = ExprVisitor.visit(ctx?.expr())
        return Stat.Print(ctx!!.pos, expr)
    }

    override fun visitPrintln(ctx: WaccParser.PrintlnContext?): Stat {
        val expr = ExprVisitor.visit(ctx?.expr())
        return Stat.Println(ctx!!.pos, expr)
    }

    override fun visitIfThenElse(ctx: WaccParser.IfThenElseContext?): Stat {
        val expr = ExprVisitor.visit(ctx?.expr())
        val stat1 = visit(ctx?.stat(0))
        val stat2 = visit(ctx?.stat(1))
        return Stat.IfThenElse(ctx!!.pos, expr, stat1, stat2)
    }

    override fun visitWhileDo(ctx: WaccParser.WhileDoContext?): Stat {
        val expr = ExprVisitor.visit(ctx?.expr())
        val stat = visit(ctx?.stat())
        return Stat.WhileDo(ctx!!.pos, expr, stat)
    }

    override fun visitBegin(ctx: WaccParser.BeginContext?): Stat {
        val stat = visit(ctx?.stat())
        return Stat.Begin(ctx!!.pos, stat)
    }

    override fun visitCompose(ctx: WaccParser.ComposeContext?): Stat {
        val stat1 = visit(ctx?.stat(0))
        val stat2 = visit(ctx?.stat(1))
        return Stat.Compose(ctx!!.pos, stat1, stat2)
    }
}
