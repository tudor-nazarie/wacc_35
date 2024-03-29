package wacc.codegen.types

sealed class Operation {
    object AddOp : Operation() {
        override fun toString(): String = "ADD"
    }

    object SubOp : Operation() {
        override fun toString(): String = "SUB"
    }

    object RevSubOp : Operation() {
        override fun toString(): String = "RSB"
    }

    object AndOp : Operation() {
        override fun toString(): String {
            return "AND"
        }
    }

    object OrOp : Operation() {
        override fun toString(): String = "ORR"
    }

    object NegateOp : Operation()

    object BitwiseNotOp : Operation()

    object BitwiseAndOp : Operation() {
        override fun toString(): String = "AND"
    }

    object BitwiseOrOp : Operation() {
        override fun toString(): String = "ORR"
    }

    object BitwiseXorOp : Operation() {
        override fun toString(): String = "EOR"
    }

    object BitwiseLeftOp : Operation() {
        override fun toString(): String = "LSL"
    }

    object BitwiseRightOp : Operation() {
        override fun toString(): String = "ASR"
    }
}
