package wacc.ast

/***
 * An expression AST Node
 *
 * Expression node representing different expressions in the language specification,
 * namely literals, variables or operator application.
 *
 * @property pos the position in the source of the start of the statement
 */
sealed class Expr(pos: FilePos) : ASTNode(pos) {
    sealed class Literal(pos: FilePos) : Expr(pos) {
        class IntLiteral(pos: FilePos, val value: Long) : Literal(pos)
        class BoolLiteral(pos: FilePos, val value: Boolean) : Literal(pos)
        class CharLiteral(pos: FilePos, val value: Char) : Literal(pos)
        class StringLiteral(pos: FilePos, val value: String) : Literal(pos)
        class PairLiteral(pos: FilePos) : Literal(pos)
    }

    class Ident(pos: FilePos, val name: String) : Expr(pos)

    class ArrayElem(pos: FilePos, val name: Ident, val exprs: Array<Expr>) : Expr(pos) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ArrayElem

            if (name != other.name) return false
            if (!exprs.contentEquals(other.exprs)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + exprs.contentHashCode()
            return result
        }
    }

    class UnaryOp(pos: FilePos, val operator: UnaryOperator, val expr: Expr) : Expr(pos)
    class BinaryOp(pos: FilePos, val operator: BinaryOperator, val expr1: Expr, val expr2: Expr) : Expr(pos)

    class ClassField(pos: FilePos, val expr: Expr, val ident: String) : Expr(pos) {
        lateinit var cls: Class
    }

    class Instantiate(pos: FilePos, val className: String) : Expr(pos) {
        lateinit var cls: Class
    }
}
